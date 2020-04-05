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

/**
  * A [[Resolver]] allows fetching and validating self-describing schemas
  * for entities of type `A`.
  *
  * Resolvers are meant to be implemented as separate artifacts.
  *
  * @tparam F An effect wrapping the [[Resolver]]'s work,
  *           such as `Either[String, Option[A]]` or `IO[A]`.
  * @tparam A An AST for data or schema.
  */
trait Resolver[F[_], A] {

  /** Look up a schema by its key. */
  def lookup(data: SchemaKey): F[SelfDescribingSchema[A]]

  /** Validate a piece of self-describing data against a schema. */
  def validate(
    data: SelfDescribingData[A],
    schema: SelfDescribingSchema[A]
  ): F[Either[String, Unit]]
}
