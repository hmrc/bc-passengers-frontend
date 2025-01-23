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

package util

import models.{JourneyData, ProductPath, PurchasedProductInstance}
import play.api.data.validation.*

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

      bigDecimalCostCheckConstraint("cost").apply("-95.00").equals(Valid) shouldBe false
    }

    "restrict negative value like -9.50" in {

      blankOkCostCheckConstraint("cost").apply("-9.50").equals(Valid) shouldBe false
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
    "return true when there are no errors" in {
      calculatorLimitConstraint(
        limits = Map(
          "L-WINE"   -> 2.2222,
          "L-WINESP" -> 4.4444
        ),
        applicableLimits = List.empty
      ) shouldBe true
    }

    "return false when there are errors" in {
      calculatorLimitConstraint(
        limits = Map(
          "L-WINE"   -> 2.2222,
          "L-WINESP" -> 4.4444
        ),
        applicableLimits = List("L-WINE", "L-WINESP")
      ) shouldBe false
    }
  }

  "Formatting with decimalFormat5" when {
    "using the HALF_UP rounding mode" should {
      "preserve the correct value in 5 decimal places" in {
        val (start, end, step): (BigDecimal, BigDecimal, BigDecimal) =
          (BigDecimal(0.00), BigDecimal(0.99), BigDecimal(0.01))

        start
          .to(end, step)
          .foreach(value =>
            BigDecimal(decimalFormat5.format(value.toDouble / 1000)) shouldBe BigDecimal(
              decimalFormat5.format(value / 1000)
            )
          )
      }
    }

    "using the UP rounding mode" should {
      "not preserve the correct value in 5 decimal places" in {
        val values: Seq[BigDecimal] = Seq(0.07, 0.13, 0.14, 0.26, 0.28, 0.52, 0.56, 0.77, 0.81, 0.89)

        decimalFormat5.setRoundingMode(RoundingMode.UP)

        values.foreach(value =>
          BigDecimal(decimalFormat5.format(value.toDouble / 1000)) should not be BigDecimal(
            decimalFormat5.format(value / 1000)
          )
        )
      }
    }
  }

  "validating loose tobacco weight" should {
    "return true" when {
      "supplied weight is less than 1000 grams" in {
        looseTobaccoWeightConstraint(BigDecimal(1)) shouldBe true
      }

      "supplied weight is equal to 1000 grams" in {
        looseTobaccoWeightConstraint(BigDecimal(1000.00)) shouldBe true
      }
    }

    "return false when supplied weight is greater than 1000 grams" in {
      looseTobaccoWeightConstraint(2000.00) shouldBe false
    }
  }

  "validating alcohol volume" should {
    val purchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
      path = ProductPath("alcohol/wine"),
      iid = "iid0",
      weightOrVolume = Some(20.00),
      currency = Some("GBP"),
      cost = Some(100.00)
    )

    def journeyData(purchasedProductInstances: List[PurchasedProductInstance]): JourneyData = JourneyData(
      prevDeclaration = Some(false),
      euCountryCheck = Some("greatBritain"),
      arrivingNICheck = Some(true),
      bringingOverAllowance = Some(true),
      isUKResident = Some(false),
      privateCraft = Some(false),
      ageOver17 = Some(true),
      purchasedProductInstances = purchasedProductInstances
    )

    "return true" when {
      Seq(
        ("beer", 109),
        ("spirits", 9),
        ("wine", 89),
        ("sparkling-wine", 89),
        ("other", 19),
        ("non-sparkling-cider", 19),
        ("sparkling-cider", 19),
        ("sparkling-cider-up", 19)
      ).foreach { case (productToken, volume) =>
        s"supplied volume is less than the limit for $productToken" in {
          alcoholVolumeConstraint(journeyData(List(purchasedProductInstance)), volume, productToken) shouldBe true
        }
      }

      Seq(
        ("beer", 110),
        ("spirits", 10),
        ("wine", 90),
        ("sparkling-wine", 90),
        ("other", 20),
        ("non-sparkling-cider", 20),
        ("sparkling-cider", 20),
        ("sparkling-cider-up", 20)
      ).foreach { case (productToken, volume) =>
        s"supplied volume is equal to the limit for $productToken" in {
          alcoholVolumeConstraint(journeyData(List(purchasedProductInstance)), volume, productToken) shouldBe true
        }
      }
    }

    Seq(
      ("beer", 110.001),
      ("spirits", 10.001),
      ("wine", 90.001),
      ("sparkling-wine", 60.001),
      ("other", 20.001),
      ("non-sparkling-cider", 20.001),
      ("sparkling-cider", 20.001),
      ("sparkling-cider-up", 20.001)
    ).foreach { case (productToken, volume) =>
      s"return false when supplied volume is greater than the limit for $productToken" in {
        alcoholVolumeConstraint(journeyData(Nil), volume, productToken) shouldBe false
      }
    }
  }
}
