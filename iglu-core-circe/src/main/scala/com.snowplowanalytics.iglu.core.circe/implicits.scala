/*
 * Copyright (c) 2016-2021 Snowplow Analytics Ltd. All rights reserved.
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
package circe

import cats.{Eq, Show}
import cats.syntax.either._
import io.circe._

import com.snowplowanalytics.iglu.core.typeclasses._

trait implicits {
  implicit final val igluAttachToDataCirce: ExtractSchemaKey[Json] with ToData[Json] =
    new ExtractSchemaKey[Json] with ToData[Json] {

      def extractSchemaKey(entity: Json): Either[ParseError, SchemaKey] =
        for {
          jsonSchema <- entity.asObject.toRight(ParseError.InvalidData)
          schemaUri <- jsonSchema
            .toMap
            .get("schema")
            .flatMap(_.asString)
            .toRight(ParseError.InvalidData)
          schemaKey <- SchemaKey.fromUri(schemaUri)
        } yield schemaKey

      def getContent(json: Json): Either[ParseError, Json] =
        json.asObject.flatMap(_.apply("data")).toRight(ParseError.InvalidData)
    }

  implicit final val igluAttachToSchema: ToSchema[Json] with ExtractSchemaMap[Json] =
    new ToSchema[Json] with ExtractSchemaMap[Json] {

      def extractSchemaMap(entity: Json): Either[ParseError, SchemaMap] =
        CirceIgluCodecs.parseSchemaMap(entity.hcursor).leftMap(_._2)

      override def checkSchemaUri(entity: Json): Either[ParseError, Unit] =
        CirceIgluCodecs.checkSchemaUri(entity.hcursor).leftMap(_._2)

      def getContent(schema: Json): Json =
        Json.fromJsonObject {
          JsonObject.fromMap {
            schema
              .asObject
              .map(_.toMap.filter { case (key, _) => !(key == "self" || key == s"$$schema") })
              .getOrElse(Map.empty)
          }
        }
    }

  // Container-specific instances
  implicit final val igluNormalizeDataJson: NormalizeData[Json] =
    container => CirceIgluCodecs.selfDescribingDataCirceEncoder(container)

  implicit final val igluNormalizeSchemaJson: NormalizeSchema[Json] =
    container => CirceIgluCodecs.selfDescribingSchemaCirceEncoder(container)

  implicit final val igluStringifyDataJson: StringifyData[Json] =
    container => container.normalize(igluNormalizeDataJson).noSpaces

  implicit final val igluStringifySchemaJson: StringifySchema[Json] =
    container => container.normalize(igluNormalizeSchemaJson).noSpaces

  // Cats instances
  implicit final val schemaVerShow: Show[SchemaVer] =
    Show.show(_.asString)

  implicit final val schemaKeyShow: Show[SchemaKey] =
    Show.show(_.toSchemaUri)

  implicit final val partialSchemaKeyShow: Show[PartialSchemaKey] =
    Show.show(_.toSchemaUri)

  implicit final val schemaVerEq: Eq[SchemaVer.Full] =
    Eq.fromUniversalEquals[SchemaVer.Full]

  // Decide if we want to provide Eq partial
  implicit final val schemaKeyEq: Eq[SchemaKey] =
    Eq.fromUniversalEquals[SchemaKey]
}

object implicits extends implicits with CirceIgluCodecs
