package models

import play.api.data.FormError
import services.CurrencyService
import util.BaseSpec


class DtoTest extends BaseSpec {

  "Mapping the NoOfSticksAndWeightDto form -> dto -> form" should {

    "change grams to kilos and back again" in {

      val formData = Map(
        "noOfSticks" -> "200",
        "weight" -> "1500"
      )

      val form = NoOfSticksAndWeightDto.form.bind(formData)
      form.value.get shouldBe NoOfSticksAndWeightDto(200, BigDecimal(1.5))
      NoOfSticksAndWeightDto.form.fill(form.value.get).data shouldBe formData
    }

    "fail on decimals" in {

      val formData = Map(
        "noOfSticks" -> "200",
        "weight" -> "1500.55555"
      )

      val form = NoOfSticksAndWeightDto.form.bind(formData)
      form.value.get shouldBe NoOfSticksAndWeightDto(200, BigDecimal(1.50055555))
      NoOfSticksAndWeightDto.form.fill(form.value.get).data shouldBe formData
    }
  }

  "Mapping the WeightDto form -> dto -> form" should {

    "change grams to kilos and back again" in {

      val formData = Map(
        "weight" -> "250"
      )

      val form = WeightDto.form.bind(formData)
      form.value.get shouldBe WeightDto(BigDecimal(0.25))
      WeightDto.form.fill(form.value.get).data shouldBe NoOfSticksAndWeightDto.form.bind(formData).data
    }

    "fail on decimals" in {

      val formData = Map(
        "weight" -> "250.55555"
      )

      val form = WeightDto.form.bind(formData)
      form.value.get shouldBe WeightDto(BigDecimal(0.25055555))
      WeightDto.form.fill(form.value.get).data shouldBe NoOfSticksAndWeightDto.form.bind(formData).data
    }
  }


  "Validating the CurrencyDto form" should {

    "allow itemsRemaining to be absent and set it to 0 if optionalItemsRemaining is true" in {

      val formData = Map(
        "currency" -> "USD"
      )

      val form = CurrencyDto.form(injected[CurrencyService]).bind(formData)
      form.value.get shouldBe CurrencyDto("USD", 0)
    }

    "not allow itemsRemaining to be absent if optionalItemsRemaining is false" in {

      val formData = Map(
        "currency" -> "USD"
      )

      val form = CurrencyDto.form(injected[CurrencyService], optionalItemsRemaining = false).bind(formData)
      form.errors(0) shouldBe FormError("itemsRemaining",List("error.required"))
    }

    "bind if itemsRemaining is not absent and optionalItemsRemaining is false" in {

      val formData = Map(
        "currency" -> "USD",
        "itemsRemaining" -> "1"
      )

      val form = CurrencyDto.form(injected[CurrencyService], optionalItemsRemaining = false).bind(formData)
      form.value.get shouldBe CurrencyDto("USD", 1)
    }
  }

  "Validating the CostDto form" should {

    "allow itemsRemaining to be absent and set it to 0 if optionalItemsRemaining is true" in {

      val formData = Map(
        "cost" -> "1.10"
      )

      val form = CostDto.form().bind(formData)
      form.value.get shouldBe CostDto(BigDecimal(1.10), 0)
    }

    "not allow itemsRemaining to be absent if optionalItemsRemaining is false" in {

      val formData = Map(
        "cost" -> "1.10"
      )

      val form = CostDto.form(optionalItemsRemaining = false).bind(formData)
      form.errors(0) shouldBe FormError("itemsRemaining",List("error.required"))
    }

    "bind if itemsRemaining is not absent and optionalItemsRemaining is false" in {

      val formData = Map(
        "cost" -> "1.10",
        "itemsRemaining" -> "1"
      )

      val form = CostDto.form(optionalItemsRemaining = false).bind(formData)
      form.value.get shouldBe CostDto(BigDecimal(1.10), 1)
    }
  }
}
