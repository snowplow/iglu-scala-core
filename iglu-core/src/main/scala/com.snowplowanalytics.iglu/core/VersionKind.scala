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

/** The kind of a component of a [[SchemaVer]]. */
sealed trait VersionKind {
  def show: String = this match {
    case VersionKind.Model    => "MODEL"
    case VersionKind.Revision => "REVISION"
    case VersionKind.Addition => "ADDITION"
  }
}

object VersionKind {
  case object Model extends VersionKind
  case object Revision extends VersionKind
  case object Addition extends VersionKind

  def parse(s: String): Option[VersionKind] = s match {
    case "MODEL"    => Some(Model)
    case "REVISION" => Some(Revision)
    case "ADDITION" => Some(Addition)
    case _          => None
  }
}
