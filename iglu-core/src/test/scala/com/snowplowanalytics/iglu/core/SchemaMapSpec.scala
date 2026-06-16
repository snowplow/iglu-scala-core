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

class SchemaMapSpec extends Specification {
  def is = s2"""
  Specification for parsing SchemaKey
    parse simple correct schema map from Iglu path $e1

  Specification for 128-character field length limit
    fail to parse with vendor exceeding 128 characters $e2
    fail to parse with name exceeding 128 characters $e3
    fail to parse with format exceeding 128 characters $e4
    parse successfully when vendor, name, and format are exactly 128 characters $e5

  Specification for version component overflow behaviour
    a model exceeding Int.MaxValue fails to parse $e6
  """

  def e1 = {
    val path = "uk.edu.acme.sub-division/second-event_complex/jsonschema/2-10-32"
    SchemaMap.fromPath(path) must beRight(
      SchemaMap(
        "uk.edu.acme.sub-division",
        "second-event_complex",
        "jsonschema",
        SchemaVer.Full(2, 10, 32)
      )
    )
  }

  def e2 = {
    val longVendor = "a" * 129
    SchemaMap.fromPath(s"$longVendor/event/jsonschema/1-0-0") must beLeft
  }

  def e3 = {
    val longName = "a" * 129
    SchemaMap.fromPath(s"com.acme/$longName/jsonschema/1-0-0") must beLeft
  }

  def e4 = {
    val longFormat = "a" * 129
    SchemaMap.fromPath(s"com.acme/event/$longFormat/1-0-0") must beLeft
  }

  def e5 = {
    val maxField = "a" * 128
    SchemaMap.fromPath(s"$maxField/$maxField/$maxField/1-0-0") must beRight
  }

  def e6 =
    SchemaMap.fromPath("com.acme/event/jsonschema/2147483648-0-0") must beLeft
}
