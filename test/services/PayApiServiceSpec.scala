/*
 * Copyright 2025 HM Revenue & Customs
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

import models.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.{BaseSpec, parseLocalDate, parseLocalTime}

import java.net.URL
import scala.concurrent.Future

class PayApiServiceSpec extends BaseSpec with ScalaFutures {

  private val mockRequestBuilder: RequestBuilder = mock(classOf[RequestBuilder])
  private val mockHttpClient: HttpClientV2       = mock(classOf[HttpClientV2])
  private val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
  private val mockConfiguration: Configuration   = mock(classOf[Configuration])
  private val countriesService: CountriesService = new CountriesService

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
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
       |    "backUrl": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/user-information-journey",
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
       |    "backUrl": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/user-information-journey",
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

  private trait Setup {
    def httpResponse: HttpResponse
    val userInformation: UserInformation       = UserInformation(
      "Harry",
      "Potter",
      "passport",
      "SX12345",
      "abc@gmail.com",
      "LHR",
      "",
      parseLocalDate("2018-11-12"),
      parseLocalTime("12:20 pm")
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

    val payUrl: String = "http://localhost:9057/pay-api/pngr/pngr/journey/start"

    val urlCapture: ArgumentCaptor[URL]      = ArgumentCaptor.forClass(classOf[URL])
    val bodyCapture: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

    when(mockServicesConfig.baseUrl("pay-api")).thenReturn("http://localhost:9057")
    when(mockConfiguration.getOptional[String]("feedback-frontend.host")).thenReturn(Some("http://localhost:9514"))
    when(mockConfiguration.getOptional[String]("bc-passengers-frontend.host")).thenReturn(Some("http://localhost:9008"))
    when(mockRequestBuilder.withBody(any())(using any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute(using any[HttpReads[HttpResponse]], any()))
      .thenReturn(Future.successful(httpResponse))
    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)

    lazy val s: PayApiService = new PayApiService(
      httpClient = mockHttpClient,
      configuration = mockConfiguration,
      servicesConfig = mockServicesConfig,
      countriesService = countriesService,
      ec = ec
    )
  }

  "Calling requestPaymentUrl" should {

    given messages: Messages = injected[MessagesApi].preferred(enhancedFakeRequest("POST", "/nowhere"))

    "return PayApiServiceFailureResponse when client returns 400" in new Setup {

      override lazy val httpResponse: HttpResponse = HttpResponse(Status.BAD_REQUEST, "")

      val r: PayApiServiceResponse =
        s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, isAmendment = false, None)
          .futureValue

      r shouldBe PayApiServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$payUrl"
      bodyCapture.getValue shouldBe exampleJson
    }

    "return PayApiServiceFailureResponse when client returns 500" in new Setup {

      override lazy val httpResponse: HttpResponse = HttpResponse(Status.BAD_REQUEST, "")

      val r: PayApiServiceResponse =
        s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, isAmendment = false, None)
          .futureValue

      r shouldBe PayApiServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$payUrl"
      bodyCapture.getValue shouldBe exampleJson
    }

    "return a PayApiServiceSuccessResponse with a payment url when http client returns 201" in new Setup {

      override lazy val httpResponse: HttpResponse =
        HttpResponse(Status.CREATED, json = Json.obj("nextUrl" -> "https://example.com"), Map.empty)

      val r: PayApiServiceResponse =
        s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, isAmendment = false, None)
          .futureValue

      r shouldBe PayApiServiceSuccessResponse("https://example.com")

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$payUrl"
      bodyCapture.getValue shouldBe exampleJson
    }

    "return a PayApiServiceSuccessResponse with a payment url when http client returns 201 (when in BST)" in new Setup {

      val uiWithBstArrival: UserInformation = userInformation.copy(
        selectPlaceOfArrival = "",
        enterPlaceOfArrival = "LHR",
        dateOfArrival = parseLocalDate("2018-07-12"),
        timeOfArrival = parseLocalTime("12:20 pm")
      )

      override lazy val httpResponse: HttpResponse =
        HttpResponse(Status.CREATED, json = Json.obj("nextUrl" -> "https://example.com"), Map.empty)

      val r: PayApiServiceResponse =
        s.requestPaymentUrl(
          exampleChargeRef,
          uiWithBstArrival,
          calculatorResponse,
          9700000,
          isAmendment = false,
          None
        ).futureValue

      r shouldBe PayApiServiceSuccessResponse("https://example.com")

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$payUrl"
      bodyCapture.getValue shouldBe exampleJsonForBstArrival
    }

    "return a PayApiServiceSuccessResponse with a declare-your-good back url in amendment journey" in new Setup {

      val uiWithBstArrival: UserInformation = userInformation.copy(
        selectPlaceOfArrival = "",
        enterPlaceOfArrival = "LHR",
        dateOfArrival = parseLocalDate("2018-07-12"),
        timeOfArrival = parseLocalTime("12:20 pm")
      )

      override lazy val httpResponse: HttpResponse =
        HttpResponse(Status.CREATED, json = Json.obj("nextUrl" -> "https://example.com"), Map.empty)

      val expectedJsonForAmendment: JsObject = exampleJsonForBstArrival
        .as[JsObject]
        .deepMerge(
          Json.obj(
            "backUrl"              -> "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/declare-your-goods",
            "amountPaidPreviously" -> "100.99",
            "totalPaidNow"         -> "97000.00"
          )
        )

      val r: PayApiServiceResponse =
        s.requestPaymentUrl(
          exampleChargeRef,
          uiWithBstArrival,
          calculatorResponse,
          9700000,
          isAmendment = true,
          Some("100.99")
        ).futureValue

      r shouldBe PayApiServiceSuccessResponse("https://example.com")

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$payUrl"
      bodyCapture.getValue shouldBe expectedJsonForAmendment
    }

    "return a PayApiServiceSuccessResponse with a pending-payment back url in pending payment journey" in new Setup {

      val uiWithBstArrival: UserInformation = userInformation.copy(
        selectPlaceOfArrival = "",
        enterPlaceOfArrival = "LHR",
        dateOfArrival = parseLocalDate("2018-7-12"),
        timeOfArrival = parseLocalTime("12:20 pm")
      )

      override lazy val httpResponse: HttpResponse =
        HttpResponse(Status.CREATED, json = Json.obj("nextUrl" -> "https://example.com"), Map.empty)

      val expectedJsonForAmendment: JsObject = exampleJsonForBstArrival
        .as[JsObject]
        .deepMerge(
          Json.obj(
            "backUrl"              -> "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/pending-payment",
            "amountPaidPreviously" -> "100.99",
            "totalPaidNow"         -> "97000.00"
          )
        )

      val r: PayApiServiceResponse =
        s.requestPaymentUrl(
          exampleChargeRef,
          uiWithBstArrival,
          calculatorResponse,
          9700000,
          isAmendment = true,
          Some("100.99"),
          Some("pending-payment")
        ).futureValue

      r shouldBe PayApiServiceSuccessResponse("https://example.com")

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$payUrl"
      bodyCapture.getValue shouldBe expectedJsonForAmendment
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
}
