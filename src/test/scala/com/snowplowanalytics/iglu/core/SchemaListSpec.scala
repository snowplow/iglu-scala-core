/*
 * Copyright (c) 2016-2019 Snowplow Analytics Ltd. All rights reserved.
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

class SchemaListSpec extends Specification { def is = s2"""
  parseString parses list of valid strings $e1
  parseString parses single valid 1-0-0 SchemaKey $e2
  parseString rejects list of strings starting not from 1-0-0 $e3
  parseString rejects a non-unique SchemaKey $e4
  parseString rejects a SchemaKey with non-matching vendor or name $e5
  """

  def e1 = {
    val input = List(
      "iglu:com.acme/example/jsonschema/1-0-0",
      "iglu:com.acme/example/jsonschema/1-1-0")
    val expected = List(
      SchemaKey("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)),
      SchemaKey("com.acme", "example", "jsonschema", SchemaVer.Full(1,1,0)))

    SchemaList.parseStrings(input) must beRight(SchemaList(expected))
  }

  def e2 = {
    val input = List("iglu:com.acme/example/jsonschema/1-0-0")
    val expected = List(SchemaKey("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)))

    SchemaList.parseStrings(input) must beRight(SchemaList(expected))
  }

  def e3 = {
    val input = List(
      "iglu:com.acme/example/jsonschema/1-0-1",
      "iglu:com.acme/example/jsonschema/1-0-0")
    val expectedError = "Cannot parse list of strings into SchemaList. " +
      "Init schema iglu:com.acme/example/jsonschema/1-0-1 is not 1-0-0"

    SchemaList.parseStrings(input) must beLeft(expectedError)
  }

  def e4 = {
    val input = List(
      "iglu:com.acme/example/jsonschema/1-0-0",
      "iglu:com.acme/example/jsonschema/1-0-1",
      "iglu:com.acme/example/jsonschema/1-0-1")
    val expectedError = "Cannot parse list of strings into SchemaList. " +
      "SchemaKey iglu:com.acme/example/jsonschema/1-0-1 is not unique"

    SchemaList.parseStrings(input) must beLeft(expectedError)
  }

  def e5 = {
    val input = List(
      "iglu:com.acme/example/jsonschema/1-0-0",
      "iglu:com.acme/example/jsonschema/1-0-1",
      "iglu:com.foo/example/jsonschema/1-0-1")
    val expectedError = "Cannot parse list of strings into SchemaList. " +
      "SchemaKey iglu:com.foo/example/jsonschema/1-0-1 does not match previous vendor (com.foo) or name (example)"

    SchemaList.parseStrings(input) must beLeft(expectedError)
  }
}
