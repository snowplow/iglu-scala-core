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

import io.circe.Json
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
    import IgluCoreCommon.CirceAttachSchemaKeyData

    val result: Json = parse(
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
    import IgluCoreCommon.CirceAttachSchemaMapComplex

    val result: Json = parse(
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
    import IgluCoreCommon.CirceNormalizeData

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

    val expected: Json = parse(
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
    import IgluCoreCommon.CirceNormalizeSchema

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

    val expected: Json = parse(
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
    implicit val stringify: StringifyData[Json] = IgluCoreCommon.StringifyData

    val schema = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "geolocation_context",
      "jsonschema",
      SchemaVer.Full(1, 1, 0)
    )
    val data: Json = parse("""
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
    implicit val stringify: StringifySchema[Json] = IgluCoreCommon.StringifySchema

    val self           = SchemaMap("com.acme", "keyvalue", "jsonschema", SchemaVer.Full(1, 1, 0))
    val schema: Json = parse("""
                                 |{
                                 |	"type": "object",
                                 |	"properties": {
                                 |		"name": { "type": "string" },
                                 |		"value": { "type": "string" }
                                 | }
                                 |}
      """.stripMargin)

    val expected: String =
      s"""{"$$schema":"http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#","self":{"vendor":"com.acme","name":"keyvalue","format":"jsonschema","version":"1-1-0"},"type":"object","properties":{"name":{"type":"string"},"value":{"type":"string"}}}"""

    val result = SelfDescribingSchema(self, schema)
    result.asString must beEqualTo(expected)
  }

  def e7 = {
    import IgluCoreCommon.CirceAttachSchemaMapComplex

    val result: Json = parse(
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
    import IgluCoreCommon.CirceAttachSchemaMapComplex

    val result: Json = parse("""
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

  private def parse(input: String): Json = io.circe.parser.parse(input).fold(throw _, identity)
}
