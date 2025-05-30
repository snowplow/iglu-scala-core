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

import IgluCoreCommon.DescribedString

class SchemaKeySpec extends Specification {
  def is = s2"""
  Specification for parsing SchemaKey
    parse simple correct key from Iglu URI $e1
    parse complex correct key from Iglu URI $e2
    fail to parse with preceding zero $e3
    fail to parse with missing REVISION $e4
    fail to parse with invalid name $e5
    fail to parse with missing ADDITION $e8
    fail to parse partial schema key $e9
    fail to parse if key is URL-encoded $e10
    fail to parse schema key when one of the version components is outside the Int range $e11

  Specification for SchemaKey
    sort entities with SchemaKey $e6
    vendorAsSnakeCase convert vendor to snake case $e7
    nameAsSnakeCase convert name to snake case $e12
  """

  def e1 = {
    val uri = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-0"
    SchemaKey.fromUri(uri) must beRight(
      SchemaKey(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        SchemaVer.Full(1, 0, 0)
      )
    )
  }

  def e2 = {
    val uri = "iglu:uk.edu.acme.sub-division/second-event_complex/jsonschema/2-10-32"
    SchemaKey.fromUri(uri) must beRight(
      SchemaKey(
        "uk.edu.acme.sub-division",
        "second-event_complex",
        "jsonschema",
        SchemaVer.Full(2, 10, 32)
      )
    )
  }

  def e10 = {
    val uri = "iglu%3Auk.edu.acme.sub-division%2Fsecond-event_complex%2Fjsonschema%2F2-10-32"
    SchemaKey.fromUri(uri) must beLeft
  }

  def e3 = {
    val uri = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-01-0"
    SchemaKey.fromUri(uri) must beLeft
  }

  def e4 = {
    val uri = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1--0"
    SchemaKey.fromUri(uri) must beLeft
  }

  def e5 = {
    val uri = "iglu:com.snowplowanalytics.snowplow/mobile.context/jsonschema/1-2-0"
    SchemaKey.fromUri(uri) must beLeft
  }

  def e8 = {
    val uri = "iglu:com.snowplowanalytics.snowplow/mobile.context/jsonschema/1-2-"
    SchemaKey.fromUri(uri) must beLeft
  }

  def e9 = {
    val uri = "iglu:com.snowplowanalytics.snowplow/mobile.context/jsonschema/1-2-?"
    SchemaKey.fromUri(uri) must beLeft
  }

  def e6 = {

    implicit val ordering = SchemaKey.ordering

    val describedData = List(
      DescribedString(
        "com.snowplowanalytics.snowplow",
        "aobile_context",
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
      DescribedString(
        "com.snowplowanalytics.snowplow",
        "mobile_context",
        "jsonschema",
        2,
        1,
        0,
        "210"
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

    describedData.sortBy(
      SchemaKey.extract(_).getOrElse(throw new RuntimeException("Not possible"))
    ) must beEqualTo(
      Seq(
        DescribedString(
          "com.snowplowanalytics.snowplow",
          "aobile_context",
          "jsonschema",
          2,
          1,
          0,
          "210"
        ),
        DescribedString("com.snowplowanalytics.snowplow", "mobile_context", "avro", 2, 1, 0, "210"),
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
          1,
          1,
          1,
          "111"
        ),
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
          2,
          1,
          0,
          "210"
        )
      )
    )
  }

  def e11 = {
    val uri = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/111111111111114-0-0"
    SchemaKey.fromUri(uri) must beLeft
  }

  def e7 =
    SchemaKey("com.ac-mE", "test", "jsonschema", SchemaVer.Full(1, 0, 0)).vendorAsSnakeCase must beEqualTo(
      "com_ac_me"
    )

  def e12 =
    SchemaKey("com.acme", "te.s-tNAme", "jsonschema", SchemaVer.Full(1, 0, 0)).nameAsSnakeCase must beEqualTo(
      "te_s_t_name"
    )
}
