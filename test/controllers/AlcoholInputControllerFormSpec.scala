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

package controllers

import forms.AlcoholInputForm
import models.{AlcoholDto, ProductPath}
import play.api.data.Form
import util.BaseSpec

class AlcoholInputFormFormSpec extends BaseSpec {

  val alcoholItems: Seq[String]      = List("beer", "sparkling-wine", "spirits", "wine", "other", "cider")
  val invalidCostInputs: Seq[String] = List("   ", "----", "***", "$5", "£399.70", "-100")

  "AlcoholInputForm" when {
    alcoholItems.foreach { item =>
      val path: ProductPath = ProductPath(path = s"alcohol/$item")

      s".alcoholForm($path)" should {

        "fail on empty string in weightOrVolume" in {
          val form: Form[AlcoholDto] = injected[AlcoholInputForm]
            .alcoholForm(path)
            .bind(
              Map(
                "weightOrVolume" -> "",
                "country"        -> "FR",
                "currency"       -> "EUR",
                "cost"           -> "50"
              )
            )

          getFormErrors(form) shouldBe buildExpectedFormErrors(
            "weightOrVolume" -> s"error.required.volume.alcohol.$item"
          )
        }

        "fail on special character in weightOrVolume" in {
          val form: Form[AlcoholDto] = injected[AlcoholInputForm]
            .alcoholForm(path)
            .bind(
              Map(
                "weightOrVolume" -> "***",
                "country"        -> "FR",
                "currency"       -> "EUR",
                "cost"           -> "50"
              )
            )

          getFormErrors(form) shouldBe buildExpectedFormErrors("weightOrVolume" -> "error.invalid.characters.volume")
        }

        "fail on empty in weightOrVolume" in {
          val form: Form[AlcoholDto] = injected[AlcoholInputForm]
            .alcoholForm(path)
            .bind(
              Map(
                "weightOrVolume" -> "   ",
                "country"        -> "FR",
                "currency"       -> "EUR",
                "cost"           -> "50"
              )
            )

          getFormErrors(form) shouldBe buildExpectedFormErrors("weightOrVolume" -> "error.invalid.characters.volume")
        }

        "fail on more than 3 decimal places in weightOrVolume" in {
          val form: Form[AlcoholDto] = injected[AlcoholInputForm]
            .alcoholForm(path)
            .bind(
              Map(
                "weightOrVolume" -> "4.5678",
                "country"        -> "FR",
                "currency"       -> "EUR",
                "cost"           -> "50"
              )
            )

          getFormErrors(form) shouldBe buildExpectedFormErrors("weightOrVolume" -> "error.max.decimal.places.volume")
        }

        "pass on cost with comma separated thousands" in {
          val form: Form[AlcoholDto] = injected[AlcoholInputForm]
            .alcoholForm(path)
            .bind(
              Map(
                "weightOrVolume" -> "90",
                "country"        -> "FR",
                "currency"       -> "EUR",
                "cost"           -> "4,444.00"
              )
            )
          form.hasErrors shouldBe false
          form.value.get shouldBe AlcoholDto(90.00, "FR", None, "EUR", 4444.00, None, None, None, None)

        }

        "pass on more than allowance and sending empty limits so shouldn't validate maximum limits" in {
          val form: Form[AlcoholDto] = injected[AlcoholInputForm]
            .alcoholForm(path)
            .bind(
              Map(
                "weightOrVolume" -> "300",
                "country"        -> "IN",
                "currency"       -> "INR",
                "cost"           -> "5000.00"
              )
            )
          form.hasErrors shouldBe false
          form.value.get shouldBe AlcoholDto(300.00, "IN", None, "INR", 5000.00, None, None, None, None)

        }

        invalidCostInputs.foreach { costInput =>
          s"fail on special characters in cost=$costInput" in {
            val form: Form[AlcoholDto] = injected[AlcoholInputForm]
              .alcoholForm(path)
              .bind(
                Map(
                  "weightOrVolume" -> "300",
                  "country"        -> "FR",
                  "currency"       -> "EUR",
                  "cost"           -> costInput
                )
              )

            getFormErrors(form) shouldBe buildExpectedFormErrors("cost" -> "error.invalid.characters")
          }
        }
      }
    }

    ".resilientForm" should {
      val weightOrVolume: BigDecimal = 50
      val alcoholDto: AlcoholDto     = AlcoholDto(
        weightOrVolume = weightOrVolume,
        country = "",
        originCountry = None,
        currency = "",
        cost = 0,
        isVatPaid = None,
        isExcisePaid = None,
        isCustomPaid = None,
        hasEvidence = None
      )
      val form: Form[AlcoholDto]     = injected[AlcoholInputForm].resilientForm
        .bind(
          Map(
            "weightOrVolume" -> "50"
          )
        )

      "pass returning no errors with valid data" in {
        form.hasErrors shouldBe false
        form.value.get shouldBe alcoholDto
      }

      "return the correct result when filled" in {
        form.fill(alcoholDto).value.get shouldBe form.value.get
      }
    }
  }
}
