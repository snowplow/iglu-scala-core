/*
 * Copyright (c) 2012-2023 Snowplow Analytics Ltd.. All rights reserved.
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
package com.snowplowanalytics.iglu.core

import com.snowplowanalytics.iglu.core.typeclasses._
import io.circe.{Decoder, DecodingFailure, Encoder, Json, JsonObject}
import io.circe.syntax._

/**
  * This module contains examples of common traits and type class instances
  * based on Circe library
  */
object IgluCoreCommon {
  implicit private val schemaKeyDecoder: Decoder[SchemaKey] = Decoder.instance(c =>
    for {
      vendor <- c.get[String]("vendor")
      name   <- c.get[String]("name")
      format <- c.get[String]("format")
      version <- c
        .get[String]("version")
        .flatMap(ver => SchemaVer.parseFull(ver).left.map(er => DecodingFailure(er.code, c.history))
        )
    } yield SchemaKey(vendor, name, format, version)
  )

  implicit private val schemaKeyEncoder: Encoder[SchemaKey] = Encoder.instance(key =>
    Json.obj(
      "vendor" := key.vendor,
      "name" := key.name,
      "format" := key.format,
      "version" := key.version.asString
    )
  )

  ////////////////////////
  // ExtractFrom Json4s //
  ////////////////////////

  /** Example common trait for [[ExtractSchemaKey]] *data* objects */
  trait CirceExtractSchemaKeyData extends ExtractSchemaKey[Json] {
    def extractSchemaKey(entity: Json): Either[ParseError, SchemaKey] =
      entity
        .hcursor
        .get[String]("schema")
        .left
        .map(_ => ParseError.InvalidData)
        .flatMap(SchemaKey.fromUri)
  }

  /**
    * Example of [[ExtractSchemaKey]] instance for json4s JSON *data*
    */
  implicit object CirceExtractSchemaKeyData extends CirceExtractSchemaKeyData

  /** Example common trait for [[ExtractSchemaKey]] *Schemas* objects */
  trait CirceExtractSchemaKeySchema extends ExtractSchemaKey[Json] {

    /**
      * Extract SchemaKey usning serialization formats defined at [[IgluJson4sCodecs]]
      */
    def extractSchemaKey(entity: Json): Either[ParseError, SchemaKey] =
      schemaKeyDecoder.at("self").decodeJson(entity).left.map(_ => ParseError.InvalidData)
  }

  /**
    * Example of [[ExtractSchemaKey]] instance for json4s JSON *Schemas*
    */
  implicit object CirceExtractSchemaKeySchema extends CirceExtractSchemaKeySchema

  /////////////////////
  // AttachTo Json4s //
  /////////////////////

  // Schemas

  implicit object CirceAttachSchemaKeySchema extends ExtractSchemaKey[Json] {
    def extractSchemaKey(entity: Json): Either[ParseError, SchemaKey] =
      schemaKeyDecoder.at("self").decodeJson(entity).left.map(_ => ParseError.InvalidSchema)
  }

  implicit object CirceAttachSchemaMapComplex extends ExtractSchemaMap[Json] with ToSchema[Json] {
    def extractSchemaMap(entity: Json): Either[ParseError, SchemaMap] =
      schemaKeyDecoder
        .at("self")
        .decodeJson(entity)
        .left
        .map(_ => ParseError.InvalidSchema)
        .map(SchemaMap.apply)

    def checkSchemaUri(entity: Json): Either[ParseError, Unit] =
      entity.hcursor.get[String]("$schema") match {
        case Right(schemaUri) if schemaUri == SelfDescribingSchema.SelfDescribingUri.toString =>
          Right(())
        case _ => Left(ParseError.InvalidMetaschema)
      }

    /**
      * Remove key with `self` description
      * `getContent` required to be implemented here because it extends [[ToSchema]]
      */
    def getContent(json: Json): Json =
      json.asObject.map(_.remove("$schema").remove("self").asJson).getOrElse(json)
  }

  // Data

  implicit object CirceAttachSchemaKeyData
      extends ExtractSchemaKey[Json]
      with ToData[Json]
      with CirceExtractSchemaKeyData {

    def getContent(json: Json): Either[ParseError, Json] =
      json
        .hcursor
        .get[JsonObject]("data")
        .left
        .map(_ => ParseError.InvalidData: ParseError)
        .map(_.asJson)

    def attachSchemaKey(schemaKey: SchemaKey, instance: Json): Json =
      Json.obj("schema" := schemaKey.toSchemaUri, "data" := instance)
  }

  //////////////////////////
  // ExtractFrom non-JSON //
  //////////////////////////

  /**
    * Stub class bearing its Schema
    */
  case class DescribedString(
    vendor: String,
    name: String,
    format: String,
    model: Int,
    revision: Int,
    addition: Int,
    data: String
  )

  /**
    * Example of [[ExtractSchemaKey]] instance for usual case class
    */
  implicit object DescribingStringInstance extends ExtractSchemaKey[DescribedString] {
    def extractSchemaKey(entity: DescribedString): Either[ParseError, SchemaKey] =
      Right(
        SchemaKey(
          entity.vendor,
          entity.name,
          entity.format,
          SchemaVer.Full(entity.model, entity.revision, entity.addition)
        )
      )
  }

  ///////////////
  // Normalize //
  ///////////////

  implicit object CirceNormalizeSchema extends NormalizeSchema[Json] {
    def normalize(schema: SelfDescribingSchema[Json]): Json =
      schema
        .schema
        .deepMerge(
          Json.obj(
            s"$$schema" := SelfDescribingSchema.SelfDescribingUri,
            "self" := schema.self.schemaKey
          )
        )
  }

  implicit object CirceNormalizeData extends NormalizeData[Json] {
    def normalize(instance: SelfDescribingData[Json]): Json =
      CirceAttachSchemaKeyData.attachSchemaKey(instance.schema, instance.data)
  }

  object StringifySchema extends StringifySchema[Json] {
    def asString(container: SelfDescribingSchema[Json]): String =
      container.normalize(CirceNormalizeSchema).noSpaces
  }

  object StringifyData extends StringifyData[Json] {
    def asString(container: SelfDescribingData[Json]): String =
      container.normalize(CirceNormalizeData).noSpaces
  }
}
