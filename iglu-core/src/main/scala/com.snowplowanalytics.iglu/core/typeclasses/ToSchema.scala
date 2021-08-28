/*
 * Copyright (c) 2016-2021 Snowplow Analytics Ltd. All rights reserved.
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
  * A mixin for [[ExtractSchemaMap]], signalling that this particular instance of
  * [[ExtractSchemaMap]] is intended for extracting schema, not data.
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
  * which contains a schema that can be extracted as a [[SelfDescribingSchema]].
  */
trait ToSchema[E] { self: ExtractSchemaMap[E] =>
  def toSchema(schema: E): Either[ParseError, SelfDescribingSchema[E]] =
    for {
      _         <- self.checkSchemaUri(schema)
      schemaMap <- self.extractSchemaMap(schema)
    } yield SelfDescribingSchema(schemaMap, getContent(schema))

  protected def getContent(entity: E): E

  /** Validate the URI specified with the "\$schema" keyword. */
  protected def checkSchemaUri(entity: E): Either[ParseError, Unit]
}
