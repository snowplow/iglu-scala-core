# Iglu Scala Core

[![Build Status][ci-image]][ci]
[![Maven Central][maven-badge]][maven-link]
[![License][license-image]][license]
[![Coverage Status][coveralls-image]][coveralls]

Core entities for working with Iglu in Scala.

Recent documentation can be found on dedicated wiki page: **[Iglu Scala Core][techdocs]**.

## Quickstart

Assuming git, JVM and [SBT][sbt-site] are installed:

```bash
$ git clone https://github.com/snowplow/iglu-scala-core
$ cd iglu-scala-core
$ sbt compile
```

In order to include Iglu Scala Core into your project, add following to your `build.sbt`:

```scala
libraryDependencies += "com.snowplowanalytics" % "iglu-core" % "1.0.1"
```

## Find out more

| **[Technical Docs][techdocs]**     | **[Setup Guide][setup]**     | **[Roadmap][roadmap]**           | **[Contributing][contributing]**           |
|-------------------------------------|-------------------------------|-----------------------------------|---------------------------------------------|
| [![i1][techdocs-image]][techdocs] | [![i2][setup-image]][setup] | [![i3][roadmap-image]][roadmap] | [![i4][contributing-image]][contributing] |

## Copyright and license

Iglu Scala Core is copyright 2016-2021 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[maven-badge]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/iglu-core_2.13/badge.svg
[maven-link]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/iglu-core_2.13

[ci]: https://github.com/snowplow/iglu-scala-core/actions?query=workflow%3ACI
[ci-image]: https://github.com/snowplow/iglu-scala-core/workflows/CI/badge.svg

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[coveralls]: https://coveralls.io/github/snowplow/iglu-scala-core?branch=master
[coveralls-image]: https://coveralls.io/repos/github/snowplow/iglu-scala-core/badge.svg?branch=master

[sbt-site]: https://www.scala-sbt.org/

[techdocs]: https://docs.snowplowanalytics.com/docs/pipeline-components-and-applications/iglu/iglu-clients/
[setup]: https://docs.snowplowanalytics.com/docs/pipeline-components-and-applications/iglu/iglu-clients/scala-client-setup/
[roadmap]: https://github.com/snowplow/snowplow/projects/7
[contributing]: https://docs.snowplowanalytics.com/docs/contributing/

[techdocs]: https://docs.snowplowanalytics.com/docs/pipeline-components-and-applications/iglu/common-architecture/iglu-core/
[setup]: https://docs.snowplowanalytics.com/docs/pipeline-components-and-applications/iglu/common-architecture/iglu-core/
[roadmap]: https://github.com/snowplow/snowplow/projects/7
[contributing]: https://docs.snowplowanalytics.com/docs/contributing/

[techdocs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/techdocs.png
[setup-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/setup.png
[roadmap-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/roadmap.png
[contributing-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/contributing.png
