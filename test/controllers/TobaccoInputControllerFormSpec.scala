/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import models.{Country, ProductPath, TobaccoDto}
import util.BaseSpec

class TobaccoInputControllerFormSpec extends BaseSpec {

  "Posting the noOfSticksForm" should {

    val path = ProductPath("tobacco/cigarettes")

    "fail on empty string in noOfSticks" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path).bind(Map(
        "noOfSticks" -> "",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("noOfSticks").get.message shouldBe "error.no_of_sticks.required.tobacco.cigarettes"
    }

    "fail on invalid characters in noOfSticks" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path).bind(Map(
        "noOfSticks" -> "***",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("noOfSticks").get.message shouldBe "error.invalid.characters.noofsticks.tobacco.cigarettes"
    }


    "fail on more than allowance (800) in noOfSticks" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path, Map("L-CIGRT" -> 1.1), List("L-CIGRT")).bind(Map(
        "noOfSticks" -> "801",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("noOfSticks").get.message shouldBe "error.l-cigrt.limit-exceeded"
    }

    "fail on empty string in country" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path).bind(Map(
        "noOfSticks" -> "500",
        "country" -> "",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("country").get.message shouldBe "error.country.invalid"
    }

    "fail on empty string in currency" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path).bind(Map(
        "noOfSticks" -> "500",
        "country" -> "FR",
        "currency" -> "",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("currency").get.message shouldBe "error.currency.invalid"
    }

    "fail on empty string in cost" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path).bind(Map(
        "noOfSticks" -> "500",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> ""
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.required.tobacco.cigarettes"
    }

    "fail on special characters in cost" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path).bind(Map(
        "noOfSticks" -> "500",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "***"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.invalid.characters"
    }

    "fail on more than 2 decimal places in cost" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path).bind(Map(
        "noOfSticks" -> "500",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "4.567"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.invalid.format"
    }

    "fail when cost exceeds 9,999,999,999" in {
      val form = injected[TobaccoInputController].noOfSticksForm(path).bind(Map(
        "noOfSticks" -> "500",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "99999999999"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.exceeded.max"
    }

  }

  "Posting the weightOrVolumeForm" should {

    val path = ProductPath("tobacco/rolling-tobacco")

    "fail on empty string in weightOrVolume" in {
      val form = injected[TobaccoInputController].weightOrVolumeForm(path).bind(Map(
        "weightOrVolume" -> "",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weightOrVolume").get.message shouldBe "error.required.weight.tobacco.rolling-tobacco"
    }

    "fail on special characters in weightOrVolume" in {
      val form = injected[TobaccoInputController].weightOrVolumeForm(path).bind(Map(
        "weightOrVolume" -> "***",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weightOrVolume").get.message shouldBe "error.invalid.characters.weight"
    }

    "fail on more than 2 decimal places in weightOrVolume" in {
      val form = injected[TobaccoInputController].weightOrVolumeForm(path).bind(Map(
        "weightOrVolume" -> "4.567",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weightOrVolume").get.message shouldBe "error.max.decimal.places.weight"
    }

        "fail on more than allowance 1000g in weightOrVolume" in {
          val form = injected[TobaccoInputController].weightOrVolumeForm(path, Map("L-LOOSE" -> 1.1), List("L-LOOSE")).bind(Map(
            "weightOrVolume" -> "1001",
            "country" -> "FR",
            "currency" -> "EUR",
            "cost" -> "50"
          ))
          form.hasErrors shouldBe true
          form.errors.size shouldBe 1
          form.error("weightOrVolume").get.message shouldBe "error.l-loose.limit-exceeded"
        }

    "fail on empty string in country" in {
      val form = injected[TobaccoInputController].weightOrVolumeForm(path).bind(Map(
        "weightOrVolume" -> "500",
        "country" -> "",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("country").get.message shouldBe "error.country.invalid"
    }

    "fail on empty string in currency" in {
      val form = injected[TobaccoInputController].weightOrVolumeForm(path).bind(Map(
        "weightOrVolume" -> "500",
        "country" -> "FR",
        "currency" -> "",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("currency").get.message shouldBe "error.currency.invalid"
    }

    "fail on empty string in cost" in {
      val form = injected[TobaccoInputController].weightOrVolumeForm(path).bind(Map(
        "weightOrVolume" -> "500",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> ""
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.required.tobacco.rolling-tobacco"
    }

    "pass on cost with comma seperated thousands" in {
      val form = injected[TobaccoInputController].weightOrVolumeForm(path).bind(Map(
        "weightOrVolume" -> "500",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "4,444.00"
      ))
      form.hasErrors shouldBe false
      form.value.get shouldBe TobaccoDto(None, Some(0.5), "FR", "EUR", 4444)
    }

  }

}
