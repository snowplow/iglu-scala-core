/*
 * Copyright (c) 2012-2020 Snowplow Analytics Ltd.. All rights reserved.
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

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.compact

import com.snowplowanalytics.iglu.core.typeclasses._

/**
  * This module contains examples of common traits and type class instances
  * based on Json4s library
  */
object IgluCoreCommon {

  implicit val formats: Formats = IgluJson4sCodecs.formats

  ////////////////////////
  // ExtractFrom Json4s //
  ////////////////////////

  /** Example common trait for [[ExtractSchemaKey]] *data* objects */
  trait Json4SExtractSchemaKeyData extends ExtractSchemaKey[JValue] {
    def extractSchemaKey(entity: JValue): Either[ParseError, SchemaKey] =
      entity \ "schema" match {
        case JString(schema) => SchemaKey.fromUri(schema)
        case _               => Left(ParseError.InvalidData)
      }
  }

  /**
    * Example of [[ExtractSchemaKey]] instance for json4s JSON *data*
    */
  implicit object Json4SExtractSchemaKeyData extends Json4SExtractSchemaKeyData

  /** Example common trait for [[ExtractSchemaKey]] *Schemas* objects */
  trait Json4SExtractSchemaKeySchema extends ExtractSchemaKey[JValue] {

    /**
      * Extract SchemaKey usning serialization formats defined at [[IgluJson4sCodecs]]
      */
    def extractSchemaKey(entity: JValue): Either[ParseError, SchemaKey] =
      (entity \ "self").extractOpt[SchemaKey] match {
        case None       => Left(ParseError.InvalidSchema)
        case Some(self) => Right(self)
      }
  }

  /**
    * Example of [[ExtractSchemaKey]] instance for json4s JSON *Schemas*
    */
  implicit object Json4SExtractSchemaKeySchema extends Json4SExtractSchemaKeySchema

  /////////////////////
  // AttachTo Json4s //
  /////////////////////

  // Schemas

  implicit object Json4SAttachSchemaKeySchema extends ExtractSchemaKey[JValue] {
    def extractSchemaKey(entity: JValue): Either[ParseError, SchemaKey] =
      (entity \ "self").extractOpt[SchemaKey] match {
        case Some(self) => Right(self)
        case None       => Left(ParseError.InvalidSchema)
      }
  }

  implicit object Json4SAttachSchemaMapComplex
      extends ExtractSchemaMap[JValue]
      with ToSchema[JValue] {
    def extractSchemaMap(entity: JValue): Either[ParseError, SchemaMap] = {
      implicit val formats: Formats = IgluJson4sCodecs.formats
      (entity \ "self").extractOpt[SchemaKey].map(key => SchemaMap(key)) match {
        case Some(map) => Right(map)
        case None      => Left(ParseError.InvalidSchema)
      }
    }

    def checkSchemaUri(entity: JValue): Either[ParseError, Unit] =
      (entity \ "$schema").extractOpt[String] match {
        case Some(schemaUri) if schemaUri == SelfDescribingSchema.SelfDescribingUri.toString =>
          Right(())
        case _ => Left(ParseError.InvalidMetaschema)
      }

    /**
      * Remove key with `self` description
      * `getContent` required to be implemented here because it extends [[ToSchema]]
      */
    def getContent(json: JValue): JValue =
      removeMetaFields(json)
  }

  // Data

  implicit object Json4SAttachSchemaKeyData
      extends ExtractSchemaKey[JValue]
      with ToData[JValue]
      with Json4SExtractSchemaKeyData {

    def getContent(json: JValue): Either[ParseError, JValue] =
      json \ "data" match {
        case data: JObject => Right(data)
        case _             => Left(ParseError.InvalidData)
      }

    def attachSchemaKey(schemaKey: SchemaKey, instance: JValue): JValue =
      ("schema" -> schemaKey.toSchemaUri) ~ ("data" -> instance)
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

  implicit object Json4SNormalizeSchema extends NormalizeSchema[JValue] {
    def normalize(schema: SelfDescribingSchema[JValue]): JValue =
      Extraction.decompose(schema)
  }

  implicit object Json4SNormalizeData extends NormalizeData[JValue] {
    def normalize(instance: SelfDescribingData[JValue]): JValue =
      Extraction.decompose(instance)
  }

  object StringifySchema extends StringifySchema[JValue] {
    def asString(container: SelfDescribingSchema[JValue]): String =
      compact(container.normalize(Json4SNormalizeSchema))
  }

  object StringifyData extends StringifyData[JValue] {
    def asString(container: SelfDescribingData[JValue]): String =
      compact(container.normalize(Json4SNormalizeData))
  }

  /////////
  // Aux //
  /////////

  def removeMetaFields(json: JValue): JValue = json match {
    case JObject(fields) =>
      fields.filterNot {
        case ("self", JObject(keys)) => intersectsWithSchemakey(keys)
        case ("$schema", _)          => true
        case _                       => false
      }
    case jvalue => jvalue
  }

  private def intersectsWithSchemakey(fields: List[JField]): Boolean =
    fields.map(_._1).toSet.diff(Set("name", "vendor", "format", "version")).isEmpty
}
