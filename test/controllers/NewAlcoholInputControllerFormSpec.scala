package controllers

import models.ProductPath
import util.BaseSpec

class NewAlcoholInputControllerFormSpec extends BaseSpec {

  "Posting the alcoholForm" should {

    val path = ProductPath("alcohol/beer")

    "fail on empty string in weightOrVolume" in {
      val form = injected[NewAlcoholInputController].alcoholForm(path).bind(Map(
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
      val form = injected[NewAlcoholInputController].alcoholForm(path).bind(Map(
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
      val form = injected[NewAlcoholInputController].alcoholForm(path).bind(Map(
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
      val form = injected[NewAlcoholInputController].alcoholForm(path, Map("L-WINESP" -> 1.1), List("L-WINESP")).bind(Map(
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
        val form = injected[NewAlcoholInputController].alcoholForm(path, Map("L-WINE" -> 1.1), List("L-WINE")).bind(Map(
          "weightOrVolume" -> "95",
          "country" -> "FR",
          "currency" -> "EUR",
          "cost" -> "50"
        ))
        form.hasErrors shouldBe true
        form.errors.size shouldBe 1
        form.error("weightOrVolume").get.message shouldBe "error.l-wine.limit-exceeded"
      }

  }
}
