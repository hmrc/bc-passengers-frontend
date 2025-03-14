/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import scala.math.BigDecimal.RoundingMode

trait FormatsAndConversions {

  implicit class BigDecimalDecimalFormatter(value: BigDecimal) {

    def formatDecimalPlaces(scale: Int): BigDecimal =
      value.setScale(scale, RoundingMode.HALF_UP)

    def stripTrailingZerosToString: String =
      value.bigDecimal.stripTrailingZeros().toPlainString
  }

  implicit class OptionBigDecimalHelper(value: Option[BigDecimal]) {
    def getOrElseZero: BigDecimal =
      value.getOrElse(BigDecimal(0))
  }
}
