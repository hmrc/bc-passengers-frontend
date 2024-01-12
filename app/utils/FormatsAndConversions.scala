/*
 * Copyright 2024 HM Revenue & Customs
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

trait ToBigDecimal[A] {
  def toBigDecimal(value: A): BigDecimal
}

trait FormatsAndConversions {

  implicit val stringToBigDecimal: ToBigDecimal[String] =
    new ToBigDecimal[String] {
      override def toBigDecimal(value: String): BigDecimal = BigDecimal(value)
    }

  implicit val intToBigDecimal: ToBigDecimal[Int] =
    new ToBigDecimal[Int] {
      override def toBigDecimal(value: Int): BigDecimal = BigDecimal(value)
    }

  implicit class CovertToBigDecimal[A](value: A)(implicit tc: ToBigDecimal[A]) {
    def toBigDecimal: BigDecimal =
      tc.toBigDecimal(value)
  }

  implicit class BigDecimalHelper(value: BigDecimal) {
    def format2dps: BigDecimal =
      value.setScale(2, RoundingMode.HALF_UP)

    def format3dps: BigDecimal =
      value.setScale(3, RoundingMode.HALF_UP)

    def format5dps: BigDecimal =
      value.setScale(5, RoundingMode.HALF_UP)
  }

  implicit class OptionBigDecimalHelper(value: Option[BigDecimal]) {
    def getOrElseZero: BigDecimal =
      value.getOrElse(BigDecimal(0))
  }
}
