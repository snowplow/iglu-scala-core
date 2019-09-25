/*
 * Copyright (c) 2016-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.iglu.core.circe

// Cats
import cats.syntax.either._

// Circe
import io.circe._
import io.circe.syntax._

// This library
import com.snowplowanalytics.iglu.core._

/**
 * Example of Circe codecs for Iglu entities
 */
trait CirceIgluCodecs {

  def toDecodingFailure(cursor: HCursor)(error: ParseError): DecodingFailure =
    DecodingFailure(error.code, cursor.history)

  final implicit val schemaVerCirceJsonDecoder: Decoder[SchemaVer] =
    Decoder.instance(parseSchemaVer)

  final implicit val schemaVerCirceJsonEncoder: Encoder[SchemaVer] =
    Encoder.instance { schemaVer => Json.fromString(schemaVer.asString) }

  final implicit val schemaMapCirceJsonDecoder: Decoder[SchemaMap] =
    Decoder.instance(parseSchemaMap)

  final implicit val schemaMapCirceJsonEncoder: Encoder[SchemaMap] =
    Encoder.instance { schemaMap =>
      Json.obj(
        "vendor" -> Json.fromString(schemaMap.schemaKey.vendor),
        "name"    -> Json.fromString(schemaMap.schemaKey.name),
        "format"  -> Json.fromString(schemaMap.schemaKey.format),
        "version" -> Json.fromString(schemaMap.schemaKey.version.asString)
      )
    }

  final implicit val schemaKeyCirceJsonDecoder: Decoder[SchemaKey] =
    Decoder.instance { cursor =>
      cursor
        .as[String]
        .flatMap(s => SchemaKey.fromUri(s).leftMap(e => DecodingFailure(s"Cannot decode $s as SchemaKey, ${e.code}", cursor.history)))
    }

  final implicit val schemaKeyCirceJsonEncoder: Encoder[SchemaKey] =
    Encoder.instance { key => Json.fromString(key.toSchemaUri) }

  final implicit val selfDescribingSchemaCirceDecoder: Decoder[SelfDescribingSchema[Json]] =
    Decoder.instance { hCursor =>
      for {
        map <- hCursor.as[JsonObject].map(_.toMap)
        jsonSchema <- map.get("self") match {
          case None => Left(DecodingFailure("self-key is not available", hCursor.history))
          case Some(_) => Right(map - "self" - "$schema")
        }
        schemaMap <- parseSchemaMap(hCursor)
        _ <- checkSchemaUri(hCursor)
      } yield SelfDescribingSchema(schemaMap, Json.fromJsonObject(JsonObject.fromMap(jsonSchema)))
    }

  final implicit val selfDescribingSchemaCirceEncoder: Encoder[SelfDescribingSchema[Json]] =
    Encoder.instance { schema =>
      Json.fromFields(List(
        "self" -> schema.self.asJson(schemaMapCirceJsonEncoder),
        "$schema" -> SelfDescribingSchema.SelfDescribingUri.toString.asJson
      )).deepMerge(schema.schema)
    }

  final implicit val selfDescribingDataCirceEncoder: Encoder[SelfDescribingData[Json]] =
    Encoder.instance { data =>
      Json.obj("schema" -> Json.fromString(data.schema.toSchemaUri), "data" -> data.data)
    }

  final implicit val selfDescribingDataCirceDecoder: Decoder[SelfDescribingData[Json]] =
    Decoder.instance { hCursor =>
      for {
        map <- hCursor.as[JsonObject].map(_.toMap)
        schema <- map.get("schema") match {
          case None => Left(DecodingFailure("schema key is not available", hCursor.history))
          case Some(schema) => for {
            schemaString <- schema.as[String]
            schemaKey <- SchemaKey.fromUri(schemaString).leftMap(toDecodingFailure(hCursor))
          } yield schemaKey
        }
        data <- map.get("data") match {
          case None => Left(DecodingFailure("data key is not available", hCursor.history))
          case Some(data) => Right(data)
        }
      } yield SelfDescribingData(schema, data)
    }


  final implicit val schemaListCirceJsonEncoder: Encoder[SchemaList] =
    Encoder.instance { data => Json.fromValues(data.schemas.map(s => Json.fromString(s.toSchemaUri))) }

  final implicit val schemaListCirceJsonDecoder: Decoder[SchemaList] =
    Decoder.instance { cursor =>
      for {
        strings <- cursor.value.as[List[String]]
        result <- SchemaList.parseStrings(strings).leftMap(err => DecodingFailure(err, cursor.history))
      } yield result
    }

  private[circe] def parseSchemaVer(hCursor: HCursor): Either[DecodingFailure, SchemaVer] =
    for {
      jsonString <- hCursor.as[String]
      schemaVer  <- SchemaVer.parse(jsonString).leftMap(toDecodingFailure(hCursor))
    } yield schemaVer

  private[circe] def parseSchemaVerFull(hCursor: HCursor): Either[DecodingFailure, SchemaVer.Full] =
    parseSchemaVer(hCursor) match {
      case Right(full: SchemaVer.Full) => Right(full)
      case Right(other) => Left(DecodingFailure(s"SchemaVer ${other.asString} is not full", hCursor.history))
      case Left(left) => Left(left)
    }

  private[circe] def parseSchemaMap(hCursor: HCursor): Either[DecodingFailure, SchemaMap] =
    for {
      map <- hCursor.as[JsonObject].map(_.toMap)
      selfMapJson <- map.get("self") match {
        case None => Left(DecodingFailure(ParseError.InvalidSchema.code, hCursor.history))
        case Some(self) => Right(self)
      }
      selfMap <- selfMapJson.as[JsonObject].map(_.toMap)
      schemaKey <- selfMapToSchemaMap(selfMap, hCursor)
    } yield schemaKey

  private[circe] def selfMapToSchemaMap(selfMap: Map[String, Json], hCursor: HCursor): Either[DecodingFailure, SchemaMap] = {
    val self = (selfMap.get("vendor"), selfMap.get("name"), selfMap.get("format"), selfMap.get("version")).mapN {
      (v, n, f, ver) =>
        for {
          vendor  <- v.asString.toRight(ParseError.InvalidSchema)
          name    <- n.asString.toRight(ParseError.InvalidSchema)
          format  <- f.asString.toRight(ParseError.InvalidSchema)
          version <- ver.as(Decoder.instance(parseSchemaVerFull)).toOption.toRight(ParseError.InvalidSchemaVer)
        } yield SchemaMap(vendor, name, format, version)
    }

    self.toRight(ParseError.InvalidSchema: ParseError).flatten.leftMap(toDecodingFailure(hCursor))
  }

  private[circe] def checkSchemaUri(hCursor: HCursor): Either[DecodingFailure, Unit] =
    hCursor.downField(s"$$schema").as[String].flatMap { schemaUri =>
      if (schemaUri != SelfDescribingSchema.SelfDescribingUri.toString)
        Left(DecodingFailure(ParseError.InvalidSchemaUri.code, hCursor.history))
      else
        Right(())
    }
}

object CirceIgluCodecs extends CirceIgluCodecs
