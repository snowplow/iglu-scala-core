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
package com.snowplowanalytics.iglu.core

/** Common error type for parsing core Iglu entities */
sealed trait ParseError extends Product with Serializable {
  def code: String
  def message(str: String): String
}

object ParseError {

  /**
   * The schema version appears to be invalid.
   *
   * A valid schema version consists of MODEL, REVISION and ADDITION,
   * separated by dashes, eg 1-0-0.
   *
   * Partial versions are allowed, which only specify the MODEL
   * and / or REVISION, eg 1-?-? or 1-0-?.
   *
   * Zeroes cannot be prepended to any component, so this is not allowed: 01-01-01.
   *
   * A full schema version must match the following regex:
   * {{{"^([1-9][0-9]*)-(0|[1-9][0-9]*)-(0|[1-9][0-9]*)$"}}}.
   *
   * A partial schema version must match the following regex:
   * {{{"^([1-9][0-9]*|\\?)-((?:0|[1-9][0-9]*)|\\?)-((?:0|[1-9][0-9]*)|\\?)$"}}}.
   */
  case object InvalidSchemaVer extends ParseError {
    def code = "INVALID_SCHEMAVER"
    def message(str: String) = s"Invalid schema version: $str, code: $code"
  }

  /**
   * The Iglu URI appears to be invalid.
   *
   * A valid Iglu schema URI looks something like this:
   * "iglu:com.vendor/schema_name/jsonschema/1-0-0".
   *
   * It must match the following regex:
   * {{{"^iglu:([a-zA-Z0-9-_.]+)/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)/([0-9]*(?:-(?:[0-9]*)){2})$"}}}.
   *
   * This regex allows for invalid schema versions. If the schema version is
   * invalid, it will be reported in a separate [[InvalidSchemaVer]] error.
   */
  case object InvalidIgluUri extends ParseError {
    def code = "INVALID_IGLUURI"
    def message(str: String) = s"Invalid Iglu URI: $str, code: $code"
  }

  /**
   * The data payload's structure appears to be invalid.
   *
   * A valid structure is one that has the following fields:
   * - 'schema': a string containing a valid Iglu schema URI;
   * - 'data': a JSON blob containing the actual data.
   *
   * It's likely one or both of these fields is missing or
   * malformed.
   */
  case object InvalidData extends ParseError {
    def code = "INVALID_DATA_PAYLOAD"
    def message(str: String) = s"Invalid data payload: $str, code: $code"
  }

  /**
   * The schema appears to not be a valid self-describing schema.
   *
   * A valid self-describing schema must contain a 'self' property
   * with information that describes the schema, eg:
   * {{{
   *   "self": {
   *      "vendor": "com.vendor",
   *      "name": "schema_name",
   *      "format": "jsonschema",
   *      "version": "1-0-0"
   *    }
   * }}}
   *
   * It's likely this property is missing or malformed.
   */
  case object InvalidSchema extends ParseError {
    def code = "INVALID_SCHEMA"
    def message(str: String) = s"Invalid schema: $str, code: $code"
  }

  /**
   * The metaschema URI appears to be invalid.
   *
   * A valid Iglu schema must make use of the "$schema" keyword
   * to declare that it conforms to the 'com.snowplowanalytics.self-desc/schema'
   * metaschema.
   *
   * The valid format is:
   * {{{"$schema" : "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#"}}}.
   *
   * It's likely this declaration is missing or malformed.
   */
  case object InvalidMetaschema extends ParseError {
    def code = "INVALID_METASCHEMA"
    def message(str: String) = s"Invalid metaschema: $str, code: $code"
  }

  def parse(string: String): Option[ParseError] =
    List(InvalidSchemaVer, InvalidIgluUri, InvalidData, InvalidSchema, InvalidMetaschema).find { _.code == string }

  /** List parse function to get an entity that failed parsing */
  def liftParse[A, B](parser: A => Either[ParseError, B]): A => Either[(ParseError, A), B] =
    a => parser(a).left.map(e => (e, a))
}
