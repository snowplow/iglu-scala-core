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

import typeclasses.ExtractSchemaKey

/** Filter self-describing schemas by [[SchemaKey]]. */
final case class SchemaCriterion(
  vendor: String,
  name: String,
  format: String,
  model: Option[Int]    = None,
  revision: Option[Int] = None,
  addition: Option[Int] = None
) {

  /**
    * Check if a [[SchemaKey]] is valid.
    *
    * It's valid if the vendor, name, format, and model all match
    * and the supplied key's REVISION and ADDITION do not exceed the
    * criterion's REVISION and ADDITION.
    *
    * @param key The [[SchemaKey]] to validate.
    * @return `true` if the [[SchemaKey]] is valid.
    */
  def matches(key: SchemaKey): Boolean =
    prefixMatches(key) && verMatch(key.version)

  /**
    * Filter a sequence of entities by this [[SchemaCriterion]].
    *
    * Can be used for getting only the `Right` JSON instances
    * out of an array of custom contexts.
    *
    * Usage:
    * {{{
    *   // This will get the best match for an entity
    *   criterion.takeFrom(_.schema)(entities).sort.getOption
    * }}}
    *
    * @param entities A list of self-describing data blobs.
    * @tparam E The base type of the self-describing data, having
    *           an `ExtractSchemaKey` instance in scope.
    * @return A list of matching entities.
    */
  def pickFrom[E: ExtractSchemaKey](entities: Seq[E]): Seq[E] =
    entities.foldLeft(Seq.empty[E]) { (acc, cur) =>
      SchemaKey.extract(cur) match {
        case Right(key) if this.matches(key) => cur +: acc
        case _                               => acc
      }
    }

  /**
    * Format this [[SchemaCriterion]] as an Iglu schema URI,
    * whereby the REVISION and ADDITION may be replaced with
    * "*" wildcards.
    *
    * @return The string representation of this criterion.
    */
  def asString: String =
    s"iglu:$vendor/$name/$format/$versionString"

  /** Stringify the version of this [[SchemaCriterion]]. */
  def versionString: String =
    "%s-%s-%s".format(model.getOrElse("*"), revision.getOrElse("*"), addition.getOrElse("*"))

  /**
    * Check if the vendor, name, and format are all valid.
    *
    * @param key The [[SchemaKey]] to validate.
    * @return `true` if the first three fields are correct.
    */
  private def prefixMatches(key: SchemaKey): Boolean =
    key.vendor == vendor && key.name == name && key.format == format

  /**
    * Match only [[SchemaVer]].
    *
    * @param ver A [[SchemaVer]] of some other [[SchemaKey]].
    * @return `true` if all specified groups match.
    */
  private[this] def verMatch(ver: SchemaVer): Boolean =
    groupMatch(ver.getModel, model) &&
      groupMatch(ver.getRevision, revision) &&
      groupMatch(ver.getAddition, addition)

  /**
    * Helper function for `verMatch`. Compares two numbers for the same group.
    *
    * @param other The other schema's [[SchemaVer]] group (MODEL, REVISION, ADDITION).
    * @param crit This [[SchemaCriterion]]'s corresponding group.
    * @return `true` if the groups match or if either entities are unknown.
    */
  private[this] def groupMatch(other: Option[Int], crit: Option[Int]): Boolean = crit match {
    case Some(c) if other == Some(c) => true
    case Some(_) if other.isEmpty    => true
    case None                        => true
    case _                           => false
  }
}

/** Companion object, which contains custom constructors for [[SchemaCriterion]]. */
object SchemaCriterion {

  /** Canonical regular expression to extract [[SchemaCriterion]]. */
  val criterionRegex = ("^iglu:" + // Protocol
    "([a-zA-Z0-9-_.]+)/" + // Vendor
    "([a-zA-Z0-9-_]+)/" + // Name
    "([a-zA-Z0-9-_]+)/" + // Format
    "([1-9][0-9]*|\\*)-" + // MODEL (cannot start with zero)
    "((?:0|[1-9][0-9]*)|\\*)-" + // REVISION
    "((?:0|[1-9][0-9]*)|\\*)$").r // ADDITION

  /**
    * A custom constructor for a [[SchemaCriterion]] from
    * a string like:
    * "iglu:com.vendor/schema_name/jsonschema/1-*-*".
    *
    * An Iglu schema URI is the default for schema lookup.
    *
    * @param criterion The string to convert to a [[SchemaCriterion]].
    * @return A [[SchemaCriterion]] if the string satisfies the format.
    */
  def parse(criterion: String): Option[SchemaCriterion] =
    criterion match {
      case criterionRegex(vendor, name, format, m, r, a) =>
        Some(SchemaCriterion(vendor, name, format, parseInt(m), parseInt(r), parseInt(a)))
      case _ => None
    }

  /**
    * Constructs a comprehensive [[SchemaCriterion]].
    *
    * @return the constructed [[SchemaCriterion]].
    */
  def apply(
    vendor: String,
    name: String,
    format: String,
    model: Int,
    revision: Int,
    addition: Int
  ): SchemaCriterion =
    SchemaCriterion(vendor, name, format, Some(model), Some(revision), Some(addition))

  /**
    * Constructs a [[SchemaCriterion]] with everything
    * except ADDITION.
    *
    * @return the constructed [[SchemaCriterion]].
    */
  def apply(
    vendor: String,
    name: String,
    format: String,
    model: Int,
    revision: Int
  ): SchemaCriterion =
    SchemaCriterion(vendor, name, format, Some(model), Some(revision))

  /**
    * Constructs a [[SchemaCriterion]], which is agnostic
    * about REVISION and ADDITION (restricted to MODEL only).
    *
    * @return the constructed [[SchemaCriterion]].
    */
  def apply(vendor: String, name: String, format: String, model: Int): SchemaCriterion =
    SchemaCriterion(vendor, name, format, Some(model), None, None)

  /**
    * Try to parse a string as a number.
    * Helper method for [[parse]].
    */
  private def parseInt(number: String): Option[Int] =
    try {
      Some(number.toInt)
    } catch {
      case _: NumberFormatException => None
    }
}
