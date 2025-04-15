/*
 * Copyright (c) 2012-2024 Snowplow Analytics Ltd.. All rights reserved.
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

import org.specs2.Specification

class StringUtilsSpec extends Specification {
  def is = s2"""
  Specification for StringUtils
    toSnakeCase should transform CamelCase to camel_case $e1
    toSnakeCase should transform CAmelCase to c_amel_case $e2
    toSnakeCase should transform CAmel1Case to c_amel1_case $e3
    toSnakeCase should transform CA-mel1Ca.se to c_a_mel1_ca_se $e4
  """

  def e1 =
    StringUtils.toSnakeCase("CamelCase") must beEqualTo("camel_case")

  def e2 =
    StringUtils.toSnakeCase("CAmelCase") must beEqualTo("c_amel_case")

  def e3 =
    StringUtils.toSnakeCase("CAmel1Case") must beEqualTo("c_amel1_case")

  def e4 =
    StringUtils.toSnakeCase("CA-mel1Ca.se") must beEqualTo("ca_mel1_ca_se")
}
