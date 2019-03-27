package models

import org.joda.time.{DateTime, LocalDateTime}
import play.api.data.FormError
import services.CurrencyService
import util.BaseSpec
import play.api.data.Forms._

import scala.util.Random


class DtoTest extends BaseSpec {

  "Mapping the VolumeDto form -> dto -> form" should {

    "fail on empty string" in {
      val formData = Map("volume" -> "")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("volume").get.message shouldBe "error.required.volume"
    }

    "fail on entering 0" in {
      val formData = Map("volume" -> "0")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("volume").get.message shouldBe "error.invalid.characters.volume"
    }

    "fail on entering 0.0" in {
      val formData = Map("volume" -> "0.0")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("volume").get.message shouldBe "error.invalid.characters.volume"
    }

    "pass on entering .5" in {
      val formData = Map("volume" -> ".5")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "pass on entering 5." in {
      val formData = Map("volume" -> "5.")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "fail on entering 00" in {
      val formData = Map("volume" -> "00")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("volume").get.message shouldBe "error.invalid.characters.volume"
    }

    "fail on invalid characters" in {
      val formData = Map("volume" -> "gfss")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("volume").get.message shouldBe "error.invalid.characters.volume"
    }

    "pass on 3 decimal places" in {
      val formData = Map("volume" -> "100.555")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "fail on over 3 decimal places" in {
      val formData = Map("volume" -> "100.5555")
      val form = VolumeDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("volume").get.message shouldBe "error.invalid.format.volume"
    }

  }

  "Mapping the NoOfSticksDto form -> dto -> form" should {

    "fail on empty string" in {
      val formData = Map("noOfSticks" -> "")
      val form = NoOfSticksDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("noOfSticks").get.message shouldBe "error.required.noofsticks"
    }

    "fail on entering 0" in {
      val formData = Map("noOfSticks" -> "0")
      val form = NoOfSticksDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("noOfSticks").get.message shouldBe "error.invalid.characters.noofsticks"
    }

    "fail on invalid characters" in {
      val formData = Map("noOfSticks" -> "500.5")
      val form = NoOfSticksDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("noOfSticks").get.message shouldBe "error.invalid.characters.noofsticks"
    }

  }

  "Mapping the NoOfSticksAndWeightDto form -> dto -> form" should {

    "change grams to kilos and back again" in {

      val formData = Map(
        "noOfSticks" -> "200",
        "weight" -> "1500.55"
      )

      val form = NoOfSticksAndWeightDto.form().bind(formData)
      form.value.get shouldBe NoOfSticksAndWeightDto(200, BigDecimal(1.50055))
      NoOfSticksAndWeightDto.form().fill(form.value.get).data shouldBe formData
    }

    "fail on empty strings" in {
      val formData = Map(
        "noOfSticks" -> "",
        "weight" -> "")
      val form = NoOfSticksAndWeightDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 2
      form.error("noOfSticks").get.message shouldBe "error.required.noofsticks"
      form.error("weight").get.message shouldBe "error.required.weight"
    }

    "fail on entering 0" in {
      val formData = Map(
        "noOfSticks" -> "0",
        "weight" -> "0")
      val form = NoOfSticksAndWeightDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 2
      form.error("noOfSticks").get.message shouldBe "error.invalid.characters.noofsticks"
      form.error("weight").get.message shouldBe "error.invalid.characters.weight"
    }

    "pass on entering weight .5" in {
      val formData = Map(
        "noOfSticks" -> "100",
        "weight" -> ".5")
      val form = NoOfSticksAndWeightDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "pass on entering weight 5." in {
      val formData = Map(
        "noOfSticks" -> "100",
        "weight" -> "5.")
      val form = NoOfSticksAndWeightDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "fail on invalid characters" in {
      val formData = Map(
        "noOfSticks" -> "10.5",
        "weight" -> "100g")
      val form = NoOfSticksAndWeightDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 2
      form.error("noOfSticks").get.message shouldBe "error.invalid.characters.noofsticks"
      form.error("weight").get.message shouldBe "error.invalid.characters.weight"
    }

  }

  "Mapping the WeightDto form -> dto -> form" should {

    "change grams to kilos and back again" in {

      val formData = Map(
        "weight" -> "250.55"
      )

      val form = WeightDto.form().bind(formData)
      form.value.get shouldBe WeightDto(BigDecimal(0.25055))
      WeightDto.form().fill(form.value.get).data shouldBe NoOfSticksAndWeightDto.form().bind(formData).data
    }

    "fail on empty strings" in {
      val formData = Map(
        "weight" -> "")
      val form = WeightDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weight").get.message shouldBe "error.required.weight"
    }

    "fail on entering 0" in {
      val formData = Map(
        "weight" -> "0")
      val form = WeightDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weight").get.message shouldBe "error.invalid.characters.weight"
    }

    "pass on entering weight .5" in {
      val formData = Map(
        "weight" -> ".5")
      val form = WeightDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "pass on entering weight 5." in {
      val formData = Map(
        "weight" -> "5.")
      val form = WeightDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "fail on invalid characters" in {
      val formData = Map(
        "weight" -> "100g")
      val form = WeightDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("weight").get.message shouldBe "error.invalid.characters.weight"
    }

  }


  "Validating the CurrencyDto form" should {

    "allow itemsRemaining to be absent and set it to 0 if optionalItemsRemaining is true" in {

      val formData = Map(
        "currency" -> "USA dollars (USD)"
      )

      val form = CurrencyDto.form(injected[CurrencyService]).bind(formData)
      form.value.get shouldBe CurrencyDto("USD", 0)
    }

    "not allow itemsRemaining to be absent if optionalItemsRemaining is false" in {

      val formData = Map(
        "currency" -> "USA dollars (USD)"
      )

      val form = CurrencyDto.form(injected[CurrencyService], optionalItemsRemaining = false).bind(formData)
      form.errors(0) shouldBe FormError("itemsRemaining", List("error.required"))
    }

    "bind if itemsRemaining is not absent and optionalItemsRemaining is false" in {

      val formData = Map(
        "currency" -> "USA dollars (USD)",
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
      form.errors(0) shouldBe FormError("itemsRemaining", List("error.required"))
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

  "Mapping the CostDto form -> dto -> form" should {

    "fail on empty string" in {
      val formData = Map("cost" -> "")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.required.cost"
    }

    "fail on entering 0" in {
      val formData = Map("cost" -> "0")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.invalid.characters"
    }

    "fail on entering 0.0" in {
      val formData = Map("cost" -> "0.0")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.invalid.characters"
    }

    "fail on entering 00" in {
      val formData = Map("cost" -> "00")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.invalid.characters"
    }

    "pass on entering 50." in {
      val formData = Map("cost" -> "50.")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "fail on invalid characters" in {
      val formData = Map("cost" -> "£500")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.invalid.characters"
    }

    "pass on 2 decimal places" in {
      val formData = Map("cost" -> "1500.55")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "fail on over 2 decimal places" in {
      val formData = Map("cost" -> "1500.555")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.invalid.format"
    }

    "pass on up to maximum cost 9,999,999,999" in {
      val formData = Map("cost" -> "9999999999")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "fail on over maximum cost 9,999,999,999" in {
      val formData = Map("cost" -> "10000000000")
      val form = CostDto.form().bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("cost").get.message shouldBe "error.exceeded.max"
    }
  }

  "Mapping the QuantityDto form -> dto -> form" should {

    "fail on empty string" in {
      val formData = Map("quantity" -> "")
      val form = QuantityDto.form.bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("quantity").get.message shouldBe "error.required.quantity"
    }

    "fail on entering 0" in {
      val formData = Map("quantity" -> "0")
      val form = QuantityDto.form.bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("quantity").get.message shouldBe "error.invalid.characters.quantity"
    }

    "fail on entering 0.0" in {
      val formData = Map("quantity" -> "0.0")
      val form = QuantityDto.form.bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("quantity").get.message shouldBe "error.invalid.characters.quantity"
    }


    "fail on entering 00" in {
      val formData = Map("quantity" -> "00")
      val form = QuantityDto.form.bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("quantity").get.message shouldBe "error.invalid.characters.quantity"
    }

    "fail on invalid characters" in {
      val formData = Map("quantity" -> "500.5")
      val form = QuantityDto.form.bind(formData)
      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("quantity").get.message shouldBe "error.invalid.characters.quantity"
    }

  }


  "Validating the EnterYourDetailsDto form" should {

    "allow the firstName to be any string that is 35 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe false
    }


    "return validation errors if the firstName is over 35 characters" in {
      val formData = Map(
        "firstName" -> "Harrybuthasareallylongfirstnameinstead",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("firstName").get.message shouldBe "error.max-length.first_name"
    }

    "allow the lastName to be any string that is 35 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe false
    }


    "return validation errors if the lastName is over 35 characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potterbutnowhislastnamehasbecomereallylong",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("lastName").get.message shouldBe "error.max-length.last_name"
    }

    "allow the passportNumber to be any string that is 40 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "0123456789012345678901234567890123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe false
    }


    "return validation errors if the passportNumber is over 40 characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "01234567890123456789012345678901234567891",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("passportNumber").get.message shouldBe "error.max-length.passport_number"
    }

    "allow the placeOfArrival to be any string that is 40 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe false
    }


    "return validation errors if the placeOfArrival is over 40 characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrowbutnotactuallyheathrowbecauseitsnowoverfourtycharacters",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("placeOfArrival").get.message shouldBe "error.max-length.place_of_arrival"
    }


    "return validation errors if the dateOfArrival is not a valid date" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "40",
        "dateTimeOfArrival.dateOfArrival.month" -> "23",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("dateTimeOfArrival.dateOfArrival").get.message shouldBe "error.enter_a_real_date"
    }



    "return validation errors if the dateOfArrival is using special characters in any field" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "s@",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("dateTimeOfArrival.dateOfArrival").get.message shouldBe "error.only_whole_numbers"
    }


    "return validation errors if all the dateOfArrival contains an out of range date value" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "50",
        "dateTimeOfArrival.dateOfArrival.month" -> "04",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.errors.map(_.message) shouldBe List("error.enter_a_real_date")
    }

    "return validation errors if all the dateOfArrival fields are blank" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "",
        "dateTimeOfArrival.dateOfArrival.month" -> "",
        "dateTimeOfArrival.dateOfArrival.year" -> "",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.errors.map(_.message) shouldBe List("error.enter_a_date")
    }

    "return validation errors if any but not all of the dateOfArrival fields are blank" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "1",
        "dateTimeOfArrival.dateOfArrival.month" -> "",
        "dateTimeOfArrival.dateOfArrival.year" -> "",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.errors.map(_.message) shouldBe List("error.include_day_month_and_year")
    }

    "return an error if the year field is not 4 chars long" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "18",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.errors.map(_.message) shouldBe List("error.year_length")
    }

    "check for whole numbers before it checks for year length" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "x",
        "dateTimeOfArrival.dateOfArrival.year" -> "18",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.errors.map(_.message) shouldBe List("error.only_whole_numbers")
    }

    "allow the dateOfArrival if it is a valid date" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe false
    }

    "allow the timeOfArrival to be any string that is 40 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe false
    }


    "return validation errors if the timeOfArrival is over 40 characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrowbutnotactuallyheathrowbecauseitsnowoverfourtycharacters",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("placeOfArrival").get.message shouldBe "error.max-length.place_of_arrival"
    }


    "allow the dateTimeOfArrival.timeOfArrival.halfday to be either 'am' or 'pm'" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe false
    }

    "return a validation error if the dateTimeOfArrival.timeOfArrival.halfday is not 'am' or 'pm'" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "somethingelse"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("dateTimeOfArrival.timeOfArrival").get.message shouldBe "error.enter_a_real_time"
    }


    "allow the timeOfArrival to be after the declaration time" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "30",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "am"
      )

      val declarationTime = DateTime.parse("2018-11-23T09:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)


      form.hasErrors shouldBe false
    }

    "allow the timeOfArrival to be after the declaration time (3 hours leeway)" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "06",
        "dateTimeOfArrival.timeOfArrival.minute" -> "21",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "am"
      )

      val declarationTime = DateTime.parse("2018-11-23T09:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe false
    }

    "return validation errors if the date/time of arrival is more than 72 hours after the declaration date" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "passportNumber" -> "123456789",
        "placeOfArrival" -> "Heathrow",
        "dateTimeOfArrival.dateOfArrival.day" -> "26",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "20",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "am"
      )

      val declarationTime = DateTime.parse("2018-11-23T09:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("dateTimeOfArrival").get.message shouldBe "error.72_hours"

    }
  }

}
