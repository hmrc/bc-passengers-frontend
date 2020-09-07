/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package models

import org.joda.time.{DateTime, LocalDateTime}
import play.api.data.FormError
import services.CurrencyService
import util.BaseSpec
import play.api.data.Forms._

import scala.util.Random


class DtoTest extends BaseSpec {

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
