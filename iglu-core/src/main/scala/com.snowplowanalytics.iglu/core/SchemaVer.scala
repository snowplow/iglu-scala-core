/*
 * Copyright (c) 2012-2021 Snowplow Analytics Ltd.. All rights reserved.
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
  * Semantic version for a self-describing schema.
  *
  * - `model` is the schema MODEL, representing a major schema version;
  * - `revision` is the schema REVISION, representing backward-incompatible changes;
  * - `addition` is the schema ADDITION, representing backward-compatible changes.
  */
sealed trait SchemaVer {
  def asString: String

  def getModel: Option[Int]
  def getRevision: Option[Int]
  def getAddition: Option[Int]
}

object SchemaVer {

  def apply(model: Int, revision: Int, addition: Int): SchemaVer =
    Full(model, revision, addition)

  /**
    * An explicit, fully known version. It can be attached to both
    * self-describing data and self-describing schema.
    */
  final case class Full(model: Int, revision: Int, addition: Int) extends SchemaVer {
    def asString = s"$model-$revision-$addition"

    def getModel: Option[Int]    = Some(model)
    def getRevision: Option[Int] = Some(revision)
    def getAddition: Option[Int] = Some(addition)

    /** Get the kind of a specific version component. */
    def get(kind: VersionKind): Int = kind match {
      case VersionKind.Model    => model
      case VersionKind.Revision => revision
      case VersionKind.Addition => addition
    }
  }

  /**
    * A partially known version. It can be attached only to self-describing data.
    * (A self-describing schema must be capable of being looked up by version.)
    */
  final case class Partial(model: Option[Int], revision: Option[Int], addition: Option[Int])
      extends SchemaVer {
    def asString = s"${model.getOrElse("?")}-${revision.getOrElse("?")}-${addition.getOrElse("?")}"

    def getModel: Option[Int]    = model
    def getRevision: Option[Int] = revision
    def getAddition: Option[Int] = addition

    /** Get the kind of a specific version component. */
    def get(kind: VersionKind): Option[Int] = kind match {
      case VersionKind.Model    => model
      case VersionKind.Revision => revision
      case VersionKind.Addition => addition
    }
  }

  /**
    * A regular expression to validate or extract a [[SchemaVer.Full]],
    * with known MODEL, REVISION and ADDITION.
    *
    * The MODEL cannot be 0.
    */
  val schemaVerFullRegex: Regex = "^([1-9][0-9]*)-(0|[1-9][0-9]*)-(0|[1-9][0-9]*)$".r

  /**
    * A regular expression to validate or extract a [[SchemaVer.Partial]],
    * with potentially unknown MODEL, REVISION or ADDITION.
    */
  val schemaVerPartialRegex: Regex =
    ("^([1-9][0-9]*|\\?)-" + // MODEL (cannot start with zero)
      "((?:0|[1-9][0-9]*)|\\?)-" + // REVISION
      "((?:0|[1-9][0-9]*)|\\?)$").r // ADDITION

  /**
    * A default `Ordering` instance for a [[SchemaVer]],
    * in ascending order.
    */
  implicit val ordering: Ordering[SchemaVer] =
    Ordering.by { schemaVer: SchemaVer =>
      (schemaVer.getModel, schemaVer.getRevision, schemaVer.getAddition)
    }

  /**
    * A default `Ordering` instance for a [[SchemaVer.Full]],
    * in ascending order.
    */
  implicit val orderingFull: Ordering[Full] =
    Ordering.by { schemaVer: SchemaVer.Full =>
      (schemaVer.model, schemaVer.revision, schemaVer.addition)
    }

  /**
    * Parse the MODEL, REVISION, and ADDITION components of a [[SchemaVer]],
    * which can be potentially unknown.
    */
  def parse(version: String): Either[ParseError, SchemaVer] =
    parseFull(version) match {
      case Left(ParseError.InvalidSchemaVer) =>
        version match {
          case schemaVerPartialRegex(IntString(m), IntString(r), IntString(a)) =>
            Right(SchemaVer.Partial(m, r, a))
          case _ => Left(ParseError.InvalidSchemaVer)
        }
      case other => other
    }

  /**
    * Parse the MODEL, REVISION, and ADDITION components of a [[SchemaVer.Full]],
    * which are always known.
    */
  def parseFull(version: String): Either[ParseError, SchemaVer.Full] = version match {
    case schemaVerFullRegex(m, r, a) =>
      try {
        Right(SchemaVer.Full(m.toInt, r.toInt, a.toInt))
      } catch {
        case _: NumberFormatException =>
          Left(ParseError.InvalidSchemaVer)
      }
    case _ =>
      Left(ParseError.InvalidSchemaVer)
  }

  /**
    * Check if a string is a valid [[SchemaVer]].
    *
    * @param version The string to be checked.
    * @return `true` if the string is a valid [[SchemaVer]].
    */
  def isValid(version: String): Boolean =
    version.matches(schemaVerFullRegex.toString)

  private object IntString {
    def unapply(arg: String): Option[Option[Int]] =
      if (arg == "?") Some(None)
      else {
        try {
          Some(Some(arg.toInt))
        } catch {
          case _: NumberFormatException => None
        }
      }
  }
}
