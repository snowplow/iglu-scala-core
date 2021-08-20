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
package com.snowplowanalytics.iglu.core.circe

import io.circe._
import io.circe.literal._

import com.snowplowanalytics.iglu.core._
import com.snowplowanalytics.iglu.core.circe.implicits._

import org.specs2.Specification

class ContainersSpec extends Specification {
  def is = s2"""
  Specification for container types
    extract SelfDescribingData $e1
    extract SelfDescribingSchema $e2
    normalize SelfDescribingData $e3
    normalize SelfDescribingSchema $e4
    stringify SelfDescribingData $e5
    stringify SelfDescribingSchema $e6
    fail to extract SelfDescribingSchema if metaschema field contains invalid value $e7
    fail to extract SelfDescribingSchema if metaschema field is missing $e8
  """

  def e1 = {

    val result: Json =
      json"""
        {
         "schema": "iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0",
         "data": {
          "latitude": 32.2,
          "longitude": 53.23,
          "speed": 40
         }
        }
      """

    val key = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "geolocation_context",
      "jsonschema",
      SchemaVer.Full(1, 1, 0)
    )
    val data =
      json"""
        {
          "latitude": 32.2,
          "longitude": 53.23,
          "speed": 40
        }
      """

    SelfDescribingData.parse(result) must beRight(SelfDescribingData(key, data))
  }

  def e2 = {

    val result: Json =
      json"""
        {
          "$$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
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

    val self = SchemaMap("com.acme", "keyvalue", "jsonschema", SchemaVer.Full(1, 1, 0))
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
    SelfDescribingSchema.parse(result) must beRight(SelfDescribingSchema(self, schema))
  }

  def e3 = {

    val schema = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "geolocation_context",
      "jsonschema",
      SchemaVer.Full(1, 1, 0)
    )
    val data: Json =
      json"""
        {
          "latitude": 32.2,
          "longitude": 53.23,
          "speed": 40
        }
      """

    val expected: Json =
      json"""
        {
         "schema": "iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0",
         "data": {
          "latitude": 32.2,
          "longitude": 53.23,
          "speed": 40
         }
        }
      """

    val result = SelfDescribingData(schema, data).normalize
    result must beEqualTo(expected)
  }

  def e4 = {

    val self = SchemaMap("com.acme", "keyvalue", "jsonschema", SchemaVer.Full(1, 1, 0))
    val schema: Json =
      json"""
        {
        	"type": "object",
        	"properties": {
        		"name": { "type": "string" },
        		"value": { "type": "string" }
         }
        }
      """

    val expected: Json =
      json"""
        {
          "$$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
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

    val result = SelfDescribingSchema(self, schema)
    result.normalize must beEqualTo(expected)
  }

  def e5 = {

    val schema = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "geolocation_context",
      "jsonschema",
      SchemaVer.Full(1, 1, 0)
    )
    val data: Json =
      json"""
        {
          "latitude": 32.2,
          "longitude": 53.23,
          "speed": 40
        }
      """

    val expected: String =
      """{"schema":"iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0","data":{"latitude":32.2,"longitude":53.23,"speed":40}}"""

    val result = SelfDescribingData(schema, data).asString
    result must beEqualTo(expected)
  }

  def e6 = {

    val self = SchemaMap("com.acme", "keyvalue", "jsonschema", SchemaVer.Full(1, 1, 0))
    val schema: Json =
      json"""
        {
        	"type": "object",
        	"properties": {
        		"name": { "type": "string" },
        		"value": { "type": "string" }
         }
        }
      """

    val expected: String =
      s"""{"type":"object","properties":{"name":{"type":"string"},"value":{"type":"string"}},"self":{"vendor":"com.acme","name":"keyvalue","format":"jsonschema","version":"1-1-0"},"$$schema":"http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#"}"""

    val result = SelfDescribingSchema(self, schema)
    result.asString must beEqualTo(expected)
  }

  def e7 = {

    val result: Json =
      // The valid vendor is 'com.snowplowanalytics.self-desc'
      json"""
        {
          "$$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self/schema/jsonschema/1-0-0#",
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

    SelfDescribingSchema.parse(result) must beLeft(ParseError.InvalidMetaschema: ParseError)
  }

  def e8 = {

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

    SelfDescribingSchema.parse(result) must beLeft(ParseError.InvalidMetaschema: ParseError)
  }
}
