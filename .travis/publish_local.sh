#!/bin/bash

set -e

cd "${TRAVIS_BUILD_DIR}"
sbt +publishLocal
sbt "project igluCoreCirce" +publishLocal --warn
sbt "project igluCoreJson4s" +publishLocal --warn
