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

package controllers

import models.{ProductPath, TobaccoDto}
import play.api.data.Form
import util.BaseSpec

class TobaccoInputControllerFormSpec extends BaseSpec {

  "TobaccoInputController" when {
    ".noOfSticksForm" should {
      val path: ProductPath = ProductPath(path = "tobacco/cigarettes")

      val path2: ProductPath = ProductPath(path = "tobacco/heated-tobacco")

      "fail on empty string in noOfSticks" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks" -> "",
              "country"    -> "FR",
              "currency"   -> "EUR",
              "cost"       -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                     shouldBe 1
        form.error("noOfSticks").get.message shouldBe "error.no_of_sticks.required.tobacco.cigarettes"
      }

      "fail on invalid characters in noOfSticks" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks" -> "***",
              "country"    -> "FR",
              "currency"   -> "EUR",
              "cost"       -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                     shouldBe 1
        form.error("noOfSticks").get.message shouldBe "error.invalid.characters.noofsticks.tobacco.cigarettes"
      }

      "fail on empty string in country" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks" -> "500",
              "country"    -> "",
              "currency"   -> "EUR",
              "cost"       -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                  shouldBe 1
        form.error("country").get.message shouldBe "error.country.invalid"
      }

      "fail on empty string in currency" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path2)
          .bind(
            Map(
              "noOfSticks" -> "500",
              "country"    -> "FR",
              "currency"   -> "",
              "cost"       -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                   shouldBe 1
        form.error("currency").get.message shouldBe "error.currency.invalid"
      }

      "fail on empty string in cost" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path2)
          .bind(
            Map(
              "noOfSticks" -> "500",
              "country"    -> "FR",
              "currency"   -> "EUR",
              "cost"       -> ""
            )
          )
        form.hasErrors shouldBe true
        form.errors.size               shouldBe 1
        form.error("cost").get.message shouldBe "error.required.tobacco.heated-tobacco"
      }

      "fail on special characters in cost" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks" -> "500",
              "country"    -> "FR",
              "currency"   -> "EUR",
              "cost"       -> "***"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size               shouldBe 1
        form.error("cost").get.message shouldBe "error.invalid.characters"
      }

      "fail on more than 2 decimal places in cost" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path2)
          .bind(
            Map(
              "noOfSticks" -> "500",
              "country"    -> "FR",
              "currency"   -> "EUR",
              "cost"       -> "4.567"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size               shouldBe 1
        form.error("cost").get.message shouldBe "error.invalid.format"
      }

      "fail when cost exceeds 9,999,999,999" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks" -> "500",
              "country"    -> "FR",
              "currency"   -> "EUR",
              "cost"       -> "99999999999"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size               shouldBe 1
        form.error("cost").get.message shouldBe "error.exceeded.max"
      }

      "pass on more than allowance and sending empty limits so shouldn't validate maximum limits" in {
        val noOfSticks: Int        = 1000
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .noOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks" -> "1000",
              "country"    -> "IN",
              "currency"   -> "INR",
              "cost"       -> "4500.00"
            )
          )
        form.hasErrors shouldBe false
        form.value.get shouldBe TobaccoDto(Some(noOfSticks), None, "IN", None, "INR", 4500.00, None, None, None, None)
      }
    }

    ".weightOrVolumeForm" should {
      val path: ProductPath = ProductPath(path = "tobacco/rolling-tobacco")

      "fail on empty string in weightOrVolume" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeForm(path)
          .bind(
            Map(
              "weightOrVolume" -> "",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                         shouldBe 1
        form.error("weightOrVolume").get.message shouldBe "error.required.weight.tobacco.rolling-tobacco"
      }

      "fail on special characters in weightOrVolume" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeForm(path)
          .bind(
            Map(
              "weightOrVolume" -> "***",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                         shouldBe 1
        form.error("weightOrVolume").get.message shouldBe "error.invalid.characters.weight"
      }

      "fail on more than 2 decimal places in weightOrVolume" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeForm(path)
          .bind(
            Map(
              "weightOrVolume" -> "4.567",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                         shouldBe 1
        form.error("weightOrVolume").get.message shouldBe "error.max.decimal.places.weight"
      }

      "fail on empty string in country" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeForm(path)
          .bind(
            Map(
              "weightOrVolume" -> "500",
              "country"        -> "",
              "currency"       -> "EUR",
              "cost"           -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                  shouldBe 1
        form.error("country").get.message shouldBe "error.country.invalid"
      }

      "fail on empty string in currency" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeForm(path)
          .bind(
            Map(
              "weightOrVolume" -> "500",
              "country"        -> "FR",
              "currency"       -> "",
              "cost"           -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                   shouldBe 1
        form.error("currency").get.message shouldBe "error.currency.invalid"
      }

      "fail on empty string in cost" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeForm(path)
          .bind(
            Map(
              "weightOrVolume" -> "500",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> ""
            )
          )
        form.hasErrors shouldBe true
        form.errors.size               shouldBe 1
        form.error("cost").get.message shouldBe "error.required.tobacco.rolling-tobacco"
      }

      "pass on cost with comma separated thousands" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeForm(path)
          .bind(
            Map(
              "weightOrVolume" -> "500",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "4,444.00"
            )
          )
        form.hasErrors shouldBe false
        form.value.get shouldBe TobaccoDto(None, Some(0.5), "FR", None, "EUR", 4444.00, None, None, None, None)
      }

      "return the correct result when filled" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeForm(path)
          .bind(
            Map(
              "weightOrVolume" -> "50",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "500.00"
            )
          )
        val tobaccoDto: TobaccoDto = TobaccoDto(
          noOfSticks = None,
          weightOrVolume = Some(0.05),
          country = "FR",
          originCountry = None,
          currency = "EUR",
          cost = 500.00,
          isVatPaid = None,
          isExcisePaid = None,
          isCustomPaid = None,
          hasEvidence = None
        )

        form.fill(tobaccoDto).value.get shouldBe form.value.get
      }
    }

    ".weightOrVolumeNoOfSticksForm" should {
      val path: ProductPath      = ProductPath(path = "tobacco/cigars")
      val noOfSticks: Int        = 10
      val tobaccoDto: TobaccoDto = TobaccoDto(
        noOfSticks = Some(noOfSticks),
        weightOrVolume = Some(0.05),
        country = "FR",
        originCountry = None,
        currency = "EUR",
        cost = 100.00,
        isVatPaid = None,
        isExcisePaid = None,
        isCustomPaid = None,
        hasEvidence = None
      )

      "fail on weightOrVolume with more than 2 decimal places" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeNoOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks"     -> "10",
              "weightOrVolume" -> "50.999",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "50"
            )
          )
        form.hasErrors shouldBe true
        form.errors.size                         shouldBe 1
        form.error("weightOrVolume").get.message shouldBe "error.max.decimal.places.weight"
      }

      "pass on cost with comma separated thousands" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeNoOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks"     -> "10",
              "weightOrVolume" -> "50",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "1,00.00"
            )
          )
        form.hasErrors shouldBe false
        form.value.get shouldBe tobaccoDto
      }

      "return the correct result when filled" in {
        val form: Form[TobaccoDto] = injected[TobaccoInputController]
          .weightOrVolumeNoOfSticksForm(path)
          .bind(
            Map(
              "noOfSticks"     -> "10",
              "weightOrVolume" -> "50",
              "country"        -> "FR",
              "currency"       -> "EUR",
              "cost"           -> "100.00"
            )
          )

        form.fill(tobaccoDto).value.get shouldBe form.value.get
      }
    }

    ".resilientForm" should {
      val noOfSticks: Int        = 10
      val tobaccoDto: TobaccoDto = TobaccoDto(
        noOfSticks = Some(noOfSticks),
        weightOrVolume = Some(0.05),
        country = "",
        originCountry = None,
        currency = "",
        cost = 0,
        isVatPaid = None,
        isExcisePaid = None,
        isCustomPaid = None,
        hasEvidence = None
      )
      val form: Form[TobaccoDto] = injected[TobaccoInputController].resilientForm
        .bind(
          Map(
            "noOfSticks"     -> "10",
            "weightOrVolume" -> "50"
          )
        )

      "pass returning no errors with valid data" in {
        form.hasErrors shouldBe false
        form.value.get shouldBe tobaccoDto
      }

      "return the correct result when filled" in {
        form.fill(tobaccoDto).value.get shouldBe form.value.get
      }
    }
  }
}
