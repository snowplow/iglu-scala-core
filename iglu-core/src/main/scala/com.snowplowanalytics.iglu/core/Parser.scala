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

/**
  * Hand-written parsers for the string forms of the core Iglu entities.
  *
  * Every entity documents a canonical regular expression, and those regexes remain the
  * specification of the format. The parsers here accept exactly the same strings, but do
  * so in a single pass over the input, without allocating the `Matcher` and the `List` of
  * groups that a Scala regex match needs. An Iglu URI is parsed once per self-describing
  * entity per event, so the extra code earns its place.
  *
  * The range of an `Int` is the one thing no regex can express, so that check sits on top
  * of the format rather than inside it. `2147483648-0-0` matches both
  * [[SchemaKey.schemaUriRegex]] and [[SchemaVer.schemaVerFullRegex]], yet
  * [[SchemaKey.fromUri]] answers [[ParseError.InvalidSchemaVer]] and
  * [[SchemaVer.parseFull]] fails, while [[SchemaVer.isValid]], which asks only about the
  * format, says `true`.
  *
  * Only the successful path is optimised. Invalid input still reaches the same
  * `ParseError` as it always did, but the code takes its time getting there.
  */
private[core] object Parser {

  /** The protocol that every Iglu schema URI starts with. */
  final val IgluProtocol = "iglu:"

  /** The maximum length of the vendor, name and format fields. */
  final val MaxFieldLength = 128

  /** The character standing for an unknown component in a [[PartialSchemaKey]] URI. */
  final val PartialWildcard = '?'

  /** The character standing for an unknown component in a [[SchemaCriterion]]. */
  final val CriterionWildcard = '*'

  /**
    * Passed as the wildcard where no component may be a wildcard. Wildcards are compared
    * as character codes and no `Char` is negative, so this cannot collide with anything
    * the input could contain.
    */
  final private val NoWildcard = -1

  /**
    * One component of a version: a MODEL, a REVISION or an ADDITION. Either a value, or
    * one of the three reasons it is not one.
    *
    * Represented as an `Int` so that parsing a [[Version]] allocates nothing: a value
    * holds itself, and each reason is a distinct negative number. `VersionComponent` is
    * a value class, so it is erased to that `Int` too. The encoding is confined to this
    * class and its companion; the rest of the parser asks the predicates instead of
    * comparing.
    */
  final class VersionComponent(val raw: Int) extends AnyVal {

    /** True if this is a value, and therefore within `Int` range. */
    def isValue: Boolean = raw >= 0

    /** True if this was the wildcard character rather than a number. */
    def isWildcard: Boolean = raw == VersionComponent.Wildcard

    /** True if this is a value or a wildcard. */
    def isValueOrWildcard: Boolean = isValue || isWildcard

    /** True if this is a well-formed number, including one too large for an `Int`. */
    def isNumberOrTooBig: Boolean = isValue || raw == VersionComponent.TooBig

    /** True if this is not a well-formed number or a wildcard at all. */
    def isMalformed: Boolean = raw == VersionComponent.Malformed

    /** The value, or `None` if this is a wildcard or out of `Int` range. */
    def toOption: Option[Int] = if (isValue) Some(raw) else None
  }

  object VersionComponent {

    /** A component that was the wildcard character rather than a number. */
    def wildcard: VersionComponent = new VersionComponent(Wildcard)

    /** A component that is a well-formed number, but too large for an `Int`. */
    def tooBig: VersionComponent = new VersionComponent(TooBig)

    /** A component that is not a well-formed number at all. */
    def malformed: VersionComponent = new VersionComponent(Malformed)

    /** A component that is the given value, which must not be negative. */
    def value(i: Int): VersionComponent = new VersionComponent(i)

    // All three are negative, and a value never is, so a reason for not being a value
    // cannot be mistaken for one.
    final private val Wildcard  = -1
    final private val TooBig    = -2
    final private val Malformed = -3
  }

  /**
    * The vendor, name and format of an Iglu URI, path or criterion, together with where
    * the version begins in the string they were read from.
    */
  final class Prefix(
    val vendor: String,
    val name: String,
    val format: String,
    val versionFrom: Int
  )

  /** The three [[VersionComponent]]s of a version. */
  final class Version(
    val model: VersionComponent,
    val revision: VersionComponent,
    val addition: VersionComponent
  ) {

    /** True if all three components are known and within `Int` range. */
    def isFull: Boolean =
      model.isValue && revision.isValue && addition.isValue

    /** True if every component is either known and within `Int` range, or a wildcard. */
    def isKnownOrWildcard: Boolean =
      model.isValueOrWildcard && revision.isValueOrWildcard && addition.isValueOrWildcard

    /**
      * True if every component is a well-formed number, including one too large for an
      * `Int`. This is exactly what [[SchemaVer.schemaVerFullRegex]] matches, which is
      * weaker than [[isFull]]: `2147483648-0-0` has the shape but cannot be parsed.
      */
    def hasFullShape: Boolean =
      model.isNumberOrTooBig && revision.isNumberOrTooBig && addition.isNumberOrTooBig

    /**
      * True if every component is a well-formed number or a wildcard, again including a
      * number too large for an `Int`. This is as much as [[PartialSchemaKey.schemaUriRegex]]
      * checks, so it separates a malformed URI from a well-formed one whose version is
      * out of range.
      */
    def hasWildcardShape: Boolean =
      !model.isMalformed && !revision.isMalformed && !addition.isMalformed

    /** Only meaningful when [[isFull]]. */
    def toFull: SchemaVer.Full =
      SchemaVer.Full(model.raw, revision.raw, addition.raw)

    def toPartial: SchemaVer.Partial =
      SchemaVer.Partial(modelOption, revisionOption, additionOption)

    /** The MODEL, or `None` if it is a wildcard or out of `Int` range. */
    def modelOption: Option[Int] = model.toOption

    /** The REVISION, or `None` if it is a wildcard or out of `Int` range. */
    def revisionOption: Option[Int] = revision.toOption

    /** The ADDITION, or `None` if it is a wildcard or out of `Int` range. */
    def additionOption: Option[Int] = addition.toOption
  }

  /** Returned whenever a version does not even have three dash-separated components. */
  private val MalformedVersion =
    new Version(VersionComponent.malformed, VersionComponent.malformed, VersionComponent.malformed)

  /** Parse the `iglu:vendor/name/format/` portion of an Iglu schema URI or criterion. */
  def uriPrefix(s: String): Option[Prefix] =
    if (s.startsWith(IgluProtocol)) prefix(s, IgluProtocol.length) else None

  /**
    * Parse the `vendor/name/format/` portion of an Iglu URI, path or criterion, starting
    * at `from`.
    *
    * @return `None` unless all three fields are present, use only their permitted
    *         characters, and are between 1 and [[MaxFieldLength]] characters long.
    */
  def prefix(s: String, from: Int): Option[Prefix] = {
    val vendorUntil = s.indexOf('/', from)
    if (!isField(s, from, vendorUntil, dotAllowed = true)) None
    else {
      val nameUntil = s.indexOf('/', vendorUntil + 1)
      if (!isField(s, vendorUntil + 1, nameUntil, dotAllowed = false)) None
      else {
        val formatUntil = s.indexOf('/', nameUntil + 1)
        if (!isField(s, nameUntil + 1, formatUntil, dotAllowed = false)) None
        else
          Some(
            new Prefix(
              s.substring(from, vendorUntil),
              s.substring(vendorUntil + 1, nameUntil),
              s.substring(nameUntil   + 1, formatUntil),
              formatUntil + 1
            )
          )
      }
    }
  }

  /** Parse a `MODEL-REVISION-ADDITION` version in which every component must be known. */
  def fullVersion(s: String, from: Int, until: Int): Version =
    version(s, from, until, NoWildcard)

  /**
    * Parse a `MODEL-REVISION-ADDITION` version, in which any component may instead be the
    * given wildcard.
    *
    * @param wildcard the character code standing for an unknown component, or
    *                 [[NoWildcard]] if no component may be unknown.
    */
  def version(s: String, from: Int, until: Int, wildcard: Int): Version = {
    val firstDash = s.indexOf('-', from)
    if (firstDash < 0 || firstDash >= until) MalformedVersion
    else {
      val secondDash = s.indexOf('-', firstDash + 1)
      if (secondDash < 0 || secondDash >= until) MalformedVersion
      else
        new Version(
          component(s, from, firstDash, wildcard, zeroAllowed = false),
          component(s, firstDash  + 1, secondDash, wildcard, zeroAllowed = true),
          component(s, secondDash + 1, until, wildcard, zeroAllowed      = true)
        )
    }
  }

  /**
    * True if the substring has the shape of a version, ie exactly three dash-separated
    * runs of digits, any of which may be empty.
    *
    * This is what picks [[ParseError.InvalidSchemaVer]] over [[ParseError.InvalidIgluUri]]
    * in [[SchemaKey.fromUri]], and over [[ParseError.InvalidSchema]] in
    * [[SchemaMap.fromPath]]. It is therefore looser than the version part of
    * [[SchemaKey.schemaUriRegex]] and [[SchemaMap.schemaPathRegex]], which reject a leading
    * zero and an empty component: tightening it to match them would report `0-0-0` as a
    * malformed URI or path rather than as a bad version.
    */
  def isVersionShaped(s: String, from: Int, until: Int): Boolean = {
    var i      = from
    var dashes = 0
    var valid  = true
    while (i < until && valid) {
      val c = s.charAt(i)
      if (c == '-') dashes += 1
      else valid = c >= '0' && c <= '9'
      i += 1
    }
    valid && dashes == 2
  }

  /** Parse a single MODEL, REVISION or ADDITION from `s` between `from` and `until`. */
  private def component(
    s: String,
    from: Int,
    until: Int,
    wildcard: Int,
    zeroAllowed: Boolean
  ): VersionComponent = {
    val length = until - from
    if (length == 1 && s.charAt(from).toInt == wildcard) VersionComponent.wildcard
    else if (length < 1) VersionComponent.malformed
    else {
      val first = s.charAt(from)
      if (first == '0')
        // Zeroes cannot be prepended, so a leading zero only ever forms the component "0"
        if (zeroAllowed && length == 1) VersionComponent.value(0) else VersionComponent.malformed
      else if (first < '1' || first > '9') VersionComponent.malformed
      else {
        var value      = first - '0'
        var i          = from + 1
        var overflowed = false
        var malformed  = false
        // Keep scanning after an overflow: `tooBig` claims the component is a well-formed
        // number, which the regexes only agree with if every remaining character is a
        // digit. Stopping early would report "2147483648x" as merely out of range, so
        // hasWildcardShape would call it well formed and PartialSchemaKey.fromUri would
        // answer InvalidSchemaVer where the regex says InvalidIgluUri. Malformed is terminal.
        while (i < until && !malformed) {
          val digit = s.charAt(i) - '0'
          if (digit < 0 || digit > 9) malformed = true
          else if (overflowed || value > (Int.MaxValue - digit) / 10) overflowed = true
          else value = value * 10 + digit
          i += 1
        }
        if (malformed) VersionComponent.malformed
        else if (overflowed) VersionComponent.tooBig
        else VersionComponent.value(value)
      }
    }
  }

  /** True if `s` between `from` and `until` is a valid vendor, name or format field. */
  private def isField(s: String, from: Int, until: Int, dotAllowed: Boolean): Boolean = {
    val length = until - from
    if (length < 1 || length > MaxFieldLength) false
    else {
      var i     = from
      var valid = true
      while (i < until && valid) {
        val c = s.charAt(i)
        valid = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
          c == '-' || c == '_' || (dotAllowed && c == '.')
        i += 1
      }
      valid
    }
  }
}
