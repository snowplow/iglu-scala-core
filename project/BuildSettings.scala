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
import sbt._
import Keys._

// dynver plugin
import sbtdynver.DynVerPlugin.autoImport._

// Mima plugin
import com.typesafe.tools.mima.plugin.MimaKeys._

// Scoverage plugin
import scoverage.ScoverageKeys._

// GHPages plugin
import com.typesafe.sbt.sbtghpages.GhpagesPlugin.autoImport._
import com.typesafe.sbt.site.SitePlugin.autoImport._
import com.typesafe.sbt.SbtGit.GitKeys.{gitBranch, gitRemoteRepo}
import com.typesafe.sbt.site.preprocess.PreprocessPlugin.autoImport._
import sbtunidoc.ScalaUnidocPlugin.autoImport._


object BuildSettings {
  // Basic project settings
  lazy val commonProjectSettings: Seq[sbt.Setting[_]] = Seq(
    organization := "com.snowplowanalytics",
    scalaVersion := "2.13.6",
    crossScalaVersions := Seq("2.12.14", "2.13.6"),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    ThisBuild / dynverVTagPrefix := false // Otherwise git tags required to have v-prefix
  )

  lazy val coreProjectSettings: Seq[sbt.Setting[_]] = Seq(
    name := "iglu-core",
    description := "Core entities for Iglu"
  )

  lazy val circeProjectSettings: Seq[sbt.Setting[_]] = Seq(
    name := "iglu-core-circe",
    description := "Iglu Core type classes instances for Circe"
  )

  lazy val json4sProjectSettings: Seq[sbt.Setting[_]] = Seq(
    name := "iglu-core-json4s",
    description := "Iglu Core type classes instances for Json4s"
  )

  lazy val docsProjectSettings: Seq[sbt.Setting[_]] = Seq(
    name := "docs",
    description := "Scaladoc publishing"
  )

  // Make package (build) metadata available within source code.
  lazy val scalifiedSettings = Seq(
    Compile / sourceGenerators += Def.task {
      val file = (Compile / sourceManaged).value / "settings.scala"
      IO.write(
        file,
        """package com.snowplowanalytics.iglu.core.generated
          |object ProjectSettings {
          |  val organization = "%s"
          |  val name = "%s"
          |  val version = "%s"
          |  val scalaVersion = "%s"
          |  val description = "%s"
          |}
          |"""
          .stripMargin
          .format(
            organization.value,
            name.value,
            version.value,
            scalaVersion.value,
            description.value
          )
      )
      Seq(file)
    }.taskValue
  )

  lazy val resolverSettings: Seq[sbt.Setting[_]] = Seq(
    resolvers ++= Seq(
      "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots/")
    )
  )

  lazy val publishSettings = Seq[Setting[_]](
    publishArtifact := true,
    Test / publishArtifact := false,
    pomIncludeRepository := { _ =>
      false
    },
    homepage := Some(url("http://snowplowanalytics.com")),
    developers := List(
      Developer(
        "Snowplow Analytics Ltd",
        "Snowplow Analytics Ltd",
        "support@snowplowanalytics.com",
        url("https://snowplowanalytics.com")
      )
    )
  )

  // If a new version introduces breaking changes,
  // clear `mimaBinaryIssueFilters` and `mimaPreviousVersions`.
  // Otherwise, add previous version to the set without
  // removing older versions.
  val mimaPreviousVersions = Set("1.0.0")
  val mimaSettings = Seq(
    mimaPreviousArtifacts := mimaPreviousVersions.map { organization.value %% name.value % _ },
    ThisBuild / mimaFailOnNoPrevious := false,
    mimaBinaryIssueFilters ++= Seq(),
    Test / test := {
      mimaReportBinaryIssues.value
      (Test / test).value
    }
  )

  val scoverageSettings = Seq(
    coverageMinimumStmtTotal := 50,
    coverageFailOnMinimum := true,
    coverageHighlighting := false,
    (Test / test) := {
      coverageReport.dependsOn(Test / test).value
    }
  )

  val ghPagesSettings = Seq(
    ghpagesPushSite := ghpagesPushSite.dependsOn(makeSite).value,
    ghpagesNoJekyll := false,
    gitRemoteRepo := "git@github.com:snowplow/iglu-scala-core.git",
    gitBranch := Some("gh-pages"),
    ScalaUnidoc / siteSubdirName := version.value,
    Preprocess / preprocessVars := Map("VERSION" -> version.value),
    addMappingsToSiteDir(ScalaUnidoc / packageDoc / mappings, ScalaUnidoc / siteSubdirName),
    ghpagesCleanSite / excludeFilter := new FileFilter {
      def accept(f: File) = true
    }
  )

  lazy val commonBuildSettings: Seq[sbt.Setting[_]] = commonProjectSettings ++ resolverSettings ++ publishSettings ++ 
    mimaSettings ++ scoverageSettings
  lazy val coreBuildSettings: Seq[sbt.Setting[_]] =
    (coreProjectSettings ++ scalifiedSettings ++ commonBuildSettings).diff(scoverageSettings)
  lazy val circeBuildSettings: Seq[sbt.Setting[_]]  = circeProjectSettings ++ commonBuildSettings
  lazy val json4sBuildSettings: Seq[sbt.Setting[_]] = json4sProjectSettings ++ commonBuildSettings
  lazy val docsBuildSettings: Seq[sbt.Setting[_]] = docsProjectSettings ++ ghPagesSettings ++ commonBuildSettings
}
