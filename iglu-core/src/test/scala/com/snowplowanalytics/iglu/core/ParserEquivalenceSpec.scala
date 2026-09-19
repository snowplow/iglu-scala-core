/*
 * Copyright (c) 2016-2026 Snowplow Analytics Ltd. All rights reserved.
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

import org.specs2.Specification

/**
  * The core entities are parsed by hand rather than by matching their canonical regular
  * expressions, because parsing an Iglu URI happens once per self-describing entity per
  * event and a regex match is comparatively expensive.
  *
  * The regexes are still the specification of the format, so this spec keeps the two in
  * step. `Reference` below is the regex-based implementation that the hand-written parsers
  * replaced, and every parser must agree with it - including on which `ParseError` comes
  * back - over a generated corpus of inputs.
  *
  * A deliberate change of behaviour means changing `Reference` to match, and the regex
  * with it where the regex can express the change.
  */
class ParserEquivalenceSpec extends Specification {
  def is = s2"""
  The hand-written parsers agree with the canonical regexes on
    SchemaKey.fromUri $e1
    SchemaMap.fromPath $e2
    PartialSchemaKey.fromUri $e3
    SchemaCriterion.parse $e4
    SchemaVer.parse $e5
    SchemaVer.parseFull $e6
    SchemaVer.isValid $e7

  The corpus itself
    covers a decent number of inputs $e8
    contains inputs that every parser accepts $e9
    everything the parsers accept also matches the published regexes $e10
    everything the published regexes match is at least a well-formed URI or path $e11
  """

  def e1 = disagreements(uris)(SchemaKey.fromUri, Reference.schemaKeyFromUri) must beEmpty
  def e2 = disagreements(paths)(SchemaMap.fromPath, Reference.schemaMapFromPath) must beEmpty
  def e3 = disagreements(uris)(PartialSchemaKey.fromUri, Reference.partialFromUri) must beEmpty
  def e4 = disagreements(uris)(SchemaCriterion.parse, Reference.criterionParse) must beEmpty
  def e5 = disagreements(versions)(SchemaVer.parse, Reference.schemaVerParse) must beEmpty
  def e6 = disagreements(versions)(SchemaVer.parseFull, Reference.schemaVerParseFull) must beEmpty
  def e7 = disagreements(versions)(SchemaVer.isValid, Reference.schemaVerIsValid) must beEmpty

  def e8 =
    (uris.size must be_>(10000))
      .and(paths.size must be_>(10000))
      .and(schemaVers.size must be_>(50))
      .and(versions.size must be_>(10000))

  def e9 = {
    val accepted = uris.count(SchemaKey.fromUri(_).isRight)
    (accepted must be_>(100)).and(
      uris.count(SchemaCriterion.parse(_).isDefined) must be_>(100)
    )
  }

  /**
    * `Reference` compares against private copies of the two rigid regexes that were
    * deleted with the code using them, which leaves [[SchemaKey.schemaUriRegex]] and
    * [[SchemaMap.schemaPathRegex]] with no caller at all - so nothing else in the repo
    * would notice if a parser and its published regex drifted apart. Everything a parser
    * accepts has to at least match the canonical shape. Only that direction holds: the
    * parsers are stricter, rejecting a version whose components do not fit in an `Int`.
    */
  def e10 = {
    val looseUris =
      uris.filter(SchemaKey.fromUri(_).isRight).filterNot(matchedBy(SchemaKey.schemaUriRegex))
    val loosePaths =
      paths.filter(SchemaMap.fromPath(_).isRight).filterNot(matchedBy(SchemaMap.schemaPathRegex))
    (looseUris must beEmpty).and(loosePaths must beEmpty)
  }

  /**
    * The other direction, and the one that catches a published regex being *relaxed*
    * without the parser following: a vendor class gaining a character, or `{1,128}`
    * becoming `{1,256}`, would leave [[e10]] green while a caller that pre-validated with
    * the canonical regex started getting `InvalidIgluUri` back. The only disagreement
    * allowed is `InvalidSchemaVer`, for a version the regex admits but no `Int` can hold.
    */
  def e11 = {
    val rejectedUris = uris
      .filter(matchedBy(SchemaKey.schemaUriRegex))
      .filter(SchemaKey.fromUri(_) == Left(ParseError.InvalidIgluUri))
    val rejectedPaths = paths
      .filter(matchedBy(SchemaMap.schemaPathRegex))
      .filter(SchemaMap.fromPath(_) == Left(ParseError.InvalidSchema))
    (rejectedUris must beEmpty).and(rejectedPaths must beEmpty)
  }

  /** `Regex.matches` arrived in 2.13 and this cross-builds to 2.12. */
  private def matchedBy(regex: Regex)(input: String): Boolean =
    regex.pattern.matcher(input).matches()

  /** Every input on which the two implementations disagree, with both of their answers. */
  private def disagreements[A, B](
    corpus: List[String]
  )(actual: String => B, reference: String => B): List[(String, B, B)] =
    corpus.flatMap { input =>
      val a = actual(input)
      val r = reference(input)
      if (a == r) None else Some((input, a, r))
    }

  private val protocols = List("iglu:", "iglu", "IGLU:", "", "iglu:/")

  private val vendors = List(
    "com.acme",
    "com.acme-sub_division",
    "COM.Acme9",
    "a",
    "_",
    "com.acme.",
    ".com.acme",
    "a" * 128,
    "a" * 129,
    "com/acme",
    "com acme",
    "com.acme!",
    ""
  )

  private val names =
    List(
      "event",
      "second-event_complex",
      "Event1",
      "9lives",
      "-event",
      "e",
      "ev.ent",
      "a" * 129,
      ""
    )

  private val formats = List("jsonschema", "avro", "JSONSchema2", "json.schema", "json/schema", "")

  private val schemaVers = List(
    // valid full versions
    "1-0-0",
    "2-10-32",
    "10-20-30",
    "1-0-10",
    "2147483647-2147483647-2147483647",
    // zeroes and prepended zeroes
    "0-0-0",
    "01-0-0",
    "1-01-0",
    "1-0-01",
    "1-00-0",
    "000-0-0",
    "1-0-0",
    // missing or empty components
    "",
    "1",
    "1-0",
    "-",
    "--",
    "---",
    "1--0",
    "-1-0",
    "1-2-",
    "1-0-0-0",
    "1-0-0-",
    // partial versions
    "1-?-?",
    "?-?-?",
    "?-0-0",
    "1-0-?",
    "1-2-?",
    "0-?-?",
    "?",
    "?-?",
    "1-??-?",
    // criterion wildcards
    "*-*-*",
    "2-0-*",
    "*-0-0",
    "1-0-*",
    "1-*-0",
    "0-*-*",
    "*",
    "*-*-",
    "1-**-*",
    // out of Int range
    "2147483648-0-0",
    "1-2147483648-0",
    "1-0-2147483648",
    "2147483650-0-0",
    "99999999999999999999-0-0",
    "1-99999999999999999999-0",
    // junk following an overflowing run of digits, which must stay Malformed rather than
    // being mistaken for a well-formed number that merely does not fit in an Int
    "2147483648x-0-0",
    "99999999999999999999x-0-0",
    "1-99999999999999999999x-0",
    "1-0-99999999999999999999x",
    "12345678901/x-0-0",
    // a space, which is what Parser uses internally to mean "no wildcard allowed"
    " ",
    " - - ",
    "1- -0",
    "1-0- ",
    // junk
    "1_0_0",
    "x-0-0",
    "1-x-0",
    "1-0-x",
    "1-0-0 ",
    " 1-0-0",
    "1-0-0\n",
    "1-0-0\t",
    "1-0-0#",
    "1-0-0/x",
    "1-0-0/1-0-0",
    " 1-0-0",
    "١-٠-٠" // Arabic-Indic digits, which Java's \d does not match by default
  )

  /**
    * Randomly generated versions, to reach shapes a hand-written list does not think of.
    * The list above originally had overflowing components and it had junk components, but
    * nothing that combined the two, and that gap hid a real bug.
    *
    * Half the versions are assembled from components drawn from the shapes that have
    * actually hidden bugs here - long digit runs, digit runs followed by junk, prepended
    * zeroes, wildcards, empties. Sampling characters uniformly instead, as the other half
    * does, makes a run of ten or more digits followed by junk rare enough that the corpus
    * would find it only by luck, and that coverage would then quietly disappear if anyone
    * changed the seed or the length range. The uniform half is still worth keeping, since
    * it reaches shapes the component pool does not enumerate.
    *
    * The alphabets include both wildcards, a letter, a slash and a space, the last being
    * what `Parser` uses internally to mean "no wildcard allowed". Seeded, so any failure
    * is reproducible.
    */
  private val randomVersions: List[String] = {
    val alphabet = "0123456789-?*x/ ".toCharArray
    val junk     = "?*x/ .".toCharArray
    val random   = new scala.util.Random(20260907L)

    def digits(length: Int): String =
      new String(Array.fill(length)(('0' + random.nextInt(10)).toChar))

    def noise(length: Int): String =
      new String(Array.fill(length)(alphabet(random.nextInt(alphabet.length))))

    // 8 to 14 digits straddles the 10-digit boundary where Int overflow starts
    def component(): String =
      random.nextInt(10) match {
        case 0 => ""
        case 1 => "?"
        case 2 => "*"
        case 3 => digits(1)
        case 4 => digits(1 + random.nextInt(3))
        case 5 => "0" + digits(random.nextInt(3))
        case 6 => digits(8 + random.nextInt(7))
        case 7 => digits(8 + random.nextInt(7)) + junk(random.nextInt(junk.length))
        case 8 => digits(1 + random.nextInt(3)) + junk(random.nextInt(junk.length))
        case _ => noise(1 + random.nextInt(6))
      }

    val assembled = List.fill(10000)(List.fill(1 + random.nextInt(4))(component()).mkString("-"))
    val uniform   = List.fill(10000)(noise(random.nextInt(24) + 1))
    assembled ++ uniform
  }

  /** Every version string, hand-written and generated, for the [[SchemaVer]] parsers. */
  private val versions = schemaVers ++ randomVersions

  private val crossPaths =
    for {
      vendor  <- vendors
      name    <- names
      format  <- formats
      version <- schemaVers
    } yield s"$vendor/$name/$format/$version"

  private val paths =
    crossPaths ++ randomVersions.map(version => s"com.acme/event/jsonschema/$version")

  private val uris =
    protocols.flatMap(protocol => crossPaths.map(protocol + _)) ++
      randomVersions.map(version => s"iglu:com.acme/event/jsonschema/$version") ++ List(
      // a handful of shapes the cross product above cannot produce
      "",
      "iglu:",
      "iglu:/",
      "iglu://",
      "iglu:///",
      "iglu:////",
      "iglu:com.acme/event/jsonschema",
      "iglu:com.acme/event/jsonschema/",
      "iglu:com.acme/event/jsonschema/1-0-0/extra",
      "iglu:com.acme//jsonschema/1-0-0",
      "iglu:com.acme/event/jsonschema/1-0-0",
      "IGLU:com.acme/event/jsonschema/1-0-0",
      "iglu :com.acme/event/jsonschema/1-0-0",
      " iglu:com.acme/event/jsonschema/1-0-0",
      "iglu%3Auk.edu.acme.sub-division%2Fsecond-event_complex%2Fjsonschema%2F2-10-32",
      "http://iglucentral.com/schemas/com.acme/event/jsonschema/1-0-0",
      "com.acme/event/jsonschema/1-0-0",
      "iglu:com.acme/event/jsonschema/1-0-0#",
      "iglu:com.acme/event/jsonschema/1-0-0?query=1"
    )

  /**
    * The regex-based implementation that the hand-written parsers replaced, kept so that
    * the two can be compared. The two "rigid" regexes were private to their companions and
    * were deleted along with the code that used them; every other regex is the published
    * one, read straight off the entity it belongs to.
    *
    * A regex cannot express the range of an `Int`, so each reference parser is a regex
    * match plus that one extra check: `schemaVerParseFull` catches the
    * `NumberFormatException`, and the `IntString` and `StarString` extractors turn an
    * out-of-range component into a non-match, so `partialFromUri` and `criterionParse`
    * both reject one.
    */
  private object Reference {

    private val schemaUriRigidRegex = ("^iglu:" + // Protocol
      "([a-zA-Z0-9-_.]{1,128})/" + // Vendor
      "([a-zA-Z0-9-_]{1,128})/" + // Name
      "([a-zA-Z0-9-_]{1,128})/" + // Format
      "([0-9]*(?:-(?:[0-9]*)){2})$").r // SchemaVer

    private val schemaPathRigidRegex = ("^([a-zA-Z0-9-_.]{1,128})/" +
      "([a-zA-Z0-9-_]{1,128})/" +
      "([a-zA-Z0-9-_]{1,128})/" +
      "([0-9]*(?:-(?:[0-9]*)){2})$").r

    def schemaKeyFromUri(schemaUri: String): Either[ParseError, SchemaKey] = schemaUri match {
      case schemaUriRigidRegex(vnd, n, f, ver) =>
        schemaVerParse(ver) match {
          case Right(full: SchemaVer.Full) => Right(SchemaKey(vnd, n, f, full))
          case _                           => Left(ParseError.InvalidSchemaVer)
        }
      case _ => Left(ParseError.InvalidIgluUri)
    }

    def schemaMapFromPath(schemaPath: String): Either[ParseError, SchemaMap] = schemaPath match {
      case schemaPathRigidRegex(vnd, n, f, ver) =>
        schemaVerParse(ver) match {
          case Right(v: SchemaVer.Full)    => Right(SchemaMap(vnd, n, f, v))
          case Right(_: SchemaVer.Partial) => Left(ParseError.InvalidSchemaVer)
          case Left(other)                 => Left(other)
        }
      case _ => Left(ParseError.InvalidSchema)
    }

    def partialFromUri(schemaUri: String): Either[ParseError, PartialSchemaKey] = schemaUri match {
      case PartialSchemaKey.schemaUriRegex(vnd, n, f, m, r, a) =>
        schemaVerParse(s"$m-$r-$a").map(PartialSchemaKey(vnd, n, f, _))
      case _ => Left(ParseError.InvalidIgluUri)
    }

    def criterionParse(criterion: String): Option[SchemaCriterion] = criterion match {
      case SchemaCriterion
            .criterionRegex(vendor, name, format, StarString(m), StarString(r), StarString(a)) =>
        Some(SchemaCriterion(vendor, name, format, m, r, a))
      case _ => None
    }

    def schemaVerParse(version: String): Either[ParseError, SchemaVer] =
      schemaVerParseFull(version) match {
        case Left(ParseError.InvalidSchemaVer) =>
          version match {
            case SchemaVer.schemaVerPartialRegex(IntString(m), IntString(r), IntString(a)) =>
              Right(SchemaVer.Partial(m, r, a))
            case _ => Left(ParseError.InvalidSchemaVer)
          }
        case other => other
      }

    def schemaVerParseFull(version: String): Either[ParseError, SchemaVer.Full] = version match {
      case SchemaVer.schemaVerFullRegex(m, r, a) =>
        try {
          Right(SchemaVer.Full(m.toInt, r.toInt, a.toInt))
        } catch {
          case _: NumberFormatException =>
            Left(ParseError.InvalidSchemaVer)
        }
      case _ =>
        Left(ParseError.InvalidSchemaVer)
    }

    def schemaVerIsValid(version: String): Boolean =
      version.matches(SchemaVer.schemaVerFullRegex.toString)

    private def parseInt(number: String): Option[Int] =
      try {
        Some(number.toInt)
      } catch {
        case _: NumberFormatException => None
      }

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

    /** As [[IntString]], but for the `*` that a criterion uses for an unknown component. */
    private object StarString {
      def unapply(arg: String): Option[Option[Int]] =
        if (arg == "*") Some(None) else parseInt(arg).map(Some(_))
    }
  }
}
