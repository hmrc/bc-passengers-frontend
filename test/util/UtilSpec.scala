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

package util

import models.ProductPath
import play.api.data.validation._

import java.math.RoundingMode

class UtilSpec extends BaseSpec {

  "Validating a cost" should {

    "succeed when passed 11,000.00" in {

      blankOkCostCheckConstraint("cost").apply("11,000.00")
    }

    "succeed when passed 11,000.00 to old constraint" in {

      bigDecimalCostCheckConstraint("cost").apply("11,000.00")
    }

    "restrict negative value like -95 to old constraint" in {

      bigDecimalCostCheckConstraint("cost").apply("-95.00").equals(Valid) should be(false)
    }

    "restrict negative value like -9.50" in {

      blankOkCostCheckConstraint("cost").apply("-9.50").equals(Valid) should be(false)
    }

    "return failed validation when a value greater than 9999999999 is passed" in {
      blankOkCostCheckConstraint(
        productPathMessageKey = "other-goods.adult.adult-clothing"
      )("99999999999.00") shouldBe Invalid(
        Seq(ValidationError("error.exceeded.max.cost.other-goods.adult.adult-clothing"))
      )
    }

    "return failed validation when a positive value with more than 2 decimal places is passed" in {
      blankOkCostCheckConstraint(
        productPathMessageKey = "other-goods.adult.adult-clothing"
      )("95.999") shouldBe Invalid(Seq(ValidationError("error.invalid.format.cost.other-goods.adult.adult-clothing")))
    }

    "return successful validation when an empty string is passed" in {
      blankOkCostCheckConstraint(productPathMessageKey = "other-goods.adult.adult-clothing")("") shouldBe Valid
    }

    "return correctly formatted string value" in {
      formatMonetaryValue(0.012) shouldBe "0.01"
    }
  }

  "validating limits" should {
    "return the correct product path" in {
      calculatorLimitConstraintBigDecimal(
        limits = Map(
          "L-WINE"   -> 2.2222,
          "L-WINESP" -> 4.4444
        ),
        applicableLimits = List("L-WINE", "L-WINESP"),
        path = ProductPath(path = "alcohol/sparkling-wine")
      ) shouldBe Some(ProductPath(path = "alcohol/sparkling-wine"))
    }
  }

//  "Formatting with decimalFormat5" when {
//    "using the HALF_UP rounding mode" should {
//      "preserve the correct value in 5 decimal places" in {
//        val (start, end, step): (BigDecimal, BigDecimal, BigDecimal) = (0.00, 0.99, 0.01)
//
//        start
//          .to(end, step)
//          .foreach(value =>
//            BigDecimal(decimalFormat5.format(value.toDouble / 1000)) shouldBe BigDecimal(
//              decimalFormat5.format(value / 1000)
//            )
//          )
//      }
//    }
//
//    "using the UP rounding mode" should {
//      "not preserve the correct value in 5 decimal places" in {
//        val values: Seq[BigDecimal] = Seq(0.07, 0.13, 0.14, 0.26, 0.28, 0.52, 0.56, 0.77, 0.81, 0.89)
//
//        decimalFormat5.setRoundingMode(RoundingMode.UP)
//
//        values.foreach(value =>
//          BigDecimal(decimalFormat5.format(value.toDouble / 1000)) should not be BigDecimal(
//            decimalFormat5.format(value / 1000)
//          )
//        )
//      }
//    }
//  }
}
