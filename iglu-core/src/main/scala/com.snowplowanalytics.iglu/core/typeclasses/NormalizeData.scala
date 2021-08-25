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
  * Type class to render self-describing data into it base type `D`.
  *
  * @tparam D Any generic type that can represent a piece of
  *           self-describing data. (See also [[ExtractSchemaKey]].)
  */
trait NormalizeData[D] {

  /**
    * Render a piece of self-describing data into its base type `D`.
    */
  def normalize(container: SelfDescribingData[D]): D
}
