/*
 * Copyright 2021 HM Revenue & Customs
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

package models

import org.joda.time.DateTime
import util.BaseSpec


class DtoTest extends BaseSpec {

  "Validating the EnterYourDetailsDto form" should {

    "allow the firstName to be any string that is 35 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "",
        "placeOfArrival.enterPlaceOfArrival" -> "Newcastle Airport",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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

    "return validation errors if the firstName contains special characters" in {
      val formData = Map(
        "firstName" -> "Harry$&",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("firstName").get.message shouldBe "error.first_name.valid"
    }

    "allow the lastName to be any string that is 35 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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

    "return validation errors if the lastName contains special characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter&$",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("lastName").get.message shouldBe "error.last_name.valid"
    }

    "allow the identificationType to be any string that is 40 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "",
        "placeOfArrival.enterPlaceOfArrival" -> "Newcastle Airport",
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

    "return validation errors if the identificationType is empty" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("identification.identificationType").get.message shouldBe "error.identification_type"
    }

    "allow the identificationNumber to be any string that is 40 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "0123456789012345678901234567890123456789",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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

    "return validation errors if the identificationNumber is over 40 characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "01234567890123456789012345678901234567891",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("identification.identificationNumber").get.message shouldBe "error.max-length.identification_number"
    }

    "allow the identificationNumber to be correct if identificationType is telephone and in correct format" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "telephone",
        "identification.identificationNumber" -> "0123456789",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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

    "return validation errors if identificationType is telephone and identificationNumber is not in correct format" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "telephone",
        "identification.identificationNumber" -> "abcdefghi",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("identification").get.message shouldBe "error.telephone_number.format"
    }


    "allow the identificationNumber to be correct if identificationType is other than telephone and in correct format" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "AB7 8b+",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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

    "return validation errors if identificationType is other than telephone and identificationNumber is not in correct format" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "driving",
        "identification.identificationNumber" -> "BE4 8&#)",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("identification").get.message shouldBe "error.identification_number.format"
    }

    "allow the same email and confirmEmail in valid format" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "0123456789012345678901234567890123456789",
        "emailAddress.email"-> "abc@gmail.com",
        "emailAddress.confirmEmail"-> "abc@gmail.com",
        "placeOfArrival.selectPlaceOfArrival" -> "",
        "placeOfArrival.enterPlaceOfArrival" -> "Newcastle Airport",
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

    "return validation errors if the email is invalid" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "abc",
        "emailAddress.confirmEmail"-> "abc@gmail.com",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("emailAddress").get.message shouldBe "error.format.emailAddress"
    }

    "return validation errors if the confirmEmail is invalid" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "abc@gmail.com",
        "emailAddress.confirmEmail"-> "abc",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("emailAddress").get.message shouldBe "error.format.emailAddress"
    }

    "return validation errors if the email and confirmEmail does not match" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "abc@gmail.com",
        "emailAddress.confirmEmail"-> "xyz@gmail.com",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("emailAddress").get.message shouldBe "error.required.emailAddress.no_match"
    }

    "return validation errors if the confirmEmail is empty" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "abc@gmail.com",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("emailAddress").get.message shouldBe "error.required.emailAddress.confirmEmail"
    }

    "return validation errors if the Email is empty" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "abc@gmail.com",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("emailAddress").get.message shouldBe "error.required.emailAddress.email"
    }

    "return validation errors if the Email and Confirm email length exceeds 132 " in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrs@gmail.com",
        "emailAddress.confirmEmail"-> "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrs@gmail.com",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.errors.size shouldBe 2
      form.error("emailAddress.email").get.message shouldBe "error.max-length.email"
      form.error("emailAddress.confirmEmail").get.message shouldBe "error.max-length.email"
    }

    "allow the placeOfArrival.selectPlaceOfArrival to be any string that is 40 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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


    "return validation errors if the placeOfArrival.selectPlaceOfArrival is over 40 characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "Heathrowbutnotactuallyheathrowbecauseitsnowoverfourtycharacters",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("placeOfArrival.selectPlaceOfArrival").get.message shouldBe "error.max-length.place_of_arrival"
    }

    "allow the placeOfArrival.enterPlaceOfArrival to be any string that is 40 characters or under" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "",
        "placeOfArrival.enterPlaceOfArrival" -> "London Airport",
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

    "return validation errors if the placeOfArrival.enterPlaceOfArrival is over 40 characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "",
        "placeOfArrival.enterPlaceOfArrival" -> "Heathrowbutnotactuallyheathrowbecauseitsnowoverfourtycharacters",
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
      form.error("placeOfArrival.enterPlaceOfArrival").get.message shouldBe "error.max-length.place_of_arrival"
    }

    "return validation errors if the placeOfArrival.enterPlaceOfArrival contains special characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "",
        "placeOfArrival.enterPlaceOfArrival" -> "Heathrow&$",
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
      form.error("placeOfArrival.enterPlaceOfArrival").get.message shouldBe "error.place_of_arrival.valid"
    }

    "return validation errors if the placeOfArrival.enterPlaceOfArrival and placeOfArrival.selectPlaceOfArrival is empty" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("placeOfArrival").get.message shouldBe "error.required.place_of_arrival"
    }

    "return validation errors if the dateOfArrival is not a valid date" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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

    "return validation errors if the dateOfArrival has more than 2 characters in day and month field" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
        "dateTimeOfArrival.dateOfArrival.day" -> "9876543210",
        "dateTimeOfArrival.dateOfArrival.month" -> "9876543210",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("dateTimeOfArrival.dateOfArrival").get.message shouldBe "error.enter_a_real_date"
    }


    "return validation errors if all the dateOfArrival contains an out of range date value" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "x",
        "dateTimeOfArrival.dateOfArrival.year" -> "18",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )

      val declarationTime = DateTime.parse("2018-11-23T12:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.errors.map(_.message) shouldBe List("error.enter_a_real_date")
    }

    "allow the dateOfArrival if it is a valid date" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "Heathrowbutnotactuallyheathrowbecauseitsnowoverfourtycharacters",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
      form.error("placeOfArrival.selectPlaceOfArrival").get.message shouldBe "error.max-length.place_of_arrival"
    }


    "allow the dateTimeOfArrival.timeOfArrival.halfday to be either 'am' or 'pm'" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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

    "return a validation error if the dateTimeOfArrival.timeOfArrival has more than 2 characters" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
        "dateTimeOfArrival.dateOfArrival.day" -> "23",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "9876543210",
        "dateTimeOfArrival.timeOfArrival.minute" -> "9876543210",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
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

    "return validation errors if the date/time of arrival is more than 5 days after the declaration date" in {
      val formData = Map(
        "firstName" -> "Harry",
        "lastName" -> "Potter",
        "identification.identificationType" -> "passport",
        "identification.identificationNumber" -> "SX12345",
        "emailAddress.email"-> "",
        "emailAddress.confirmEmail"-> "",
        "placeOfArrival.selectPlaceOfArrival" -> "LHR",
        "placeOfArrival.enterPlaceOfArrival" -> "",
        "dateTimeOfArrival.dateOfArrival.day" -> "28",
        "dateTimeOfArrival.dateOfArrival.month" -> "11",
        "dateTimeOfArrival.dateOfArrival.year" -> "2018",
        "dateTimeOfArrival.timeOfArrival.hour" -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "21",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "am"
      )

      val declarationTime = DateTime.parse("2018-11-23T09:20:00.000")

      val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

      form.hasErrors shouldBe true
      form.errors.size shouldBe 1
      form.error("dateTimeOfArrival").get.message shouldBe "error.5_days"

    }
  }

    "Don't return any validation errors if the date/time of arrival is more than 5 days after the declaration date" in {
    val formData = Map(
      "firstName" -> "Harry",
      "lastName" -> "Potter",
      "identification.identificationType" -> "passport",
      "identification.identificationNumber" -> "SX12345",
      "emailAddress.email"-> "",
      "emailAddress.confirmEmail"-> "",
      "placeOfArrival.selectPlaceOfArrival" -> "LHR",
      "placeOfArrival.enterPlaceOfArrival" -> "",
      "dateTimeOfArrival.dateOfArrival.day" -> "28",
      "dateTimeOfArrival.dateOfArrival.month" -> "11",
      "dateTimeOfArrival.dateOfArrival.year" -> "2018",
      "dateTimeOfArrival.timeOfArrival.hour" -> "09",
      "dateTimeOfArrival.timeOfArrival.minute" -> "19",
      "dateTimeOfArrival.timeOfArrival.halfday" -> "am"
    )

    val declarationTime = DateTime.parse("2018-11-23T09:20:00.000")

    val form = EnterYourDetailsDto.form(declarationTime).bind(formData)

    form.hasErrors shouldBe false

  }

    "Validating the DeclarationRetrievalDto form" should {

    "not return errors when all fields are correct" in {
      val formData = Map(
        "lastName" -> "Smith",
        "referenceNumber" -> "XAPR1234567890"
      )
      val form = DeclarationRetrievalDto.form().bind(formData)
      form.hasErrors shouldBe false
    }

    "not allow the lastName to be any string that is over 35 characters" in {
      val formData = Map(
        "lastName" -> "Smithasdasdasdasdasdasdasdasdasdasdasdasdasdasdsdsdsdsd",
        "referenceNumber" -> "XAPR1234567890"
      )
      val form = DeclarationRetrievalDto.form().bind(formData)
      form.hasErrors shouldBe true
    }

    "not allow the referenceNumber to be any format than the correct format" in {
      val formData = Map(
        "lastName" -> "Smith",
        "referenceNumber" -> "XAXR1234567890"
      )
      val form = DeclarationRetrievalDto.form().bind(formData)
      form.hasErrors shouldBe true
    }

    "allow the referenceNumber to be case insensitive and correct format" in {
      val formData = Map(
        "lastName" -> "Smith",
        "referenceNumber" -> "xvpr1234567890"
      )
      val form = DeclarationRetrievalDto.form().bind(formData)
      form.hasErrors shouldBe false
    }
  }

}
