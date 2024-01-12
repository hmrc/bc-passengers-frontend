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

  ".cigarAndCigarilloConstraint" when {

    "the productToken is cigars" when {

      "numberOfSticks is under limit of 200" should {

        "return true" in {
          util.cigarAndCigarilloConstraint(199, "cigars") shouldBe true
        }
      }

      "numberOfSticks is == limit of 200" should {

        "return true" in {
          util.cigarAndCigarilloConstraint(200, "cigars") shouldBe true
        }
      }

      "numberOfSticks is over limit of 200" should {

        "return false" in {
          util.cigarAndCigarilloConstraint(201, "cigars") shouldBe false
        }
      }
    }

    "the productToken is cigarillos" when {

      "numberOfSticks is under limit of 400" should {

        "return true" in {
          util.cigarAndCigarilloConstraint(199, "cigarillos") shouldBe true
        }
      }

      "numberOfSticks == limit of 400" should {

        "return true" in {
          util.cigarAndCigarilloConstraint(400, "cigarillos") shouldBe true
        }
      }

      "numberOfSticks over limit of 400" should {

        "return false" in {
          util.cigarAndCigarilloConstraint(401, "cigarillos") shouldBe false
        }
      }
    }

    "the productToken is something weird" when {

      "numberOfSticks is under a limit" should {

        "return false" in {
          util.cigarAndCigarilloConstraint(199, "something_smelly") shouldBe false
        }
      }
    }
  }
}
