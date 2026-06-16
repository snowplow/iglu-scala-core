/*
 * Copyright (c) 2012-2023 Snowplow Analytics Ltd.. All rights reserved.
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

import org.specs2.Specification

import IgluCoreCommon._

class SchemaCriterionSpec extends Specification {
  def is = s2"""
  Specification for parsing SchemaCriterion
    parse simple correct criterion $e1
    parse criterion without SchemaVer $e2
    parse criterion 2-0-* $e9
    parse criterion 1-0-0 $e10
    fail to parse criterion with missing ADDITION $e3
    fail to parse criterion with 0 as MODEL $e8

  Specification for matching SchemaKeys with SchemaCriterions
    match by MODEL (2-*-*) $e4
    match initial Schema (*-0-0) $e5
    do not match not matching key (*-0-0) $e6
    filter correct entities (*-0-0) $e7

  Specification for 128-character field length limit
    fail to parse with vendor exceeding 128 characters $e11
    fail to parse with name exceeding 128 characters $e12
    fail to parse with format exceeding 128 characters $e13
    parse successfully when vendor, name, and format are exactly 128 characters $e14

  Specification for version component overflow behaviour
    a model exceeding Int.MaxValue is treated as a wildcard $e15
    """

  def e1 = {
    val criterion = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/2-*-*"
    SchemaCriterion.parse(criterion) must beSome(
      SchemaCriterion("com.snowplowanalytics.snowplow", "mobile_context", "jsonschema", 2)
    )
  }

  def e2 = {
    val criterion = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/*-*-*"
    SchemaCriterion.parse(criterion) must beSome(
      SchemaCriterion(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        None,
        None,
        None
      )
    )
  }

  def e9 = {
    val criterion = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/2-0-*"
    SchemaCriterion.parse(criterion) must beSome(
      SchemaCriterion(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        Some(2),
        Some(0),
        None
      )
    )
  }

  def e10 = {
    val criterion = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-0"
    SchemaCriterion.parse(criterion) must beSome(
      SchemaCriterion(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        Some(1),
        Some(0),
        Some(0)
      )
    )
  }

  def e3 = {
    val criterion = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/*-*-"
    SchemaCriterion.parse(criterion) must beNone
  }

  def e8 = {
    val criterion = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/0-*-*"
    SchemaCriterion.parse(criterion) must beNone
  }

  def e11 = {
    val longVendor = "a" * 129
    SchemaCriterion.parse(s"iglu:$longVendor/mobile_context/jsonschema/2-*-*") must beNone
  }

  def e12 = {
    val longName = "a" * 129
    SchemaCriterion.parse(s"iglu:com.snowplowanalytics.snowplow/$longName/jsonschema/2-*-*") must beNone
  }

  def e13 = {
    val longFormat = "a" * 129
    SchemaCriterion.parse(s"iglu:com.snowplowanalytics.snowplow/mobile_context/$longFormat/2-*-*") must beNone
  }

  def e14 = {
    val maxField = "a" * 128
    SchemaCriterion.parse(s"iglu:$maxField/$maxField/$maxField/2-*-*") must beSome
  }

  def e15 =
    // parseInt catches NumberFormatException and returns None, so an overflowing model
    // is silently treated as a wildcard rather than causing the parse to fail entirely
    SchemaCriterion.parse(
      "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/2147483648-0-0"
    ) must beSome(
      SchemaCriterion(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        None,
        Some(0),
        Some(0)
      )
    )

  def e4 = {
    val criterion =
      SchemaCriterion("com.snowplowanalytics.snowplow", "mobile_context", "jsonschema", 2)
    val key = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "mobile_context",
      "jsonschema",
      SchemaVer.Full(2, 1, 0)
    )
    criterion.matches(key) must beTrue
  }

  def e5 = {
    val criterion = SchemaCriterion(
      "com.snowplowanalytics.snowplow",
      "mobile_context",
      "jsonschema",
      None,
      Some(0),
      Some(0)
    )
    val key = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "mobile_context",
      "jsonschema",
      SchemaVer.Full(2, 0, 0)
    )
    criterion.matches(key) must beTrue
  }

  def e6 = {
    val criterion = SchemaCriterion(
      "com.snowplowanalytics.snowplow",
      "mobile_context",
      "jsonschema",
      None,
      Some(0),
      Some(0)
    )
    val key = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "mobile_context",
      "jsonschema",
      SchemaVer.Full(2, 1, 0)
    )
    criterion.matches(key) must beFalse
  }

  def e7 = {
    val criterion = SchemaCriterion(
      "com.snowplowanalytics.snowplow",
      "mobile_context",
      "jsonschema",
      None,
      Some(0),
      Some(0)
    )
    val keys = List(
      DescribedString(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        2,
        1,
        0,
        "210"
      ),
      DescribedString(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        1,
        0,
        0,
        "100"
      ),
      DescribedString("com.snowplowanalytics.snowplow", "mobile_context", "avro", 2, 1, 0, "210"),
      DescribedString(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        2,
        0,
        0,
        "200"
      ),
      DescribedString(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        1,
        1,
        1,
        "111"
      )
    )
    val matched = criterion.pickFrom(keys)
    matched must containAllOf(
      Seq(
        DescribedString(
          "com.snowplowanalytics.snowplow",
          "mobile_context",
          "jsonschema",
          1,
          0,
          0,
          "100"
        ),
        DescribedString(
          "com.snowplowanalytics.snowplow",
          "mobile_context",
          "jsonschema",
          2,
          0,
          0,
          "200"
        )
      )
    )
  }
}
