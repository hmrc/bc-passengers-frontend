/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.validation
import play.api.data.validation.{Invalid, Valid, ValidationError}

class UtilSpec extends BaseSpec {

  "Validating a cost" should {

    "succeed when passed 11,000.00" in {

      blankOkCostCheckConstraint("cost").apply("11,000.00")
    }

    "succeed when passed 11,000.00 to old constraint" in {

      bigDecimalCostCheckConstraint("cost").apply("11,000.00")
    }

    "restrict negative value like -95 to old constraint" in {

      bigDecimalCostCheckConstraint("cost").apply("-95.00").equals(validation.Valid) should be(false)
    }

    "restrict negative value like -9.50" in {

      blankOkCostCheckConstraint("cost")("-9.50").equals(validation.Valid) should be(false)
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
  }
}
