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

import typeclasses.ExtractSchemaMap

/**
  * Contains details about a self-describing schema.
  *
  * Provides the same information as a [[SchemaKey]] but is used when dealing
  * with schema, not data.
  *
  * For an example, see [[typeclasses.ExtractSchemaMap]].
  */
final case class SchemaMap(schemaKey: SchemaKey) extends AnyVal

/** Companion object, which contains a custom constructor for [[SchemaMap]]. */
object SchemaMap {

  /**
    * A regular expression to extract a [[SchemaKey]] from a path.
    *
    * TODO: Is this needed?
    */
  val schemaPathRegex = ("^([a-zA-Z0-9-_.]+)/" +
    "([a-zA-Z0-9-_]+)/" +
    "([a-zA-Z0-9-_]+)/" +
    "([1-9][0-9]*" +
    "(?:-(?:0|[1-9][0-9]*)){2})$").r

  /** A regular expression to extract the [[SchemaVer]] separately. */
  private val schemaPathRigidRegex = ("^([a-zA-Z0-9-_.]+)/" +
    "([a-zA-Z0-9-_]+)/" +
    "([a-zA-Z0-9-_]+)/" +
    "([0-9]*(?:-(?:[0-9]*)){2})$").r

  def apply(vendor: String, name: String, format: String, version: SchemaVer.Full): SchemaMap =
    SchemaMap(SchemaKey(vendor, name, format, version))

  /**
    * A custom constructor for a [[SchemaMap]] from
    * an Iglu schema path, which looks like:
    * "com.vendor/schema_name/jsonschema/1-0-0".
    *
    * Can be used to get information about a slef-describing schema
    * from a path on the file system, where the schema is stored.
    *
    * @param schemaPath An Iglu schema path.
    * @return A [[SchemaMap]] or an error.
    */
  def fromPath(schemaPath: String): Either[ParseError, SchemaMap] = schemaPath match {
    case schemaPathRigidRegex(vnd, n, f, ver) =>
      SchemaVer.parse(ver) match {
        case Right(v: SchemaVer.Full)    => Right(SchemaMap(vnd, n, f, v))
        case Right(_: SchemaVer.Partial) => Left(ParseError.InvalidSchemaVer)
        case Left(other)                 => Left(other)
      }
    case _ => Left(ParseError.InvalidSchema)
  }

  /** Try to get `SchemaMap` from `E` as */
  def extract[E: ExtractSchemaMap](e: E): Either[ParseError, SchemaMap] =
    implicitly[ExtractSchemaMap[E]].extractSchemaMap(e)
}
