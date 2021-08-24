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
package com.snowplowanalytics.iglu
package core

import scala.util.matching.Regex

import typeclasses.{ExtractSchemaKey, NormalizeData}

/**
  * Contains details about the schema of a piece of self-describing data.
  *
  * Provides the same information as a [[SchemaMap]] but is used when dealing
  * with data, not schema.
  *
  * For an example, see [[typeclasses.ExtractSchemaKey]].
  *
  * Unlike [[PartialSchemaKey]], the schema version is always fully known.
  */
final case class SchemaKey(vendor: String, name: String, format: String, version: SchemaVer.Full) {

  /** Convert this [[SchemaKey]] to an Iglu schema URI. */
  def toSchemaUri: String =
    s"iglu:$vendor/$name/$format/${version.asString}"

  /**
    * Convert this [[SchemaKey]] to a path that is compatible
    * with most local and remote Iglu schema repositories.
    */
  def toPath: String =
    s"$vendor/$name/$format/${version.asString}"

  /** Lossy conversion to [[PartialSchemaKey]]. */
  def asPartial: PartialSchemaKey =
    PartialSchemaKey(vendor, name, format, version)

  /** Convert this [[SchemaKey]] to a [[SchemaMap]].
    *
    * They both provide the same information but the former is used
    * when dealing with data, and the latter when dealing with schema.
    */
  def toSchemaMap: SchemaMap =
    SchemaMap(this)

  /**
    * Attach this [[SchemaKey]] to a piece of self-describing data,
    * without changing the structure of the data's base type `E`.
    */
  def attachTo[E: NormalizeData](entity: E): E =
    implicitly[NormalizeData[E]].normalize(SelfDescribingData(this, entity))
}

/** Companion object, which contains a custom constructor for [[SchemaKey]]. */
object SchemaKey {

  /** Canonical regular expression for a [[SchemaKey]]. */
  val schemaUriRegex: Regex = ("^iglu:" + // Protocol
    "([a-zA-Z0-9-_.]+)/" + // Vendor
    "([a-zA-Z0-9-_]+)/" + // Name
    "([a-zA-Z0-9-_]+)/" + // Format
    "([1-9][0-9]*" + // MODEL (cannot start with 0)
    "(?:-(?:0|[1-9][0-9]*)){2})$").r // REVISION and ADDITION

  /** A regular expression to extract [[SchemaVer]] within a single group. */
  private val schemaUriRigidRegex: Regex = ("^iglu:" + // Protocol
    "([a-zA-Z0-9-_.]+)/" + // Vendor
    "([a-zA-Z0-9-_]+)/" + // Name
    "([a-zA-Z0-9-_]+)/" + // Format
    "([0-9]*(?:-(?:[0-9]*)){2})$").r // SchemaVer

  /**
    * A Default `Ordering` instance for a [[SchemaKey]],
    * which sorts keys alphabetically AND by ascending [[SchemaVer]].
    *
    * Usage:
    * {{{
    *   import com.snowplowanalytics.iglu.core.SchemaKey
    *   implicit val schemaOrdering = SchemaKey.ordering
    *   keys.sorted
    * }}}
    */
  val ordering: Ordering[SchemaKey] =
    Ordering.by { key: SchemaKey =>
      (key.vendor, key.name, key.format, key.version)
    }

  /**
    * A custom constructor for a [[SchemaKey]] from
    * an Iglu schema URI, which looks like:
    * "iglu:com.vendor/schema_name/jsonschema/1-0-0".
    *
    * An Iglu schema URI is the default for schema lookup.
    *
    * @param schemaUri An Iglu schema URI.
    * @return A [[SchemaKey]] or an error.
    */
  def fromUri(schemaUri: String): Either[ParseError, SchemaKey] = schemaUri match {
    case schemaUriRigidRegex(vnd, n, f, ver) =>
      SchemaVer.parse(ver) match {
        case Right(full: SchemaVer.Full) => Right(SchemaKey(vnd, n, f, full))
        case _                           => Left(ParseError.InvalidSchemaVer)
      }
    case _ => Left(ParseError.InvalidIgluUri)
  }

  /**
    * Extract a [[SchemaKey]] from an entity with base type `E`,
    * representing a piece of self-describing data.
    */
  def extract[E: ExtractSchemaKey](e: E): Either[ParseError, SchemaKey] =
    implicitly[ExtractSchemaKey[E]].extractSchemaKey(e)
}
