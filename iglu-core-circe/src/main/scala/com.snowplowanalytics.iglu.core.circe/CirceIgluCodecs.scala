/*
 * Copyright (c) 2016-2023 Snowplow Analytics Ltd. All rights reserved.
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

import cats.syntax.either._
import io.circe._
import io.circe.syntax._

import com.snowplowanalytics.iglu.core._

/**
  * Circe codecs for Iglu entities,
  * such as self-describing schema and self-describing data.
  */
trait CirceIgluCodecs {

  def toDecodingFailure(cursor: HCursor, data: String)(error: ParseError): DecodingFailure =
    DecodingFailure(error.message(data), cursor.history)

  implicit final val schemaVerCirceJsonDecoder: Decoder[SchemaVer] =
    Decoder.instance(parseSchemaVer)

  implicit final val schemaVerFullCirceJsonDecoder: Decoder[SchemaVer.Full] =
    Decoder.instance(parseSchemaVerFull)

  implicit final val schemaVerCirceJsonEncoder: Encoder[SchemaVer] =
    Encoder.instance { schemaVer =>
      Json.fromString(schemaVer.asString)
    }

  implicit final val schemaMapCirceJsonDecoder: Decoder[SchemaMap] =
    Decoder.instance(cur => parseSchemaMap(cur).leftMap(_._1))

  implicit final val schemaMapCirceJsonEncoder: Encoder[SchemaMap] =
    Encoder.instance { schemaMap =>
      Json.obj(
        "vendor"  -> Json.fromString(schemaMap.schemaKey.vendor),
        "name"    -> Json.fromString(schemaMap.schemaKey.name),
        "format"  -> Json.fromString(schemaMap.schemaKey.format),
        "version" -> Json.fromString(schemaMap.schemaKey.version.asString)
      )
    }

  implicit final val schemaCriterionDecoder: Decoder[SchemaCriterion] =
    Decoder[String].emap { s =>
      SchemaCriterion.parse(s).toRight(s"$s is invalid Iglu schema criterion")
    }

  implicit final val schemaCriterionEncoder: Encoder[SchemaCriterion] =
    Encoder[String].contramap(_.asString)

  implicit final val schemaKeyCirceJsonDecoder: Decoder[SchemaKey] =
    Decoder.instance { cursor =>
      cursor
        .as[String]
        .flatMap(s =>
          SchemaKey
            .fromUri(s)
            .leftMap(e =>
              DecodingFailure(s"Cannot decode $s as SchemaKey, ${e.message(s)}", cursor.history)
            )
        )
    }

  implicit final val schemaKeyCirceJsonEncoder: Encoder[SchemaKey] =
    Encoder.instance { key =>
      Json.fromString(key.toSchemaUri)
    }

  implicit final val selfDescribingSchemaCirceDecoder: Decoder[SelfDescribingSchema[Json]] =
    Decoder.instance { hCursor =>
      for {
        map <- hCursor.as[JsonObject].map(_.toMap)
        jsonSchema <- map.get("self") match {
          case None    => Left(DecodingFailure("self-key is not available", hCursor.history))
          case Some(_) => Right(map - "self" - s"$$schema")
        }
        schemaMap <- parseSchemaMap(hCursor).leftMap(_._1)
        _         <- checkSchemaUri(hCursor).leftMap(_._1)
      } yield SelfDescribingSchema(schemaMap, Json.fromJsonObject(JsonObject.fromMap(jsonSchema)))
    }

  implicit final val selfDescribingSchemaCirceEncoder: Encoder[SelfDescribingSchema[Json]] =
    Encoder.instance { schema =>
      Json
        .fromFields(
          List(
            "self"      -> schema.self.asJson(schemaMapCirceJsonEncoder),
            s"$$schema" -> SelfDescribingSchema.SelfDescribingUri.toString.asJson
          )
        )
        .deepMerge(schema.schema)
    }

  implicit final val selfDescribingDataCirceEncoder: Encoder[SelfDescribingData[Json]] =
    Encoder.instance { data =>
      Json.obj("schema" -> Json.fromString(data.schema.toSchemaUri), "data" -> data.data)
    }

  implicit final val selfDescribingDataCirceDecoder: Decoder[SelfDescribingData[Json]] =
    Decoder.instance { hCursor =>
      for {
        map <- hCursor.as[JsonObject].map(_.toMap)
        schema <- map.get("schema") match {
          case None => Left(DecodingFailure("schema key is not available", hCursor.history))
          case Some(schema) =>
            for {
              schemaString <- schema.as[String]
              schemaKey <- SchemaKey
                .fromUri(schemaString)
                .leftMap(toDecodingFailure(hCursor, schemaString))
            } yield schemaKey
        }
        data <- map.get("data") match {
          case None       => Left(DecodingFailure("data key is not available", hCursor.history))
          case Some(data) => Right(data)
        }
      } yield SelfDescribingData(schema, data)
    }

  implicit final val schemaListCirceJsonEncoder: Encoder[SchemaList] =
    Encoder.instance { data =>
      Json.fromValues(data.schemas.map(s => Json.fromString(s.toSchemaUri)))
    }

  implicit final val schemaListCirceJsonDecoder: Decoder[SchemaList] =
    Decoder.instance { cursor =>
      for {
        strings <- cursor.value.as[List[String]]
        result <- SchemaList
          .parseStrings(strings)
          .leftMap(err => DecodingFailure(err, cursor.history))
      } yield result
    }

  private[circe] def parseSchemaVer(hCursor: HCursor): Either[DecodingFailure, SchemaVer] =
    for {
      jsonString <- hCursor.as[String]
      schemaVer  <- SchemaVer.parse(jsonString).leftMap(toDecodingFailure(hCursor, jsonString))
    } yield schemaVer

  private[circe] def parseSchemaVerFull(hCursor: HCursor): Either[DecodingFailure, SchemaVer.Full] =
    parseSchemaVer(hCursor) match {
      case Right(full: SchemaVer.Full) => Right(full)
      case Right(other) =>
        Left(DecodingFailure(s"SchemaVer ${other.asString} is not full", hCursor.history))
      case Left(left) => Left(left)
    }

  private[circe] def parseSchemaMap(
    hCursor: HCursor
  ): Either[(DecodingFailure, ParseError), SchemaMap] = {
    val self = hCursor.downField("self")
    def selfKey[A: Decoder](key: String): Either[(DecodingFailure, ParseError), A] =
      self.downField(key).as[A].leftMap(e => (e, ParseError.InvalidSchema))

    for {
      vendor <- selfKey[String]("vendor")
      name   <- selfKey[String]("name")
      format <- selfKey[String]("format")
      version <- selfKey[SchemaVer.Full]("version").leftMap {
        case (e, _) => (e, ParseError.InvalidSchemaVer)
      }
    } yield SchemaMap(vendor, name, format, version)
  }

  private[circe] def checkSchemaUri(hCursor: HCursor): Either[(DecodingFailure, ParseError), Unit] =
    hCursor.downField(s"$$schema").as[String] match {
      case Right(schemaUri) if (schemaUri != SelfDescribingSchema.SelfDescribingUri.toString) =>
        Left(
          (
            DecodingFailure(ParseError.InvalidMetaschema.message(schemaUri), hCursor.history),
            ParseError.InvalidMetaschema
          )
        )
      case Right(_)  => Right(())
      case Left(err) => Left((err, ParseError.InvalidMetaschema))
    }
}

object CirceIgluCodecs extends CirceIgluCodecs
