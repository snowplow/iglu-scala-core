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
package typeclasses

/**
  * This type class can be used to extract a [[SchemaMap]] from a
  * self-describing schema.
  *
  * The [[SchemaMap]] contains details about the schema itself that
  * allow it to be identified.
  *
  * An example input is:
  *
  * {{{
  *    {
  *      "\$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
  *      "description": "Schema for a user entity",
  *      "self": {
  *         "vendor": "com.vendor",
  *         "name": "user_entity",
  *         "format": "jsonschema",
  *         "version": "1-0-0"
  *      },
  *      "type": "object",
  *      "properties": {
  *         "id": {
  *           "type": "string"
  *         },
  *         "email": {
  *           "type": "string"
  *         }
  *      }
  *    }
  * }}}
  *
  * where "self" contains information about the vendor, name, format and
  * version of the schema and can be extracted as [[SchemaMap]].
  *
  * @tparam E Any schema that can include a reference to itself.
  *           It's mostly intended for JSON schema,
  *           but can also be something like Thrift, Map[String, String], etc.
  */
trait ExtractSchemaMap[E] {

  /**
    * Try to extract [[SchemaMap]] from a self-describing schema entity.
    */
  def extractSchemaMap(entity: E): Either[ParseError, SchemaMap]
}
