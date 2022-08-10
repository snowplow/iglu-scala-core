/*
 * Copyright (c) 2012-2022 Snowplow Analytics Ltd. All rights reserved.
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

import BuildSettings._

ThisBuild / shellPrompt := { state =>
  Project.extract(state).get(sbt.Keys.name) + " > "
}

lazy val root = (project in file("."))
  .aggregate(igluCore, igluCoreCirce, igluCoreJson4s)
  .settings(commonBuildSettings)
  .settings(
    Seq(
      publish / skip := true
    )
  )

lazy val igluCore = (project in file("iglu-core"))
  .enablePlugins(MimaPlugin)
  .settings(coreBuildSettings)
  .settings(
    libraryDependencies ++= Seq(
      // Testing
      Dependencies.specs2,
      Dependencies.json4sTest
    )
  )

lazy val igluCoreCirce = (project in file("iglu-core-circe"))
  .dependsOn(igluCore % "test->test;compile->compile")
  .enablePlugins(MimaPlugin)
  .settings(circeBuildSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.circe,
      Dependencies.cats,
      // Testing
      Dependencies.specs2,
      Dependencies.circeParser,
      Dependencies.circeLiteral
    )
  )

lazy val igluCoreJson4s = (project in file("iglu-core-json4s"))
  .dependsOn(igluCore % "test->test;compile->compile")
  .enablePlugins(MimaPlugin)
  .settings(json4sBuildSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.json4s,
      Dependencies.jacksonDatabind,
      // Testing
      Dependencies.specs2
    )
  )

lazy val docs = (project in file("docs"))
  .enablePlugins(SiteScaladocPlugin, GhpagesPlugin, ScalaUnidocPlugin, PreprocessPlugin)
  .settings(docsBuildSettings)
  .aggregate(igluCore, igluCoreCirce, igluCoreJson4s)
