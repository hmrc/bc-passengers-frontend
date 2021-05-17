/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import models.{AlcoholDto, ProductPath}
import util.BaseSpec

class AlcoholInputControllerFormSpec extends BaseSpec {

  "Posting the alcoholForm" should {

    val path = ProductPath("alcohol/beer")

    "fail on empty string in weightOrVolume" in {
      val form = injected[AlcoholInputController].alcoholForm(path).bind(Map(
        "weightOrVolume" -> "",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weightOrVolume").get.message shouldBe "error.required.volume.alcohol.beer"
    }

    "fail on special character in weightOrVolume" in {
      val form = injected[AlcoholInputController].alcoholForm(path).bind(Map(
        "weightOrVolume" -> "***",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weightOrVolume").get.message shouldBe "error.invalid.characters.volume"
    }

    "fail on more than 3 decimal places in weightOrVolume" in {
      val form = injected[AlcoholInputController].alcoholForm(path).bind(Map(
        "weightOrVolume" -> "4.5678",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weightOrVolume").get.message shouldBe "error.max.decimal.places.volume"
    }
  }

  "Posting the alcoholForm" should {

    val path = ProductPath("alcohol/sparkling-wine")

    "pass on cost with comma separated thousands" in {
      val form = injected[AlcoholInputController].alcoholForm(path).bind(Map(
        "weightOrVolume" -> "90",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "4,444.00"
      ))
      form.hasErrors shouldBe false
      form.value.get shouldBe AlcoholDto(90, "FR", None, "EUR", 4444,None,None, None, None)

    }

    "pass on more than allowance and sending empty limits so shouldn't validate maximum limits" in {
      val form = injected[AlcoholInputController].alcoholForm(path).bind(Map(
        "weightOrVolume" -> "300",
        "country" -> "IN",
        "currency" -> "INR",
        "cost" -> "5000.00"
      ))
      form.hasErrors shouldBe false
      form.value.get shouldBe AlcoholDto(300, "IN", None, "INR", 5000, None, None, None, None)

    }

  }
}
