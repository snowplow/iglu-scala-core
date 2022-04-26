/*
 * Copyright (c) 2016-2022 Snowplow Analytics Ltd. All rights reserved.
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
  * A mixin for [[ExtractSchemaKey]], signalling that this particular instance of
  * [[ExtractSchemaKey]] is intended for extracting data, not schema.
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
  * which contains information about a user that can be
  * extracted as [[SelfDescribingData]].
  */
trait ToData[E] { self: ExtractSchemaKey[E] =>
  def toData(entity: E): Either[ParseError, SelfDescribingData[E]] =
    getContent(entity) match {
      case Right(content) =>
        self.extractSchemaKey(entity) match {
          case Right(key)  => Right(SelfDescribingData(key, content))
          case Left(error) => Left(error)
        }
      case Left(error) =>
        Left(error)
    }

  protected def getContent(entity: E): Either[ParseError, E]
}
