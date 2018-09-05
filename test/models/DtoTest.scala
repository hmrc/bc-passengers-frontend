package models

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

}
