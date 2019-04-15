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

/**
  * List of SchemaKeys, belonging to the same vendor/name group, and started with 1-0-0
  * Proven to be non-empty, but correct order is trusted and can be validated by Schema DDL
  * It is usually acceptable to trust the producer as long as producer is Iglu Server
  */
final case class SchemaList private(schemas: List[SchemaKey]) extends AnyVal {
  def vendor: String = schemas.head.vendor
  def name: String = schemas.head.name
}

object SchemaList {
  private val EmptyList = Left("SchemaList cannot be empty")
  private case class ParseAccumulator(vendor: String, name: String, parsed: List[SchemaKey])

  /** Validate that list of strings is SchemaList (non-empty SchemaKeys only list) */
  def parseStrings(strings: List[String]): Either[String, SchemaList] = {
    val results = strings.foldLeft(EmptyList: Either[String, ParseAccumulator]) {
      case (EmptyList, cur) => SchemaKey.fromUri(cur) match {
        case Right(key) if key.version != SchemaVer.Full(1, 0, 0) =>
          Left(s"Init schema ${key.toSchemaUri} is not 1-0-0")
        case Right(key) => Right(ParseAccumulator(key.vendor, key.name, List(key)))
        case Left(error) => Left(s"$cur - ${error.code}")
      }
      case (Right(acc), cur) => SchemaKey.fromUri(cur) match {
        case Right(key) if key.vendor != acc.vendor || key.name != acc.name =>
          Left(s"SchemaKey ${key.toSchemaUri} does not match previous vendor (${key.vendor}) or name (${key.name})")
        case Right(key) if acc.parsed.contains(key) =>
          Left(s"SchemaKey ${key.toSchemaUri} is not unique")
        case Right(key) =>
          Right(acc.copy(parsed = key :: acc.parsed))
        case Left(error) =>
          Left(s"$cur - ${error.code}")
      }
      case (Left(error), _) => Left(error)
    }

    results match {
      case Right(ParseAccumulator(_, _, parsed)) => Right(SchemaList(parsed.reverse))
      case Left(error) => Left(s"Cannot parse list of strings into SchemaList. $error")
    }
  }

  def parseUnsafe(keys: List[SchemaKey]) = SchemaList(keys)
}
