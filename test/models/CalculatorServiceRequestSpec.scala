/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import util.BaseSpec

class CalculatorServiceRequestSpec extends BaseSpec {

  "Converting a CalculatorServiceRequest to json" should {

    trait Setup {

      def todaysDate: String = LocalDate.parse("2019-04-30").format(DateTimeFormatter.ISO_DATE)

      implicit val messages: Messages = injected[MessagesApi].preferred(EnhancedFakeRequest("POST", "/nowhere")(app))

      def weightOrVolume: Option[BigDecimal]
      def noOfSticks: Option[Int]

      def templateId: String

      lazy val cr: CalculatorServiceRequest = CalculatorServiceRequest(isPrivateCraft = false,
        isAgeOver17 = true,
        isArrivingNI = false,
        isUKVatPaid = None,
        isUKExcisePaid = Some(false),
        isUKResident = Some(true),
        isUccRelief = Some(false),
        List(
          PurchasedItem(
            PurchasedProductInstance(
              ProductPath("path/to/dummy-product"),
              "iid0",
              weightOrVolume,
              noOfSticks,
              Some(Country("EG", "Egypt", "EG", isEu = false, Nil)),
              Some("CAD"),
              Some(BigDecimal("2.00"))
            ),
            ProductTreeLeaf("dummy-product", "Dummy product name", "DUMMY/RATE/ID", templateId, Nil),
            Currency("CAD", "Canadian dollars (CAD)", Some("CAD"), Nil), BigDecimal("1.13"), ExchangeRate("1.7654", todaysDate)
          )
        )
      )
    }

    "convert cigarettes correctly" in new Setup {

      override val weightOrVolume: None.type = None
      override val noOfSticks: Option[Int] = Some(200)

      override val templateId: String = "cigarettes"

      val expected: JsValue = Json.parse(
        """{
          |  "isPrivateCraft" : false,
          |  "isAgeOver17" : true,
          |  "isArrivingNI" : false,
          |  "isUKExcisePaid" : false,
          |  "isUKResident" : true,
          |  "isUccRelief" : false,
          |  "items" : [ {
          |    "purchaseCost" : "1.13",
          |    "rateId" : "DUMMY/RATE/ID",
          |    "noOfUnits" : 200,
          |    "metadata" : {
          |      "description" : "200 dummy product name",
          |      "name" : "Dummy product name",
          |      "cost" : "2.00",
          |      "currency" : {
          |        "code" : "CAD",
          |        "displayName" : "Canadian dollars (CAD)",
          |        "valueForConversion" : "CAD",
          |        "currencySynonyms" : [ ]
          |      },
          |      "country" : {
          |        "code": "EG",
          |        "countryName" : "Egypt",
          |        "alphaTwoCode" : "EG",
          |        "isEu" : false,
          |        "countrySynonyms" : [ ]
          |      },
          |      "exchangeRate" : {
          |        "rate" : "1.7654",
          |        "date" : "2019-04-30"
          |      }
          |    }
          |  } ]
          |}
        """.stripMargin)

      Json.toJson(cr) shouldBe expected
    }

    "convert cigars correctly" in new Setup {

      override val weightOrVolume: Option[BigDecimal] = Some(BigDecimal("1.0"))
      override val noOfSticks: Option[Int] = Some(50)

      override val templateId: String = "cigars"

      val expected: JsValue = Json.parse(
        """{
          |  "isPrivateCraft" : false,
          |  "isAgeOver17" : true,
          |  "isArrivingNI" : false,
          |  "isUKExcisePaid" : false,
          |  "isUKResident" : true,
          |  "isUccRelief" : false,
          |  "items" : [ {
          |    "purchaseCost" : "1.13",
          |    "rateId" : "DUMMY/RATE/ID",
          |    "weightOrVolume" : 1,
          |    "noOfUnits" : 50,
          |    "metadata" : {
          |      "description" : "50 dummy product name",
          |      "name" : "Dummy product name",
          |      "cost" : "2.00",
          |      "currency" : {
          |        "code" : "CAD",
          |        "displayName" : "Canadian dollars (CAD)",
          |        "valueForConversion" : "CAD",
          |        "currencySynonyms" : [ ]
          |      },
          |      "country" : {
          |        "code": "EG",
          |        "countryName" : "Egypt",
          |        "alphaTwoCode" : "EG",
          |        "isEu" : false,
          |        "countrySynonyms" : [ ]
          |      },
          |      "exchangeRate" : {
          |        "rate" : "1.7654",
          |        "date" : "2019-04-30"
          |      }
          |    }
          |  } ]
          |}
        """.stripMargin)

      Json.toJson(cr) shouldBe expected
    }

    "convert tobacco correctly" in new Setup {

      override val weightOrVolume: Option[BigDecimal] = Some(BigDecimal("1.0"))
      override val noOfSticks: None.type = None

      override val templateId: String = "tobacco"

      val expected: JsValue = Json.parse(
        """{
          |  "isPrivateCraft" : false,
          |  "isAgeOver17" : true,
          |  "isArrivingNI" : false,
          |  "isUKExcisePaid" : false,
          |  "isUKResident" : true,
          |  "isUccRelief" : false,
          |  "items" : [ {
          |    "purchaseCost" : "1.13",
          |    "rateId" : "DUMMY/RATE/ID",
          |    "weightOrVolume" : 1,
          |    "metadata" : {
          |      "description" : "1000g of dummy product name",
          |      "name" : "Dummy product name",
          |      "cost" : "2.00",
          |      "currency" : {
          |        "code" : "CAD",
          |        "displayName" : "Canadian dollars (CAD)",
          |        "valueForConversion" : "CAD",
          |        "currencySynonyms" : [ ]
          |      },
          |      "country" : {
          |        "code": "EG",
          |        "countryName" : "Egypt",
          |        "alphaTwoCode" : "EG",
          |        "isEu" : false,
          |        "countrySynonyms" : [ ]
          |      },
          |      "exchangeRate" : {
          |        "rate" : "1.7654",
          |        "date" : "2019-04-30"
          |      }
          |    }
          |  } ]
          |}
        """.stripMargin)

      Json.toJson(cr) shouldBe expected
    }

    "convert alcohol correctly" in new Setup {

      override val weightOrVolume: Option[BigDecimal] = Some(BigDecimal("1.0"))
      override val noOfSticks: None.type = None

      override val templateId: String = "alcohol"

      val expected: JsValue = Json.parse(
        """{
          |  "isPrivateCraft" : false,
          |  "isAgeOver17" : true,
          |  "isArrivingNI" : false,
          |  "isUKExcisePaid" : false,
          |  "isUKResident" : true,
          |  "isUccRelief" : false,
          |  "items" : [ {
          |    "purchaseCost" : "1.13",
          |    "rateId" : "DUMMY/RATE/ID",
          |    "weightOrVolume" : 1,
          |    "metadata" : {
          |      "description" : "1.0 litre dummy product name",
          |      "name" : "Dummy product name",
          |      "cost" : "2.00",
          |      "currency" : {
          |        "code" : "CAD",
          |        "displayName" : "Canadian dollars (CAD)",
          |        "valueForConversion" : "CAD",
          |        "currencySynonyms" : [ ]
          |      },
          |      "country" : {
          |        "code": "EG",
          |        "countryName" : "Egypt",
          |        "alphaTwoCode" : "EG",
          |        "isEu" : false,
          |        "countrySynonyms" : [ ]
          |      },
          |      "exchangeRate" : {
          |        "rate" : "1.7654",
          |        "date" : "2019-04-30"
          |      }
          |    }
          |  } ]
          |}
        """.stripMargin)

      Json.toJson(cr) shouldBe expected
    }

    "convert other goods correctly" in new Setup {

      override val weightOrVolume: None.type = None
      override val noOfSticks: None.type = None

      override val templateId: String = "other-goods"

      val expected: JsValue = Json.parse(
        """{
          |  "isPrivateCraft" : false,
          |  "isAgeOver17" : true,
          |  "isArrivingNI" : false,
          |  "isUKExcisePaid" : false,
          |  "isUKResident" : true,
          |  "isUccRelief" : false,
          |  "items" : [ {
          |    "purchaseCost" : "1.13",
          |    "rateId" : "DUMMY/RATE/ID",
          |    "metadata" : {
          |      "description" : "Dummy product name",
          |      "name" : "Dummy product name",
          |      "cost" : "2.00",
          |      "currency" : {
          |        "code" : "CAD",
          |        "displayName" : "Canadian dollars (CAD)",
          |        "valueForConversion" : "CAD",
          |        "currencySynonyms" : [ ]
          |      },
          |      "country" : {
          |        "code": "EG",
          |        "countryName" : "Egypt",
          |        "alphaTwoCode" : "EG",
          |        "isEu" : false,
          |        "countrySynonyms" : [ ]
          |      },
          |      "exchangeRate" : {
          |        "rate" : "1.7654",
          |        "date" : "2019-04-30"
          |      }
          |    }
          |  } ]
          |}
        """.stripMargin)

      Json.toJson(cr) shouldBe expected
    }
  }

}
