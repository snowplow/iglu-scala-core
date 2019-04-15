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
package com.snowplowanalytics.iglu.core.circe

// cats
import cats.syntax.either._

// circe
import io.circe._
import io.circe.literal._

// This library
import com.snowplowanalytics.iglu.core._
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._

import org.specs2.Specification

class DecordersSpec extends Specification { def is = s2"""
  Circe decoders
    decode SelfDescribingSchema $e1
    decode SelfDescribingData $e2
    decode SchemaList $e3
    decode produces valid SchemaList-specific error $e4
  """

  def e1 = {
    val result: Json =
      json"""
        {
        	"self": {
        		"vendor": "com.acme",
        		"name": "keyvalue",
        		"format": "jsonschema",
        		"version": "1-1-0"
        	},
        	"type": "object",
        	"properties": {
        		"name": { "type": "string" },
        		"value": { "type": "string" }
        	}
        }
      """

    val self = SchemaMap("com.acme", "keyvalue", "jsonschema", SchemaVer.Full(1,1,0))
    val schema =
      json"""
        {
        	"type": "object",
        	"properties": {
        		"name": { "type": "string" },
        		"value": { "type": "string" }
         }
        }
      """

    // With AttachTo[JValue] with ToData[JValue] in scope .toSchema won't be even available
    result.as[SelfDescribingSchema[Json]] must beRight(SelfDescribingSchema(self, schema))
  }

  def e2 = {
    val input: Json =
      json"""
        {
        	"schema": "iglu:com.acme/event/jsonschema/1-0-4",
        	"data": {}
        }
      """

    val expected = SelfDescribingData(
      SchemaKey("com.acme", "event", "jsonschema", SchemaVer.Full(1,0,4)),
      Json.fromFields(List.empty)
    )

    input.as[SelfDescribingData[Json]] must beRight(expected)
  }

  def e3 = {
    val input: Json =
      json"""
        ["iglu:com.acme/example/jsonschema/1-0-0", "iglu:com.acme/example/jsonschema/1-0-1"]
      """

    val expected = SchemaList(List(
      SchemaKey("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)),
      SchemaKey("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1))))

    input.as[SchemaList] must beRight(expected)
  }

  def e4 = {
    import cats.syntax.show._

    val input: Json =
      json"""
        ["iglu:com.acme/example/jsonschema/1-0-0", "iglu:com.nonacme/example/jsonschema/1-0-1"]
      """

    val expected = "DecodingFailure at : Cannot parse list of strings into SchemaList. " +
      "SchemaKey iglu:com.nonacme/example/jsonschema/1-0-1 does not match previous vendor (com.nonacme) or name (example)"

    input.as[SchemaList].leftMap(_.show) must beLeft(expected)
  }
}
