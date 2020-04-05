# Iglu Scala Core

[![Maven Central][maven-badge]][maven-link]
[![Build Status][travis-image]][travis] 
[![License][license-image]][license]
[![Scaladoc][scaladoc-image]][scaldoc]

Core entities for working with Iglu in Scala.

Recent documentation can be found on dedicated wiki page: **[Iglu Scala Core][techdocs]**.

## Quickstart

Assuming git, JVM and [SBT][sbt-site] are installed:

```bash
$ git clone https://github.com/snowplow-incubator/iglu-scala-core
$ cd iglu-scala-core
$ sbt compile
```

In order to include Iglu Scala Core into your project, add following to your `build.sbt`:

```scala
libraryDependencies += "com.snowplowanalytics" % "iglu-core" % "0.5.1"
```

## Find out more

| **[Technical Docs][techdocs]**     | **[Setup Guide][setup]**     | **[Roadmap][roadmap]**           | **[Contributing][contributing]**           |
|-------------------------------------|-------------------------------|-----------------------------------|---------------------------------------------|
| [![i1][techdocs-image]][techdocs] | [![i2][setup-image]][setup] | [![i3][roadmap-image]][roadmap] | [![i4][contributing-image]][contributing] |

## Copyright and license

Iglu Scala Core is copyright 2016-2020 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[maven-badge]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/iglu-core_2.12/badge.svg
[maven-link]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/iglu-core_2.12

[travis]: https://travis-ci.org/snowplow-incubator/iglu-scala-core
[travis-image]: https://travis-ci.org/snowplow-incubator/iglu-scala-core.png?branch=master

[sbt-site]: https://www.scala-sbt.org/

[techdocs]: https://github.com/snowplow/iglu/wiki/Scala-iglu-core
[roadmap]: https://github.com/snowplow/iglu/wiki/Product-roadmap
[setup]: https://github.com/snowplow/iglu/wiki/Scala-iglu-core#setup
[contributing]: https://github.com/snowplow/iglu/wiki/Contributing

[techdocs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/techdocs.png
[setup-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/setup.png
[roadmap-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/roadmap.png
[contributing-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/contributing.png

[license]: http://www.apache.org/licenses/LICENSE-2.0

[scaladoc]: https://snowplow-incubator.github.io/iglu-scala-core/1.0.0/com/snowplowanalytics/iglu/core/index.html
[scaladoc-image]: https://javadoc-badge.appspot.com/com.github.nscala-time/nscala-time_2.11.svg?label=scaladoc
