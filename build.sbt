/*
 * Copyright (c) 2012-2019 Snowplow Analytics Ltd. All rights reserved.
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

import Dependencies._
import BuildSettings._
import Json4sBuildSettings._
import CirceBuildSettings._

shellPrompt in ThisBuild := { state => Project.extract(state).get(sbt.Keys.name) + " > " }

// Define our project, with basic project information and library dependencies
lazy val igluCore = (project in file("."))
  .settings(buildSettings: _*)
  .settings(BuildSettings.mimaSettings)
  .settings(
    name := "iglu-core",
    libraryDependencies ++= Seq(
       // Scala (test only)
       Libraries.specs2,
       Libraries.json4sTest
    )
  )


lazy val igluCoreJson4s = (project in file("iglu-core-json4s"))
  .dependsOn(igluCore)
  .settings(json4sBuildSettings: _*)
  .settings(BuildSettings.mimaSettings)
  .settings(
    name := "iglu-core-json4s",
    libraryDependencies ++= Seq(
       Libraries.json4s,
       // Scala (test only)
       Libraries.specs2
    )
  )

lazy val igluCoreCirce = (project in file("iglu-core-circe"))
  .dependsOn(igluCore)
  .settings(circeBuildSettings: _*)
  .settings(BuildSettings.mimaSettings)
  .settings(
    name := "iglu-core-circe",
    libraryDependencies ++= Seq(
      Libraries.circe,
      Libraries.cats,
      // Scala (test only)
      Libraries.specs2,
      Libraries.circeParser,
      Libraries.circeLiteral
    )
  )
