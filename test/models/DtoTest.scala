package models

import org.joda.time.{DateTime, LocalDateTime}
import play.api.data.FormError
import services.CurrencyService
import util.BaseSpec
import play.api.data.Forms._


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
      form.errors(0) shouldBe FormError("itemsRemaining", List("error.required"))
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