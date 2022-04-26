/*
 * Copyright (c) 2012-2022 Snowplow Analytics Ltd.. All rights reserved.
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

import typeclasses.{NormalizeData, StringifyData, ToData}

/**
  * A container for self-describing data, used to eliminate the
  * need for an `Option` wrapper when extracting a [[SchemaKey]]
  * with the [[typeclasses.ExtractSchemaKey]] type class.
  *
  * @param schema A reference to a self-describing schema.
  * @param data The data blob itself.
  * @tparam D Any generic type that can represent a piece of
  *           self-describing data. (See also [[typeclasses.ExtractSchemaKey]].)
  */
final case class SelfDescribingData[D](schema: SchemaKey, data: D) {

  /**
    * Render a piece of self-describing data into its base type `D`.
    */
  def normalize(implicit ev: NormalizeData[D]): D = ev.normalize(this)

  /**
    * Render a piece of self-describing data into `String`.
    */
  def asString(implicit ev: StringifyData[D]): String = ev.asString(this)
}

object SelfDescribingData {

  /** Try to decode `D` as [[SelfDescribingData]]]. */
  def parse[D](data: D)(implicit ev: ToData[D]): Either[ParseError, SelfDescribingData[D]] =
    ev.toData(data)
}
