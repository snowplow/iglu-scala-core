/*
 * Copyright (c) 2016-2020 Snowplow Analytics Ltd. All rights reserved.
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
package json4s

import org.json4s._
import org.json4s.JsonDSL._

/**
  * Json4s serializers for Iglu entities,
  * such as self-describing schema and self-describing data.
  */
object Json4sIgluCodecs {

  // Public formats. Import it.
  lazy val formats: Formats = schemaFormats + SchemaSerializer + DataSerializer

  // Local formats
  implicit private val schemaFormats: Formats = DefaultFormats + SchemaVerSerializer

  /**
    * Extract `SchemaVer` (*-*-*) from `JValue`.
    */
  object SchemaVerSerializer
      extends CustomSerializer[SchemaVer.Full](
        _ =>
          (
            {
              case JString(version) =>
                SchemaVer.parse(version) match {
                  case Right(schemaVer: SchemaVer.Full) => schemaVer
                  case _                                => throw new MappingException("Can't convert " + version + " to SchemaVer")
                }
              case x => throw new MappingException("Can't convert " + x + " to SchemaVer")
            }, {
              case x: SchemaVer => JString(x.asString)
            }
          )
      )

  /**
    * Extract `SchemaKey` from the "self" property of a
    * self-describing schema, and the remaining properties as schema body.
    */
  object SchemaSerializer
      extends CustomSerializer[SelfDescribingSchema[JValue]](
        _ =>
          (
            {
              case fullSchema: JObject =>
                val schemaMap = SchemaMap((fullSchema \ "self").extract[SchemaKey])
                val schema    = removeMetaFields(fullSchema)
                SelfDescribingSchema(schemaMap, schema)
              case _ => throw new MappingException("Not an JSON object")
            }, {
              case SelfDescribingSchema(self, schema: JValue) =>
                JObject(
                  ("self", Extraction.decompose(self.schemaKey)),
                  (
                    s"$$schema",
                    Extraction.decompose(SelfDescribingSchema.SelfDescribingUri.toString)
                  )
                ).merge(schema)
            }
          )
      )

  /**
    * Extract `SchemaKey` from the "schema" property,
    * and data from the "data" property.
    */
  object DataSerializer
      extends CustomSerializer[SelfDescribingData[JValue]](
        _ =>
          (
            {
              case fullInstance: JObject =>
                val schemaKey =
                  (fullInstance \ "schema")
                    .extractOpt[String]
                    .flatMap(SchemaKey.fromUri(_).toOption)
                    .getOrElse {
                      throw new MappingException(
                        "Does not contain schema key with valid Schema URI"
                      )
                    }
                val data = fullInstance \ "data" match {
                  case JNothing     => throw new MappingException("Does not contain data")
                  case json: JValue => json
                }
                SelfDescribingData(schemaKey, data)
              case _ => throw new MappingException("Not an JSON object")
            }, {
              case SelfDescribingData(key, data: JValue) =>
                JObject(("schema", JString(key.toSchemaUri)) :: ("data", data) :: Nil)
            }
          )
      )

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
