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

import org.json4s._
import org.json4s.jackson.JsonMethods.parse

import typeclasses._

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
    import IgluCoreCommon.Json4SAttachSchemaKeyData

    val result: JValue = parse(
      """
        |{
        | "schema": "iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0",
        | "data": {
        |  "latitude": 32.2,
        |  "longitude": 53.23,
        |  "speed": 40
        | }
        |}
      """.stripMargin
    )

    val key = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "geolocation_context",
      "jsonschema",
      SchemaVer.Full(1, 1, 0)
    )
    val data = parse("""
                       |{
                       |  "latitude": 32.2,
                       |  "longitude": 53.23,
                       |  "speed": 40
                       |}
      """.stripMargin)

    // With AttachTo[JValue] with ToSchema[JValue] in scope .toData won't be even available
    SelfDescribingData.parse(result) must beRight(SelfDescribingData(key, data))
  }

  def e2 = {
    import IgluCoreCommon.Json4SAttachSchemaMapComplex

    val result: JValue = parse(
      s"""
         |{
         | "$$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
         |	"self": {
         |		"vendor": "com.acme",
         |		"name": "keyvalue",
         |		"format": "jsonschema",
         |		"version": "1-1-0"
         |	},
         |	"type": "object",
         |	"properties": {
         |		"name": { "type": "string" },
         |		"value": { "type": "string" }
         |	}
         |}
      """.stripMargin
    )

    val self   = SchemaMap("com.acme", "keyvalue", "jsonschema", SchemaVer.Full(1, 1, 0))
    val schema = parse("""
                         |{
                         |	"type": "object",
                         |	"properties": {
                         |		"name": { "type": "string" },
                         |		"value": { "type": "string" }
                         | }
                         |}
      """.stripMargin)

    SelfDescribingSchema.parse(result) must beRight(SelfDescribingSchema(self, schema))
  }

  def e3 = {
    import IgluCoreCommon.Json4SNormalizeData

    val schema = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "geolocation_context",
      "jsonschema",
      SchemaVer.Full(1, 1, 0)
    )
    val data = parse("""
                       |{
                       |  "latitude": 32.2,
                       |  "longitude": 53.23,
                       |  "speed": 40
                       |}
      """.stripMargin)

    val expected: JValue = parse(
      """
        |{
        | "schema": "iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0",
        | "data": {
        |  "latitude": 32.2,
        |  "longitude": 53.23,
        |  "speed": 40
        | }
        |}
      """.stripMargin
    )

    val result = SelfDescribingData(schema, data).normalize
    result must beEqualTo(expected)
  }

  def e4 = {
    import IgluCoreCommon.Json4SNormalizeSchema

    val self   = SchemaMap("com.acme", "keyvalue", "jsonschema", SchemaVer.Full(1, 1, 0))
    val schema = parse("""
                         |{
                         |	"type": "object",
                         |	"properties": {
                         |		"name": { "type": "string" },
                         |		"value": { "type": "string" }
                         | }
                         |}
      """.stripMargin)

    val expected: JValue = parse(
      s"""
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
    )

    val result = SelfDescribingSchema(self, schema)
    result.normalize must beEqualTo(expected)
  }

  def e5 = {
    implicit val stringify: StringifyData[JValue] = IgluCoreCommon.StringifyData

    val schema = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "geolocation_context",
      "jsonschema",
      SchemaVer.Full(1, 1, 0)
    )
    val data: JValue = parse("""
                               |{
                               |  "latitude": 32.2,
                               |  "longitude": 53.23,
                               |  "speed": 40
                               |}
      """.stripMargin)

    val expected: String =
      """{"schema":"iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0","data":{"latitude":32.2,"longitude":53.23,"speed":40}}"""

    val result = SelfDescribingData(schema, data).asString
    result must beEqualTo(expected)

  }

  def e6 = {
    implicit val stringify: StringifySchema[JValue] = IgluCoreCommon.StringifySchema

    val self           = SchemaMap("com.acme", "keyvalue", "jsonschema", SchemaVer.Full(1, 1, 0))
    val schema: JValue = parse("""
                                 |{
                                 |	"type": "object",
                                 |	"properties": {
                                 |		"name": { "type": "string" },
                                 |		"value": { "type": "string" }
                                 | }
                                 |}
      """.stripMargin)

    val expected: String =
      s"""{"self":{"vendor":"com.acme","name":"keyvalue","format":"jsonschema","version":"1-1-0"},"$$schema":"http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#","type":"object","properties":{"name":{"type":"string"},"value":{"type":"string"}}}"""

    val result = SelfDescribingSchema(self, schema)
    result.asString must beEqualTo(expected)
  }

  def e7 = {
    import IgluCoreCommon.Json4SAttachSchemaMapComplex

    val result: JValue = parse(
      s"""
         |{
         | "$$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self/schema/jsonschema/1-0-0#",
         |	"self": {
         |		"vendor": "com.acme",
         |		"name": "keyvalue",
         |		"format": "jsonschema",
         |		"version": "1-1-0"
         |	},
         |	"type": "object",
         |	"properties": {
         |		"name": { "type": "string" },
         |		"value": { "type": "string" }
         |	}
         |}
      """.stripMargin
    )

    SelfDescribingSchema.parse(result) must beLeft(ParseError.InvalidMetaschema: ParseError)
  }

  def e8 = {
    import IgluCoreCommon.Json4SAttachSchemaMapComplex

    val result: JValue = parse("""
                                 |{
                                 |	"self": {
                                 |		"vendor": "com.acme",
                                 |		"name": "keyvalue",
                                 |		"format": "jsonschema",
                                 |		"version": "1-1-0"
                                 |	},
                                 |	"type": "object",
                                 |	"properties": {
                                 |		"name": { "type": "string" },
                                 |		"value": { "type": "string" }
                                 |	}
                                 |}
      """.stripMargin)

    SelfDescribingSchema.parse(result) must beLeft(ParseError.InvalidMetaschema: ParseError)
  }
}
