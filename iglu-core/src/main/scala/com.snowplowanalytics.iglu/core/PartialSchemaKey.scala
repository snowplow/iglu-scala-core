/*
 * Copyright (c) 2016-2023 Snowplow Analytics Ltd. All rights reserved.
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

import scala.util.matching.Regex

/**
  * Contains details about the schema of a piece of self-describing data.
  *
  * Unlike [[SchemaKey]], the schema version may not be fully known.
  */
final case class PartialSchemaKey(
  vendor: String,
  name: String,
  format: String,
  version: SchemaVer
) {

  /** Convert this [[PartialSchemaKey]] to an Iglu schema URI. */
  def toSchemaUri: String =
    s"iglu:$vendor/$name/$format/${version.asString}"

  /** Convert this [[PartialSchemaKey]] to a fully-known [[SchemaKey]]. */
  def toSchemaKey: Option[SchemaKey] =
    version match {
      case full: SchemaVer.Full =>
        Some(SchemaKey(vendor, name, format, full))
      case _ => None
    }
}

/** Companion object, which contains a custom constructor for [[PartialSchemaKey]]. */
object PartialSchemaKey {

  /** Canonical regular expression for a [[PartialSchemaKey]]. */
  val schemaUriRegex: Regex = ("^iglu:" + // Protocol
    "([a-zA-Z0-9-_.]{1,128})/" + // Vendor
    "([a-zA-Z0-9-_]{1,128})/" + // Name
    "([a-zA-Z0-9-_]{1,128})/" + // Format
    "([1-9][0-9]*|\\?)-" + // MODEL (cannot start with zero)
    "((?:0|[1-9][0-9]*)|\\?)-" + // REVISION
    "((?:0|[1-9][0-9]*)|\\?)").r // ADDITION

  /**
    * A custom constructor for a [[PartialSchemaKey]] from
    * an Iglu schema URI, which looks like:
    * "iglu:com.vendor/schema_name/jsonschema/1-0-0" for a fully known version or
    * "iglu:com.vendor/schema_name/jsonschema/1-?-?" for a partially known version.
    *
    * An Iglu schema URI is the default for schema lookup.
    *
    * @param schemaUri An Iglu schema URI.
    * @return A [[PartialSchemaKey]] or an error.
    */
  def fromUri(schemaUri: String): Either[ParseError, PartialSchemaKey] =
    Parser.uriPrefix(schemaUri) match {
      case Some(prefix) =>
        val version =
          Parser.version(schemaUri, prefix.versionFrom, schemaUri.length, Parser.PartialWildcard)
        if (version.isFull)
          Right(PartialSchemaKey(prefix.vendor, prefix.name, prefix.format, version.toFull))
        else if (version.isKnownOrWildcard)
          Right(PartialSchemaKey(prefix.vendor, prefix.name, prefix.format, version.toPartial))
        else if (version.hasWildcardShape) Left(ParseError.InvalidSchemaVer)
        else Left(ParseError.InvalidIgluUri)
      case None => Left(ParseError.InvalidIgluUri)
    }
}
