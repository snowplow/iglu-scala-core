/*
 * Copyright (c) 2016-2024 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.iglu
package core

object StringUtils {

  /**
    * Transforms CamelCase string into snake_case as following
    * All consecutive uppercases are joined by underscore
    * All lowercase/digit followed by uppercase are joined by underscore
    * All hyphens and dots are replaced with underscore
    * Converts all chars to lowercase
    * @param str the input string, likely in CamelCase format
    * @return the input string in snake_case format
    */
  def toSnakeCase(str: String): String =
    str
      .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
      .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
      .replaceAll("-", "_")
      .replaceAll("""\.""", "_")
      .toLowerCase
}
