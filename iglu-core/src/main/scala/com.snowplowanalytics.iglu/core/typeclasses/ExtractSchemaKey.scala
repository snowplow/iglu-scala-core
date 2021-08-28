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
  * This type class can be used to extract a [[SchemaKey]] from a piece
  * of self-describing data.
  *
  * The [[SchemaKey]] contains details about the schema of the data.
  *
  * An example input is:
  *
  * {{{
  *   {
  *      "schema": "iglu:com.vendor/user_entity/jsonschema/1-0-0",
  *      "data": {
  *         "id": "78abb66e-a5ad-4772-96ec-d880650cd0b2",
  *         "email": "some_user@example.com"
  *      }
  *    }
  * }}}
  *
  * where "schema" contains information about the vendor, name, format and
  * version of the schema and can be extracted as [[SchemaKey]].
  *
  * @tparam E Any type that can include a reference to its own schema.
  *           It's mostly intended for various JSON ADTs,
  *           like Json4s, Jackson, Circe, Argonaut et al,
  *           but can also be something like Thrift, Map[String, String], etc.
  */
trait ExtractSchemaKey[E] {

  /**
    * Try to extract [[SchemaKey]] from a self-describing data entity.
    */
  def extractSchemaKey(entity: E): Either[ParseError, SchemaKey]
}
