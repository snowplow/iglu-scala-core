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

import java.net.URI

import typeclasses.{NormalizeSchema, StringifySchema, ToSchema}

/**
  * A container for a self-describing schema, used to eliminate the
  * need for an `Option` wrapper when extracting a [[SchemaMap]]
  * with the [[typeclasses.ExtractSchemaMap]] type class.
  *
  * @param self Information about the schema.
  * @param schema The schema itself.
  * @tparam S Any generic type that can represent a
  *           self-describing schema. (See also [[typeclasses.ExtractSchemaMap]].)
  */
final case class SelfDescribingSchema[S](self: SchemaMap, schema: S) {

  /**
    * Render a self-describing schema into its base type `S`.
    */
  def normalize(implicit ev: NormalizeSchema[S]): S = ev.normalize(this)

  /**
    * Render a self-describing schema into `String`.
    */
  def asString(implicit ev: StringifySchema[S]): String = ev.asString(this)
}

object SelfDescribingSchema {

  val SelfDescribingUri: URI = URI.create(
    "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#"
  )

  /** Try to decode `S` as [[SelfDescribingSchema]]]. */
  def parse[S](schema: S)(implicit ev: ToSchema[S]): Either[ParseError, SelfDescribingSchema[S]] =
    ev.toSchema(schema)
}
