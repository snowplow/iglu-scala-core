/*
 * Copyright (c) 2016-2023 Snowplow Analytics Ltd. All rights reserved.
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
import sbt._

object Dependencies {
  object V {
    // Scala third party
    val circe           = "0.14.3"
    val cats            = "2.9.0"

    // Testing
    val specs2 = "4.17.0"
  }

  val circe            = "io.circe"                   %% "circe-core"      % V.circe
  val cats             = "org.typelevel"              %% "cats-core"       % V.cats

  // Testing
  val specs2       = "org.specs2" %% "specs2-core"    % V.specs2 % "test"
  val circeParser  = "io.circe"   %% "circe-parser"   % V.circe  % "test"
  val circeLiteral = "io.circe"   %% "circe-literal"  % V.circe  % "test"
}
