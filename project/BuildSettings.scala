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
import sbt._
import Keys._

import bintray.BintrayPlugin._
import bintray.BintrayKeys._

import com.typesafe.tools.mima.plugin.MimaKeys._

import scoverage.ScoverageKeys._

import com.typesafe.sbt.sbtghpages.GhpagesPlugin.autoImport._
import com.typesafe.sbt.site.SitePlugin.autoImport._
import com.typesafe.sbt.SbtGit.GitKeys.{gitBranch, gitRemoteRepo}
import com.typesafe.sbt.site.preprocess.PreprocessPlugin.autoImport._
import sbtunidoc.ScalaUnidocPlugin.autoImport._

object BuildSettings {
  // Basic project settings
  lazy val commonProjectSettings: Seq[sbt.Setting[_]] = Seq(
    organization := "com.snowplowanalytics",
    version := "1.0.0-M2",
    scalaVersion := "2.13.1",
    crossScalaVersions := Seq("2.12.11", "2.13.1")
  )

  lazy val coreProjectSettings: Seq[sbt.Setting[_]] = commonProjectSettings ++ Seq(
    name := "iglu-core",
    description := "Core entities for Iglu"
  )

  lazy val circeProjectSettings: Seq[sbt.Setting[_]] = commonProjectSettings ++ Seq(
    name := "iglu-core-circe",
    description := "Iglu Core type classes instances for Circe"
  )

  lazy val json4sProjectSettings: Seq[sbt.Setting[_]] = commonProjectSettings ++ Seq(
    name := "iglu-core-json4s",
    description := "Iglu Core type classes instances for Json4s"
  )

  lazy val docsProjectSettings: Seq[sbt.Setting[_]] = commonProjectSettings ++ Seq(
    name := "docs",
    description := "Scaladoc publishing"
  )

  // Make package (build) metadata available within source code.
  lazy val scalifiedSettings = Seq(
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "settings.scala"
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

  lazy val allScalacFlags = Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xfuture",                          // Turn on future language features.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
    "-Ywarn-unused:params",              // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",            // Warn if a private member is unused.
    "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
  )

  lazy val compilerSettings: Seq[sbt.Setting[_]] = Seq(
    scalacOptions :=
      scalaVersion.map { version: String =>
        val scala213Flags = Seq(
          "-Xlint:by-name-right-associative", // not available
          "-Xlint:unsound-match", // not available
          "-Yno-adapted-args", // not available. Can be returned in future https://github.com/scala/bug/issues/11110
          "-Ypartial-unification", // enabled by default
          "-Ywarn-inaccessible", // not available. the same as -Xlint:inaccessible
          "-Ywarn-infer-any", // not available. The same as -Xlint:infer-any
          "-Ywarn-nullary-override", // not available. The same as -Xlint:nullary-override
          "-Ywarn-nullary-unit", // not available. The same as -Xlint:nullary-unit
          "-Xfuture",   // not available
          "-Ywarn-unused:imports"         // cats.syntax.either._
        )
        if (version.startsWith("2.13.")) allScalacFlags.diff(scala213Flags) else allScalacFlags
      }.value,
    javacOptions := Seq(
      "-source",
      "1.8",
      "-target",
      "1.8",
      "-Xlint"
    )
  )

  lazy val resolverSettings: Seq[sbt.Setting[_]] = Seq(
    resolvers ++= Seq(
      "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots/")
    )
  )

  lazy val publishSettings = bintraySettings ++ Seq[Setting[_]](
    publishMavenStyle := true,
    publishArtifact := true,
    publishArtifact in Test := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    bintrayOrganization := Some("snowplow"),
    bintrayRepository := "snowplow-maven",
    pomIncludeRepository := { _ =>
      false
    },
    homepage := Some(url("http://snowplowanalytics.com")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/snowplow-incubator/iglu-scala-core"),
        "scm:git@github.com:snowplow-incubator/iglu-scala-core.git"
      )
    ),
    pomExtra := (<developers>
      <developer>
        <name>Snowplow Analytics Ltd</name>
        <email>support@snowplowanalytics.com</email>
        <organization>Snowplow Analytics Ltd</organization>
        <organizationUrl>http://snowplowanalytics.com</organizationUrl>
      </developer>
    </developers>)
  )

  // If a new version introduces breaking changes,
  // clear `mimaBinaryIssueFilters` and `mimaPreviousVersions`.
  // Otherwise, add previous version to the set without
  // removing older versions.
  val mimaPreviousVersions = Set()
  val mimaSettings = Seq(
    mimaPreviousArtifacts := mimaPreviousVersions.map { organization.value %% name.value % _ },
    mimaBinaryIssueFilters ++= Seq(),
    test in Test := {
      mimaReportBinaryIssues.value
      (test in Test).value
    }
  )

  val scoverageSettings = Seq(
    coverageMinimum := 50,
    coverageFailOnMinimum := true,
    coverageHighlighting := false,
    (test in Test) := {
      coverageReport.dependsOn(test in Test).value
    }
  )

  val ghPagesSettings = Seq(
    ghpagesPushSite := ghpagesPushSite.dependsOn(makeSite).value,
    ghpagesNoJekyll := false,
    gitRemoteRepo := "git@github.com:snowplow-incubator/iglu-scala-core.git",
    gitBranch := Some("gh-pages"),
    siteSubdirName in ScalaUnidoc := version.value,
    preprocessVars in Preprocess := Map("VERSION" -> version.value),
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    excludeFilter in ghpagesCleanSite := new FileFilter {
      def accept(f: File) = true
    }
  )

  lazy val commonBuildSettings: Seq[sbt.Setting[_]] = compilerSettings ++ resolverSettings ++
    publishSettings ++ mimaSettings ++ scoverageSettings
  lazy val coreBuildSettings: Seq[sbt.Setting[_]] =
    (coreProjectSettings ++ scalifiedSettings ++ commonBuildSettings).diff(scoverageSettings)
  lazy val circeBuildSettings: Seq[sbt.Setting[_]]  = circeProjectSettings ++ commonBuildSettings
  lazy val json4sBuildSettings: Seq[sbt.Setting[_]] = json4sProjectSettings ++ commonBuildSettings
  lazy val docsBuildSettings: Seq[sbt.Setting[_]] = docsProjectSettings ++ compilerSettings ++
    ghPagesSettings
}
