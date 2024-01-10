/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import connectors.Cache
import models._

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.MockitoSugar
import play.api.Application
import play.api.http.Status
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.http.WsAllMethods
import uk.gov.hmrc.http.HttpResponse
import util.{BaseSpec, parseLocalDate, parseLocalTime}

import java.util.Locale
import scala.concurrent.Future

class PayApiServiceSpec extends BaseSpec {
  // scalastyle:off magic.number
  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[WsAllMethods].toInstance(MockitoSugar.mock[WsAllMethods]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .configure(
      "microservice.services.pay-api.host" -> "pay-api.service",
      "microservice.services.pay-api.port" -> "80"
    )
    .build()

  override def beforeEach(): Unit = {
    reset(app.injector.instanceOf[WsAllMethods])
    reset(app.injector.instanceOf[Cache])
    super.beforeEach()
  }

  val exampleChargeRef: ChargeReference = ChargeReference("XYPRRVWV52PVDI")

  val exampleJson: JsValue = Json.parse(s"""{
       |    "chargeReference": "XYPRRVWV52PVDI",
       |    "taxToPayInPence": 9700000,
       |    "dateOfArrival": "2018-11-12T12:20:00",
       |    "passengerName": "Harry Potter",
       |    "placeOfArrival": "LHR",
       |    "returnUrl": "http://localhost:9514/feedback/passengers",
       |    "returnUrlFailed": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/tax-due",
       |    "returnUrlCancelled": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/tax-due",
       |    "backUrl": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/user-information",
       |    "items": [
       |        {
       |            "name": "5 litres cider",
       |            "costInGbp": "21.00",
       |            "price": "120.00 USA dollars (USD)",
       |            "purchaseLocation": "United States of America",
       |            "producedIn" : "Algeria",
       |            "evidenceOfOrigin" : "Not required"
       |        },
       |        {
       |            "name": "250 cigarettes",
       |            "costInGbp": "244.49",
       |            "price": "400.00 USA dollars (USD)",
       |            "purchaseLocation": "United States of America",
       |            "producedIn" : "France",
       |            "evidenceOfOrigin" : "No"
       |        },
       |        {
       |            "name": "120g rolling tobacco",
       |            "costInGbp": "198.91",
       |            "price": "200.00 USA dollars (USD)",
       |            "purchaseLocation": "United States of America",
       |            "producedIn" : "Unknown",
       |            "evidenceOfOrigin" : "Not required"
       |        },
       |        {   "name": "Televisions",
       |            "costInGbp": "0.00",
       |            "price": "1500.00 USA dollars (USD)",
       |            "purchaseLocation": "United States of America",
       |            "producedIn": "Germany",
       |            "evidenceOfOrigin": "Yes"
       |        },
       |        {
       |            "name": "Televisions",
       |            "costInGbp": "478.40",
       |            "price": "1300.00 British pounds (GBP)",
       |            "purchaseLocation": "United Kingdom of Great Britain and Northern Ireland",
       |            "producedIn" : "Germany",
       |            "evidenceOfOrigin" : "Yes"
       |        }
       |    ],
       |    "taxBreakdown": {
       |        "customsInGbp":"534.89",
       |        "exciseInGbp":"102.54",
       |        "vatInGbp":"725.03"
       |      }
       |}
    """.stripMargin)

  val exampleJsonForBstArrival: JsValue = Json.parse(s"""{
       |    "chargeReference": "XYPRRVWV52PVDI",
       |    "taxToPayInPence": 9700000,
       |    "dateOfArrival": "2018-07-12T12:20:00",
       |    "passengerName": "Harry Potter",
       |    "placeOfArrival": "LHR",
       |    "returnUrl": "http://localhost:9514/feedback/passengers",
       |    "returnUrlFailed": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/tax-due",
       |    "returnUrlCancelled": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/tax-due",
       |    "backUrl": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/user-information",
       |    "items": [
       |        {
       |            "name": "5 litres cider",
       |            "costInGbp": "21.00",
       |            "price": "120.00 USA dollars (USD)",
       |            "purchaseLocation": "United States of America",
       |            "producedIn" : "Algeria",
       |            "evidenceOfOrigin" : "Not required"
       |        },
       |        {
       |            "name": "250 cigarettes",
       |            "costInGbp": "244.49",
       |            "price": "400.00 USA dollars (USD)",
       |            "purchaseLocation": "United States of America",
       |            "producedIn" : "France",
       |            "evidenceOfOrigin" : "No"
       |        },
       |        {
       |            "name": "120g rolling tobacco",
       |            "costInGbp": "198.91",
       |            "price": "200.00 USA dollars (USD)",
       |            "purchaseLocation": "United States of America",
       |            "producedIn" : "Unknown",
       |            "evidenceOfOrigin" : "Not required"
       |        },
       |        {   "name": "Televisions",
       |            "costInGbp": "0.00",
       |            "price": "1500.00 USA dollars (USD)",
       |            "purchaseLocation": "United States of America",
       |            "producedIn": "Germany",
       |            "evidenceOfOrigin": "Yes"
       |        },
       |        {
       |            "name": "Televisions",
       |            "costInGbp": "478.40",
       |            "price": "1300.00 British pounds (GBP)",
       |            "purchaseLocation": "United Kingdom of Great Britain and Northern Ireland",
       |            "producedIn" : "Germany",
       |            "evidenceOfOrigin" : "Yes"
       |        }
       |    ],
       |    "taxBreakdown": {
       |        "customsInGbp":"534.89",
       |        "exciseInGbp":"102.54",
       |        "vatInGbp":"725.03"
       |      }
       |}
    """.stripMargin)

  trait LocalSetup {
    def httpResponse: HttpResponse
    val userInformation: UserInformation       = UserInformation(
      "Harry",
      "Potter",
      "passport",
      "SX12345",
      "abc@gmail.com",
      "LHR",
      "",
      LocalDate.parse("2018-11-12"),
      LocalTime.parse("12:20 pm", DateTimeFormatter.ofPattern("h:m a", Locale.UK))
    )
    val calculatorResponse: CalculatorResponse = CalculatorResponse(
      Some(
        Alcohol(
          List(
            Band(
              "B",
              List(
                Item(
                  "ALC/A1/CIDER",
                  "91.23",
                  None,
                  Some(5),
                  Calculation("2.00", "0.30", "18.70", "21.00"),
                  Metadata(
                    "5 litres cider",
                    "Cider",
                    "120.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                    Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    Some(Country("DZ", "Algeria", "DZ", isEu = false, isCountry = true, Nil))
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("2.00", "0.30", "18.70", "21.00")
            )
          ),
          Calculation("2.00", "0.30", "18.70", "21.00")
        )
      ),
      Some(
        Tobacco(
          List(
            Band(
              "B",
              List(
                Item(
                  "TOB/A1/CIGRT",
                  "304.11",
                  Some(250),
                  None,
                  Calculation("74.00", "79.06", "91.43", "244.49"),
                  Metadata(
                    "250 cigarettes",
                    "Cigarettes",
                    "400.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                    Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    Some(Country("FR", "France", "FR", isEu = true, isCountry = true, Nil))
                  ),
                  None,
                  Some(false),
                  None,
                  None
                ),
                Item(
                  "TOB/A1/HAND",
                  "152.05",
                  Some(0),
                  Some(0.12),
                  Calculation("26.54", "113.88", "58.49", "198.91"),
                  Metadata(
                    "120g rolling tobacco",
                    "Rolling Tobacco",
                    "200.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                    Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  Some(true),
                  None,
                  None
                )
              ),
              Calculation("100.54", "192.94", "149.92", "443.40")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )
      ),
      Some(
        OtherGoods(
          List(
            Band(
              "C",
              List(
                Item(
                  "OGD/DIGI/TV",
                  "1140.42",
                  None,
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Televisions",
                    "Televisions",
                    "1500.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                    Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    Some(Country("DE", "Germany", "DE", isEu = true, isCountry = true, List("Deutschland")))
                  ),
                  None,
                  Some(true),
                  None,
                  None
                ),
                Item(
                  "OGD/DIGI/TV",
                  "1300.00",
                  None,
                  None,
                  Calculation("0.00", "182.00", "296.40", "478.40"),
                  Metadata(
                    "Televisions",
                    "Televisions",
                    "1300.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("GBP", "British pounds (GBP)", None, Nil),
                    Country(
                      "GB",
                      "United Kingdom of Great Britain and Northern Ireland",
                      "GB",
                      isEu = true,
                      isCountry = true,
                      Nil
                    ),
                    ExchangeRate("1.20", "2018-10-29"),
                    Some(Country("DE", "Germany", "DE", isEu = true, isCountry = true, List("Deutschland")))
                  ),
                  None,
                  Some(true),
                  None,
                  None
                )
              ),
              Calculation("0.00", "341.65", "556.41", "898.06")
            )
          ),
          Calculation("0.00", "341.65", "556.41", "898.06")
        )
      ),
      Calculation("102.54", "534.89", "725.03", "1362.46"),
      withinFreeAllowance = false,
      limits = Map.empty,
      isAnyItemOverAllowance = true
    )

    val receiptDateTime: LocalDateTime = LocalDateTime.parse("2018-11-12T13:56:01.0000")
    lazy val s: PayApiService = {
      val service = injected[PayApiService]
      when(
        service.wsAllMethods.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(httpResponse)
      service
    }
  }

  "Calling requestPaymentUrl" should {

    implicit val messages: Messages = injected[MessagesApi].preferred(enhancedFakeRequest("POST", "/nowhere"))

    "return PayApiServiceFailureResponse when client returns 400" in new LocalSetup {

      override lazy val httpResponse: HttpResponse = HttpResponse(Status.BAD_REQUEST, "")

      val r: PayApiServiceResponse =
        await(
          s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, isAmendment = false, None)
        )
      r shouldBe PayApiServiceFailureResponse
      verify(s.wsAllMethods, times(1)).POST[JsValue, HttpResponse](
        meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),
        meq(exampleJson),
        any()
      )(any(), any(), any(), any())
    }

    "return PayApiServiceFailureResponse when client returns 500" in new LocalSetup {

      override lazy val httpResponse: HttpResponse = HttpResponse(Status.BAD_REQUEST, "")

      val r: PayApiServiceResponse =
        await(
          s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, isAmendment = false, None)
        )
      r shouldBe PayApiServiceFailureResponse
      verify(s.wsAllMethods, times(1)).POST[JsValue, HttpResponse](
        meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),
        meq(exampleJson),
        any()
      )(any(), any(), any(), any())
    }

    "return a PayApiServiceSuccessResponse with a payment url when http client returns 201" in new LocalSetup {

      override lazy val httpResponse: HttpResponse =
        HttpResponse(Status.CREATED, json = Json.obj("nextUrl" -> "https://example.com"), Map.empty)

      val r: PayApiServiceResponse                 =
        await(
          s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, isAmendment = false, None)
        )
      r shouldBe PayApiServiceSuccessResponse("https://example.com")
      verify(s.wsAllMethods, times(1)).POST[JsValue, HttpResponse](
        meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),
        meq(exampleJson),
        any()
      )(any(), any(), any(), any())
    }

    "return a PayApiServiceSuccessResponse with a payment url when http client returns 201 (when in BST)" in new LocalSetup {

      val uiWithBstArrival: UserInformation = userInformation.copy(
        selectPlaceOfArrival = "",
        enterPlaceOfArrival = "LHR",
        dateOfArrival = LocalDate.parse("2018-07-12"),
        timeOfArrival = LocalTime.parse("12:20 pm", DateTimeFormatter.ofPattern("hh:mm a", Locale.UK))
      )

      override lazy val httpResponse: HttpResponse =
        HttpResponse(Status.CREATED, json = Json.obj("nextUrl" -> "https://example.com"), Map.empty)

      val r: PayApiServiceResponse                 =
        await(
          s.requestPaymentUrl(
            exampleChargeRef,
            uiWithBstArrival,
            calculatorResponse,
            9700000,
            isAmendment = false,
            None
          )
        )
      r shouldBe PayApiServiceSuccessResponse("https://example.com")
      verify(s.wsAllMethods, times(1)).POST[JsValue, HttpResponse](
        meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),
        meq(exampleJsonForBstArrival),
        any()
      )(any(), any(), any(), any())
    }

    "return a PayApiServiceSuccessResponse with a declare-your-good back url in amendment journey" in new LocalSetup {

      val uiWithBstArrival: UserInformation = userInformation.copy(
        selectPlaceOfArrival = "",
        enterPlaceOfArrival = "LHR",
        dateOfArrival = LocalDate.parse("2018-07-12"),
        timeOfArrival = LocalTime.parse("12:20 pm", DateTimeFormatter.ofPattern("hh:mm a", Locale.UK))
      )

      override lazy val httpResponse: HttpResponse =
        HttpResponse(Status.CREATED, json = Json.obj("nextUrl" -> "https://example.com"), Map.empty)

      val expectedJsonForAmendment: JsObject       = exampleJsonForBstArrival
        .as[JsObject]
        .deepMerge(
          Json.obj(
            "backUrl"              -> "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods",
            "amountPaidPreviously" -> "100.99",
            "totalPaidNow"         -> "97000.00"
          )
        )

      val r: PayApiServiceResponse = await(
        s.requestPaymentUrl(
          exampleChargeRef,
          uiWithBstArrival,
          calculatorResponse,
          9700000,
          isAmendment = true,
          Some("100.99")
        )
      )
      r shouldBe PayApiServiceSuccessResponse("https://example.com")
      verify(s.wsAllMethods, times(1)).POST[JsValue, HttpResponse](
        meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),
        meq(expectedJsonForAmendment),
        any()
      )(any(), any(), any(), any())
    }

    "return a PayApiServiceSuccessResponse with a pending-payment back url in pending payment journey" in new LocalSetup {

      val uiWithBstArrival: UserInformation = userInformation.copy(
        selectPlaceOfArrival = "",
        enterPlaceOfArrival = "LHR",
        dateOfArrival = parseLocalDate("2018-7-12"),
        timeOfArrival = parseLocalTime("12:20 pm")
      )

      override lazy val httpResponse: HttpResponse =
        HttpResponse(Status.CREATED, json = Json.obj("nextUrl" -> "https://example.com"), Map.empty)

      val expectedJsonForAmendment: JsObject       = exampleJsonForBstArrival
        .as[JsObject]
        .deepMerge(
          Json.obj(
            "backUrl"              -> "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/pending-payment",
            "amountPaidPreviously" -> "100.99",
            "totalPaidNow"         -> "97000.00"
          )
        )

      val r: PayApiServiceResponse = await(
        s.requestPaymentUrl(
          exampleChargeRef,
          uiWithBstArrival,
          calculatorResponse,
          9700000,
          isAmendment = true,
          Some("100.99"),
          Some("pending-payment")
        )
      )
      r shouldBe PayApiServiceSuccessResponse("https://example.com")
      verify(s.wsAllMethods, times(1)).POST[JsValue, HttpResponse](
        meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),
        meq(expectedJsonForAmendment),
        any()
      )(any(), any(), any(), any())
    }
  }

  "Calling generateChargeRef" should {

    "always return a valid charge ref" in {

      def isValidChargeReference(chargeReference: String): Boolean = {

        val alphaNums          = chargeReference.slice(4, chargeReference.length).toList
        val alphaNumsToConvert = chargeReference.slice(2, chargeReference.length).toList

        val alphaCheckCharacter: Char = {
          val equivalentValues        = ('A' to 'Z').zip(33 to 58).toMap
          val charIndexWeights        = (3 to 14).zip(List(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)).toMap
          val remainderCheckChars     = (0 to 22)
            .zip(
              List('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'X', 'J', 'K', 'L', 'M', 'N', 'Y', 'P', 'Q', 'R', 'S', 'T',
                'Z', 'V', 'W')
            )
            .toMap
          val convertedAlphaNums      =
            alphaNumsToConvert.map(alphaNum => equivalentValues.getOrElse(alphaNum, alphaNum.asDigit))
          val remainderFromConversion =
            convertedAlphaNums.zipWithIndex.map(x => x._1 * charIndexWeights(x._2 + 3)).sum % 23
          remainderCheckChars(remainderFromConversion)
        }

        chargeReference.toList match {
          case 'X' :: checkChar :: 'P' :: 'R' :: tail if checkChar == alphaCheckCharacter && tail == alphaNums => true
          case _                                                                                               => false
        }
      }

      isValidChargeReference(ChargeReference.generate.value) shouldBe true
    }
  }
  // scalastyle:on magic.number
}
