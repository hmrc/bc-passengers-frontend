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


    "fail on more than allowance 60 litres in sparkling-wine" in {
      val form = injected[AlcoholInputController].alcoholForm(path, Map("L-WINESP" -> 1.1), List("L-WINESP")).bind(Map(
        "weightOrVolume" -> "65",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weightOrVolume").get.message shouldBe "error.l-winesp.limit-exceeded"
    }


    "fail on more than allowance 90 litres in wine" in {
      val form = injected[AlcoholInputController].alcoholForm(path, Map("L-WINE" -> 1.1), List("L-WINE")).bind(Map(
        "weightOrVolume" -> "95",
        "country" -> "FR",
        "currency" -> "EUR",
        "cost" -> "50"
      ))
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weightOrVolume").get.message shouldBe "error.l-wine.limit-exceeded"
    }

    "pass on cost with comma seperated thousands" in {
      val form = injected[AlcoholInputController].alcoholForm(path, Map("L-WINE" -> 1.0), List("L-WINE")).bind(Map(
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
