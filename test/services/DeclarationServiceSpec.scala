/*
 * Copyright 2024 HM Revenue & Customs
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

import audit.AuditingTools
import connectors.Cache
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.{BaseSpec, EnhancedJsObject, parseLocalDate, parseLocalTime}

import java.net.URL
import java.time.LocalDateTime
import scala.concurrent.Future

class DeclarationServiceSpec extends BaseSpec with ScalaFutures {
  implicit val messages: MessagesApi = injected[MessagesApi]

  private val mockRequestBuilder: RequestBuilder           = mock(classOf[RequestBuilder])
  private val mockHttpClient: HttpClientV2                 = mock(classOf[HttpClientV2])
  private val mockCache: Cache                             = mock(classOf[Cache])
  private val mockServicesConfig: ServicesConfig           = mock(classOf[ServicesConfig])
  private val mockAuditingTools: AuditingTools             = mock(classOf[AuditingTools])
  private val mockAuditConnector: AuditConnector           = mock(classOf[AuditConnector])
  private val portsOfArrivalService: PortsOfArrivalService = new PortsOfArrivalService

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
    reset(mockCache)
    reset(mockAuditConnector)
    super.beforeEach()
  }

  private trait EndpointSetup {
    def httpResponse: HttpResponse
    def url: String

    val cid: String = "fe28db96-d9db-4220-9e12-f2d267267c29"

    val urlCapture: ArgumentCaptor[URL]                 = ArgumentCaptor.forClass(classOf[URL])
    val bodyCapture: ArgumentCaptor[JsValue]            = ArgumentCaptor.forClass(classOf[JsValue])
    val headerCapture: ArgumentCaptor[(String, String)] = ArgumentCaptor.forClass(classOf[(String, String)])

    when(mockServicesConfig.baseUrl("bc-passengers-declarations")).thenReturn("http://localhost:9073")
    when(mockRequestBuilder.withBody(any())(using any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute(using any[HttpReads[HttpResponse]], any()))
      .thenReturn(Future.successful(httpResponse))
    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
  }

  private trait Setup {
    def journeyDataInCache: Option[JourneyData]

    when(mockServicesConfig.getString("declarations.amend")).thenReturn("DeclarationAmend")
    when(mockServicesConfig.getString("declarations.create")).thenReturn("DeclarationCreate")
    when(mockServicesConfig.getString("declarations.euOnly")).thenReturn("EU Only")
    when(mockServicesConfig.getString("declarations.nonEuOnly")).thenReturn("NON_EU Only")
    when(mockServicesConfig.getString("declarations.greatBritain")).thenReturn("Great Britain")
    when(mockServicesConfig.getString("declarations.telephonePrefix")).thenReturn("XPASSID")

    val declarationService: DeclarationService = new DeclarationService(
      cache = mockCache,
      portsOfArrivalService = portsOfArrivalService,
      httpClient = mockHttpClient,
      servicesConfig = mockServicesConfig,
      auditConnector = mockAuditConnector,
      auditingTools = mockAuditingTools,
      ec = ec
    )
  }

  val userInformation: UserInformation = UserInformation(
    "Harry",
    "Potter",
    "passport",
    "SX12345",
    "abc@gmail.com",
    "LHR",
    "",
    parseLocalDate("2018-05-31"),
    parseLocalTime("01:20 pm")
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
                  "label.alcohol.cider",
                  "120.00",
                  DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                  Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                  Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                  ExchangeRate("1.2", "2018-10-29"),
                  None
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
                  "label.tobacco.cigarettes",
                  "400.00",
                  DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                  Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                  Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                  ExchangeRate("1.2", "2018-10-29"),
                  None
                ),
                None,
                None,
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
                  "label.tobacco.rolling-tobacco",
                  "200.00",
                  DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                  Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                  Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                  ExchangeRate("1.2", "2018-10-29"),
                  None
                ),
                None,
                None,
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
                Calculation("0.00", "159.65", "260.01", "419.66"),
                Metadata(
                  "Televisions",
                  "label.other-goods.electronic-devices.televisions",
                  "1500.00",
                  DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                  Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                  Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                  ExchangeRate("1.2", "2018-10-29"),
                  None
                ),
                None,
                None,
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
                  "label.other-goods.electronic-devices.televisions",
                  "1300.00",
                  DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                  Currency("GBP", "British pounds (GBP)", None, Nil),
                  Country("GB", "United Kingdom", "GB", isEu = false, isCountry = true, Nil),
                  ExchangeRate("1.2", "2018-10-29"),
                  None
                ),
                None,
                None,
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

  "Calling DeclarationService.submitDeclaration" should {

    val jd: JourneyData = JourneyData(
      euCountryCheck = Some("nonEuOnly"),
      arrivingNICheck = Some(false),
      amendmentCount = Some(0)
    )

    val expectedJsObj: JsObject = Json.obj(
      "journeyData"              -> Json.toJsObject(jd.copy(userInformation = Some(userInformation))),
      "simpleDeclarationRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "receiptDate"              -> "2018-05-31T12:14:08Z",
          "acknowledgementReference" -> "XJPR57685246250",
          "requestParameters"        -> Json.arr(
            Json.obj(
              "paramName"  -> "REGIME",
              "paramValue" -> "PNGR"
            )
          )
        ),
        "requestDetail" -> Json.obj(
          "customerReference"  -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
          "personalDetails"    -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
          "contactDetails"     -> Json.obj("emailAddress" -> "abc@gmail.com"),
          "declarationHeader"  -> Json.obj(
            "chargeReference"       -> "XJPR5768524625",
            "portOfEntry"           -> "LHR",
            "portOfEntryName"       -> "Heathrow Airport",
            "expectedDateOfArrival" -> "2018-05-31",
            "timeOfEntry"           -> "13:20",
            "messageTypes"          -> Json.obj("messageType" -> "DeclarationCreate"),
            "travellingFrom"        -> "NON_EU Only",
            "onwardTravelGBNI"      -> "GB",
            "uccRelief"             -> false,
            "ukVATPaid"             -> false,
            "ukExcisePaid"          -> false
          ),
          "declarationTobacco" -> Json.obj(
            "totalExciseTobacco"     -> "100.54",
            "totalCustomsTobacco"    -> "192.94",
            "totalVATTobacco"        -> "149.92",
            "declarationItemTobacco" -> Json.arr(
              Json.obj(
                "commodityDescription" -> "Cigarettes",
                "quantity"             -> "250",
                "goodsValue"           -> "400.00",
                "valueCurrency"        -> "USD",
                "valueCurrencyName"    -> "USA dollars (USD)",
                "originCountry"        -> "US",
                "originCountryName"    -> "United States of America",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "304.11",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "74.00",
                "customsGBP"           -> "79.06",
                "vatGBP"               -> "91.43"
              ),
              Json.obj(
                "commodityDescription" -> "Rolling tobacco",
                "weight"               -> "120.00",
                "goodsValue"           -> "200.00",
                "valueCurrency"        -> "USD",
                "valueCurrencyName"    -> "USA dollars (USD)",
                "originCountry"        -> "US",
                "originCountryName"    -> "United States of America",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "152.05",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "26.54",
                "customsGBP"           -> "113.88",
                "vatGBP"               -> "58.49"
              )
            )
          ),
          "declarationAlcohol" -> Json.obj(
            "totalExciseAlcohol"     -> "2.00",
            "totalCustomsAlcohol"    -> "0.30",
            "totalVATAlcohol"        -> "18.70",
            "declarationItemAlcohol" -> Json.arr(
              Json.obj(
                "commodityDescription" -> "Cider",
                "volume"               -> "5",
                "goodsValue"           -> "120.00",
                "valueCurrency"        -> "USD",
                "valueCurrencyName"    -> "USA dollars (USD)",
                "originCountry"        -> "US",
                "originCountryName"    -> "United States of America",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "91.23",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "2.00",
                "customsGBP"           -> "0.30",
                "vatGBP"               -> "18.70"
              )
            )
          ),
          "declarationOther"   -> Json.obj(
            "totalExciseOther"     -> "0.00",
            "totalCustomsOther"    -> "341.65",
            "totalVATOther"        -> "556.41",
            "declarationItemOther" -> Json.arr(
              Json.obj(
                "commodityDescription" -> "Television",
                "quantity"             -> "1",
                "goodsValue"           -> "1500.00",
                "valueCurrency"        -> "USD",
                "valueCurrencyName"    -> "USA dollars (USD)",
                "originCountry"        -> "US",
                "originCountryName"    -> "United States of America",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "1140.42",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "0.00",
                "customsGBP"           -> "159.65",
                "vatGBP"               -> "260.01"
              ),
              Json.obj(
                "commodityDescription" -> "Television",
                "quantity"             -> "1",
                "goodsValue"           -> "1300.00",
                "valueCurrency"        -> "GBP",
                "valueCurrencyName"    -> "British pounds (GBP)",
                "originCountry"        -> "GB",
                "originCountryName"    -> "United Kingdom",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "1300.00",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "0.00",
                "customsGBP"           -> "182.00",
                "vatGBP"               -> "296.40"
              )
            )
          ),
          "liabilityDetails"   -> Json.obj(
            "totalExciseGBP"  -> "102.54",
            "totalCustomsGBP" -> "534.89",
            "totalVATGBP"     -> "725.03",
            "grandTotalGBP"   -> "1362.46"
          )
        )
      )
    )

    val expectedSendJson: JsObject = expectedJsObj.alterFields {
      case ("chargeReference", _)          => None
      case ("acknowledgementReference", _) => None
    }

    val expectedTelephoneValueSendJson: JsObject = expectedJsObj.alterFields {
      case ("chargeReference", _)          => None
      case ("acknowledgementReference", _) => None
      case ("customerReference", _)        =>
        Some(("customerReference", Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false)))
    }

    "return a DeclarationServiceFailureResponse if the backend returns 400" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(BAD_REQUEST, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/submit-declaration"

      val ui: UserInformation = userInformation.copy(identificationType = "passport", identificationNumber = "SX12345")

      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)

      val r: DeclarationServiceResponse = declarationService
        .submitDeclaration(ui, calculatorResponse, jd, LocalDateTime.parse("2018-05-31T12:14:08"), cid)
        .futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).setHeader(headerCapture.capture())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())
      verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(meq(hc), any())

      urlCapture.getValue                                        shouldBe url"$url"
      headerCapture.getValue.asInstanceOf[Seq[(String, String)]] shouldBe Seq("X-Correlation-ID" -> cid)
      bodyCapture.getValue                                       shouldBe expectedTelephoneValueSendJson
    }

    "return a DeclarationServiceFailureResponse if the backend returns 500" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(INTERNAL_SERVER_ERROR, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/submit-declaration"

      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)

      val r: DeclarationServiceResponse = declarationService
        .submitDeclaration(
          userInformation,
          calculatorResponse,
          jd,
          LocalDateTime.parse("2018-05-31T12:14:08"),
          cid
        )
        .futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).setHeader(headerCapture.capture())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())
      verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(meq(hc), any())

      urlCapture.getValue                                        shouldBe url"$url"
      headerCapture.getValue.asInstanceOf[Seq[(String, String)]] shouldBe Seq("X-Correlation-ID" -> cid)
      bodyCapture.getValue                                       shouldBe expectedSendJson
    }

    "return a DeclarationServiceSuccessResponse if the backend returns 202" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(ACCEPTED, expectedJsObj.toString)

      override def url: String = "http://localhost:9073/bc-passengers-declarations/submit-declaration"

      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)

      val r: DeclarationServiceResponse =
        declarationService
          .submitDeclaration(
            userInformation,
            calculatorResponse,
            jd,
            LocalDateTime.parse("2018-05-31T12:14:08"),
            cid
          )
          .futureValue

      r shouldBe DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).setHeader(headerCapture.capture())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())
      verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(meq(hc), any())

      urlCapture.getValue                                        shouldBe url"$url"
      headerCapture.getValue.asInstanceOf[Seq[(String, String)]] shouldBe Seq("X-Correlation-ID" -> cid)
      bodyCapture.getValue                                       shouldBe expectedSendJson
    }
  }

  "Calling DeclarationService.submitAmendment" should {

    val calculation: Calculation                 = Calculation("0.00", "12.50", "102.50", "115.00")
    val liabilityDetails: LiabilityDetails       = LiabilityDetails("0.00", "12.50", "102.50", "115.00")
    val productPath: ProductPath                 = ProductPath("other-goods/adult/adult-footwear")
    val otherGoodsSearchItem                     =
      OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))
    val country: Country                         = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())
    val purchasedProductInstances                = List(
      PurchasedProductInstance(
        productPath,
        "UnOGll",
        None,
        None,
        Some(country),
        None,
        Some("GBP"),
        Some(500),
        Some(otherGoodsSearchItem),
        Some(false),
        Some(false),
        None,
        Some(false),
        None,
        isEditable = Some(false)
      )
    )
    val declarationResponse: DeclarationResponse =
      DeclarationResponse(calculation, liabilityDetails, purchasedProductInstances)
    val previousDeclarationRequest               = PreviousDeclarationRequest("Potter", "XJPR5768524625")
    val jd: JourneyData                          = JourneyData(
      prevDeclaration = Some(true),
      previousDeclarationRequest = Some(previousDeclarationRequest),
      euCountryCheck = Some("nonEuOnly"),
      arrivingNICheck = Some(false),
      calculatorResponse = Some(calculatorResponse),
      declarationResponse = Some(declarationResponse),
      purchasedProductInstances = purchasedProductInstances
    )
    val cumulativePPIs                           = purchasedProductInstances ++ purchasedProductInstances
    val jdTobeSent: JourneyData                  = JourneyData(
      euCountryCheck = Some("nonEuOnly"),
      arrivingNICheck = Some(false),
      userInformation = Some(userInformation),
      calculatorResponse = Some(calculatorResponse),
      purchasedProductInstances = cumulativePPIs,
      amendmentCount = Some(1)
    )

    val expectedJsObj: JsObject = Json.obj(
      "journeyData"              -> Json.toJsObject(jdTobeSent),
      "simpleDeclarationRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "receiptDate"              -> "2018-05-31T12:14:08Z",
          "acknowledgementReference" -> "XJPR57685246251",
          "requestParameters"        -> Json.arr(
            Json.obj(
              "paramName"  -> "REGIME",
              "paramValue" -> "PNGR"
            )
          )
        ),
        "requestDetail" -> Json.obj(
          "customerReference"         -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
          "personalDetails"           -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
          "contactDetails"            -> Json.obj("emailAddress" -> "abc@gmail.com"),
          "declarationHeader"         -> Json.obj(
            "chargeReference"       -> "XJPR5768524625",
            "portOfEntry"           -> "LHR",
            "portOfEntryName"       -> "Heathrow Airport",
            "expectedDateOfArrival" -> "2018-05-31",
            "timeOfEntry"           -> "13:20",
            "messageTypes"          -> Json.obj("messageType" -> "DeclarationAmend"),
            "travellingFrom"        -> "NON_EU Only",
            "onwardTravelGBNI"      -> "GB",
            "uccRelief"             -> false,
            "ukVATPaid"             -> false,
            "ukExcisePaid"          -> false
          ),
          "declarationTobacco"        -> Json.obj(
            "totalExciseTobacco"     -> "100.54",
            "totalCustomsTobacco"    -> "192.94",
            "totalVATTobacco"        -> "149.92",
            "declarationItemTobacco" -> Json.arr(
              Json.obj(
                "commodityDescription" -> "Cigarettes",
                "quantity"             -> "250",
                "goodsValue"           -> "400.00",
                "valueCurrency"        -> "USD",
                "valueCurrencyName"    -> "USA dollars (USD)",
                "originCountry"        -> "US",
                "originCountryName"    -> "United States of America",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "304.11",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "74.00",
                "customsGBP"           -> "79.06",
                "vatGBP"               -> "91.43"
              ),
              Json.obj(
                "commodityDescription" -> "Rolling tobacco",
                "weight"               -> "120.00",
                "goodsValue"           -> "200.00",
                "valueCurrency"        -> "USD",
                "valueCurrencyName"    -> "USA dollars (USD)",
                "originCountry"        -> "US",
                "originCountryName"    -> "United States of America",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "152.05",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "26.54",
                "customsGBP"           -> "113.88",
                "vatGBP"               -> "58.49"
              )
            )
          ),
          "declarationAlcohol"        -> Json.obj(
            "totalExciseAlcohol"     -> "2.00",
            "totalCustomsAlcohol"    -> "0.30",
            "totalVATAlcohol"        -> "18.70",
            "declarationItemAlcohol" -> Json.arr(
              Json.obj(
                "commodityDescription" -> "Cider",
                "volume"               -> "5",
                "goodsValue"           -> "120.00",
                "valueCurrency"        -> "USD",
                "valueCurrencyName"    -> "USA dollars (USD)",
                "originCountry"        -> "US",
                "originCountryName"    -> "United States of America",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "91.23",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "2.00",
                "customsGBP"           -> "0.30",
                "vatGBP"               -> "18.70"
              )
            )
          ),
          "declarationOther"          -> Json.obj(
            "totalExciseOther"     -> "0.00",
            "totalCustomsOther"    -> "341.65",
            "totalVATOther"        -> "556.41",
            "declarationItemOther" -> Json.arr(
              Json.obj(
                "commodityDescription" -> "Television",
                "quantity"             -> "1",
                "goodsValue"           -> "1500.00",
                "valueCurrency"        -> "USD",
                "valueCurrencyName"    -> "USA dollars (USD)",
                "originCountry"        -> "US",
                "originCountryName"    -> "United States of America",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "1140.42",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "0.00",
                "customsGBP"           -> "159.65",
                "vatGBP"               -> "260.01"
              ),
              Json.obj(
                "commodityDescription" -> "Television",
                "quantity"             -> "1",
                "goodsValue"           -> "1300.00",
                "valueCurrency"        -> "GBP",
                "valueCurrencyName"    -> "British pounds (GBP)",
                "originCountry"        -> "GB",
                "originCountryName"    -> "United Kingdom",
                "exchangeRate"         -> "1.20",
                "exchangeRateDate"     -> "2018-10-29",
                "goodsValueGBP"        -> "1300.00",
                "VATRESClaimed"        -> false,
                "exciseGBP"            -> "0.00",
                "customsGBP"           -> "182.00",
                "vatGBP"               -> "296.40"
              )
            )
          ),
          "liabilityDetails"          -> Json.obj(
            "totalExciseGBP"  -> "102.54",
            "totalCustomsGBP" -> "534.89",
            "totalVATGBP"     -> "725.03",
            "grandTotalGBP"   -> "1362.46"
          ),
          "amendmentLiabilityDetails" -> Json.obj(
            "additionalExciseGBP"  -> "102.54",
            "additionalCustomsGBP" -> "522.39",
            "additionalVATGBP"     -> "622.53",
            "additionalTotalGBP"   -> "1247.46"
          )
        )
      )
    )

    val expectedTelephoneValueSendJson: JsObject = expectedJsObj.alterFields { case ("customerReference", _) =>
      Some(("customerReference", Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false)))
    }

    "return a DeclarationServiceFailureResponse if the backend returns 400" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(BAD_REQUEST, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/submit-amendment"

      val ui: UserInformation = userInformation.copy(identificationType = "passport", identificationNumber = "SX12345")

      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)

      val r: DeclarationServiceResponse = declarationService
        .submitAmendment(ui, calculatorResponse, jd, LocalDateTime.parse("2018-05-31T12:14:08"), cid)
        .futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).setHeader(headerCapture.capture())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())
      verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(meq(hc), any())

      urlCapture.getValue                                        shouldBe url"$url"
      headerCapture.getValue.asInstanceOf[Seq[(String, String)]] shouldBe Seq("X-Correlation-ID" -> cid)
      bodyCapture.getValue                                       shouldBe expectedTelephoneValueSendJson
    }

    "return a DeclarationServiceFailureResponse if the backend returns 500" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(INTERNAL_SERVER_ERROR, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/submit-amendment"

      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)

      val r: DeclarationServiceResponse = declarationService
        .submitAmendment(
          userInformation,
          calculatorResponse,
          jd,
          LocalDateTime.parse("2018-05-31T12:14:08"),
          cid
        )
        .futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).setHeader(headerCapture.capture())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())
      verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(meq(hc), any())

      urlCapture.getValue                                        shouldBe url"$url"
      headerCapture.getValue.asInstanceOf[Seq[(String, String)]] shouldBe Seq("X-Correlation-ID" -> cid)
      bodyCapture.getValue                                       shouldBe expectedJsObj
    }

    "return a DeclarationServiceSuccessResponse if the backend returns 202" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(ACCEPTED, expectedJsObj.toString)

      override def url: String = "http://localhost:9073/bc-passengers-declarations/submit-amendment"

      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)

      val r: DeclarationServiceResponse =
        declarationService
          .submitAmendment(
            userInformation,
            calculatorResponse,
            jd,
            LocalDateTime.parse("2018-05-31T12:14:08"),
            cid
          )
          .futureValue

      r shouldBe DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).setHeader(headerCapture.capture())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())
      verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(meq(hc), any())

      urlCapture.getValue                                        shouldBe url"$url"
      headerCapture.getValue.asInstanceOf[Seq[(String, String)]] shouldBe Seq("X-Correlation-ID" -> cid)
      bodyCapture.getValue                                       shouldBe expectedJsObj
    }
  }

  "Calling DeclarationService.buildPartialDeclarationMessage" should {

    "truncate a product description to 40 characters if the product description is too big in the metadata." in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val jd: JourneyData =
        JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), amendmentCount = Some(0))

      val userInformation: UserInformation = UserInformation(
        "Harry",
        "Potter",
        "passport",
        "SX12345",
        "abc@gmail.com",
        "LHR",
        "",
        parseLocalDate("2018-05-31"),
        parseLocalTime("8:2 am")
      )

      val calculatorResponse: CalculatorResponse = CalculatorResponse(
        alcohol = Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ALC/A1/CIDER",
                    "250.10",
                    None,
                    Some(BigDecimal("2.00")),
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "2 litres cider",
                      "Cider but for some reason has a really long product description",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("100.54", "192.94", "149.92", "443.40")
          )
        ),
        otherGoods = None,
        tobacco = None,
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )

      val dm: JsObject = declarationService.buildPartialDeclarationOrAmendmentMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(
        "journeyData"              -> Json.toJsObject(jd.copy(userInformation = Some(userInformation))),
        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate"       -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr(Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR"))
          ),
          "requestDetail" -> Json.obj(
            "customerReference"  -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
            "personalDetails"    -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails"     -> Json.obj("emailAddress" -> "abc@gmail.com"),
            "declarationHeader"  -> Json.obj(
              "portOfEntry"           -> "LHR",
              "portOfEntryName"       -> "Heathrow Airport",
              "expectedDateOfArrival" -> "2018-05-31",
              "timeOfEntry"           -> "08:02",
              "messageTypes"          -> Json.obj("messageType" -> "DeclarationCreate"),
              "travellingFrom"        -> "EU Only",
              "onwardTravelGBNI"      -> "GB",
              "uccRelief"             -> false,
              "ukVATPaid"             -> false,
              "ukExcisePaid"          -> false
            ),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol"     -> "100.54",
              "totalCustomsAlcohol"    -> "192.94",
              "totalVATAlcohol"        -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider but for some reason has a really l",
                  "volume"               -> "2.00",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00"
                )
              )
            ),
            "liabilityDetails"   -> Json.obj(
              "totalExciseGBP"  -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP"     -> "149.92",
              "grandTotalGBP"   -> "443.40"
            )
          )
        )
      )
    }

    "generate the correct payload and set euCountryCheck is nonEuOnly and arrivingNI flag is true" in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val jd: JourneyData =
        JourneyData(euCountryCheck = Some("nonEuOnly"), arrivingNICheck = Some(true), amendmentCount = Some(0))

      val userInformation: UserInformation = UserInformation(
        "Harry",
        "Potter",
        "passport",
        "SX12345",
        "abc@gmail.com",
        "LHR",
        "",
        parseLocalDate("2018-05-31"),
        parseLocalTime("01:20 pm")
      )

      val calculatorResponse: CalculatorResponse = CalculatorResponse(
        alcohol = Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ALC/A1/CIDER",
                    "250.10",
                    None,
                    Some(BigDecimal("2.00")),
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "2 litres cider",
                      "Cider",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
              Band(
                "B",
                List(
                  Item(
                    "ALC/A2/BEER",
                    "304.11",
                    None,
                    Some(BigDecimal("3.00")),
                    Calculation("74.00", "79.06", "91.43", "244.49"),
                    Metadata(
                      "3 litres beer",
                      "Beer",
                      "400.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "ALC/A3/WINE",
                    "152.05",
                    None,
                    Some(BigDecimal("4.00")),
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "4 litres wine",
                      "Wine",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
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
        otherGoods = Some(
          OtherGoods(
            List(
              Band(
                "A",
                List(
                  Item(
                    "OGD/CLTHS/CHILD",
                    "250.10",
                    None,
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "children's clothes",
                      "Children's Clothes",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
              Band(
                "B",
                List(
                  Item(
                    "OGD/BKS/MISC",
                    "304.11",
                    None,
                    None,
                    Calculation("74.00", "79.06", "91.43", "244.49"),
                    Metadata(
                      "books or publications",
                      "Books or Publications",
                      "400.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "OGD/BKS/MISC",
                    "152.05",
                    None,
                    None,
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "books or publications",
                      "Books or Publications",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
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
        tobacco = Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "TOB/A1/CIGRT",
                    "250.10",
                    Some(200),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "200 cigarettes",
                      "Cigarettes",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
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
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "TOB/A1/HAND",
                    "152.05",
                    Some(0),
                    Some(BigDecimal("0.12")),
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "120g rolling tobacco",
                      "Rolling Tobacco",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
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
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )

      val dm: JsObject = declarationService.buildPartialDeclarationOrAmendmentMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(
        "journeyData"              -> Json.toJsObject(jd.copy(userInformation = Some(userInformation))),
        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate"       -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr(Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR"))
          ),
          "requestDetail" -> Json.obj(
            "customerReference"  -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
            "personalDetails"    -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails"     -> Json.obj("emailAddress" -> "abc@gmail.com"),
            "declarationHeader"  -> Json.obj(
              "portOfEntry"           -> "LHR",
              "portOfEntryName"       -> "Heathrow Airport",
              "expectedDateOfArrival" -> "2018-05-31",
              "timeOfEntry"           -> "13:20",
              "messageTypes"          -> Json.obj("messageType" -> "DeclarationCreate"),
              "travellingFrom"        -> "NON_EU Only",
              "onwardTravelGBNI"      -> "NI",
              "uccRelief"             -> false,
              "ukVATPaid"             -> false,
              "ukExcisePaid"          -> false
            ),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco"     -> "100.54",
              "totalCustomsTobacco"    -> "192.94",
              "totalVATTobacco"        -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity"             -> "200",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity"             -> "250",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight"               -> "120.00",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49"
                )
              )
            ),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol"     -> "100.54",
              "totalCustomsAlcohol"    -> "192.94",
              "totalVATAlcohol"        -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider",
                  "volume"               -> "2.00",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Beer",
                  "volume"               -> "3.00",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Wine",
                  "volume"               -> "4.00",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49"
                )
              )
            ),
            "declarationOther"   -> Json.obj(
              "totalExciseOther"     -> "100.54",
              "totalCustomsOther"    -> "192.94",
              "totalVATOther"        -> "149.92",
              "declarationItemOther" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Children's Clothes",
                  "quantity"             -> "1",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity"             -> "1",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity"             -> "1",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49"
                )
              )
            ),
            "liabilityDetails"   -> Json.obj(
              "totalExciseGBP"  -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP"     -> "149.92",
              "grandTotalGBP"   -> "443.40"
            )
          )
        )
      )
    }

    "generate the correct payload and adhere to the schema when journeyData a calculation with all product categories in" in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val jd: JourneyData = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        isUKVatPaid = Some(true),
        isUKVatExcisePaid = Some(true),
        isUKResident = Some(false),
        isUccRelief = Some(true),
        amendmentCount = Some(0)
      )

      val userInformation: UserInformation = UserInformation(
        "Harry",
        "Potter",
        "passport",
        "SX12345",
        "abc@gmail.com",
        "",
        "LHR",
        parseLocalDate("2018-05-31"),
        parseLocalTime("01:20 pm")
      )

      val calculatorResponse: CalculatorResponse = CalculatorResponse(
        alcohol = Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ALC/A1/CIDER",
                    "250.10",
                    None,
                    Some(BigDecimal("2.00")),
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "2 litres cider",
                      "Cider",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
              Band(
                "B",
                List(
                  Item(
                    "ALC/A2/BEER",
                    "304.11",
                    None,
                    Some(BigDecimal("3.00")),
                    Calculation("74.00", "79.06", "91.43", "244.49"),
                    Metadata(
                      "3 litres beer",
                      "Beer",
                      "400.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "ALC/A3/WINE",
                    "152.05",
                    None,
                    Some(BigDecimal("4.00")),
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "4 litres wine",
                      "Wine",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
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
        otherGoods = Some(
          OtherGoods(
            List(
              Band(
                "A",
                List(
                  Item(
                    "OGD/CLTHS/CHILD",
                    "250.10",
                    None,
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "children's clothes",
                      "Children's Clothes",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
              Band(
                "B",
                List(
                  Item(
                    "OGD/BKS/MISC",
                    "304.11",
                    None,
                    None,
                    Calculation("74.00", "79.06", "91.43", "244.49"),
                    Metadata(
                      "books or publications",
                      "Books or Publications",
                      "400.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "OGD/BKS/MISC",
                    "152.05",
                    None,
                    None,
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "books or publications",
                      "Books or Publications",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
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
        tobacco = Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "TOB/A1/CIGRT",
                    "250.10",
                    Some(200),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "200 cigarettes",
                      "Cigarettes",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
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
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "TOB/A1/HAND",
                    "152.05",
                    Some(0),
                    Some(BigDecimal("0.12")),
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "120g rolling tobacco",
                      "Rolling Tobacco",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
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
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )

      val dm: JsObject = declarationService.buildPartialDeclarationOrAmendmentMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(
        "journeyData"              -> Json.toJsObject(jd.copy(userInformation = Some(userInformation))),
        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate"       -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr(Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR"))
          ),
          "requestDetail" -> Json.obj(
            "customerReference"  -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
            "personalDetails"    -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails"     -> Json.obj("emailAddress" -> "abc@gmail.com"),
            "declarationHeader"  -> Json.obj(
              "portOfEntry"           -> "LHR",
              "portOfEntryName"       -> "LHR",
              "expectedDateOfArrival" -> "2018-05-31",
              "timeOfEntry"           -> "13:20",
              "messageTypes"          -> Json.obj("messageType" -> "DeclarationCreate"),
              "travellingFrom"        -> "Great Britain",
              "onwardTravelGBNI"      -> "NI",
              "uccRelief"             -> true,
              "ukVATPaid"             -> true,
              "ukExcisePaid"          -> true
            ),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco"     -> "100.54",
              "totalCustomsTobacco"    -> "192.94",
              "totalVATTobacco"        -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity"             -> "200",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity"             -> "250",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight"               -> "120.00",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49"
                )
              )
            ),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol"     -> "100.54",
              "totalCustomsAlcohol"    -> "192.94",
              "totalVATAlcohol"        -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider",
                  "volume"               -> "2.00",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Beer",
                  "volume"               -> "3.00",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Wine",
                  "volume"               -> "4.00",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49"
                )
              )
            ),
            "declarationOther"   -> Json.obj(
              "totalExciseOther"     -> "100.54",
              "totalCustomsOther"    -> "192.94",
              "totalVATOther"        -> "149.92",
              "declarationItemOther" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Children's Clothes",
                  "quantity"             -> "1",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity"             -> "1",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity"             -> "1",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49"
                )
              )
            ),
            "liabilityDetails"   -> Json.obj(
              "totalExciseGBP"  -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP"     -> "149.92",
              "grandTotalGBP"   -> "443.40"
            )
          )
        )
      )
    }

    "generate the correct payload and including ukVATPaid, ukExcisePaid, uccRelief, isMade and eu flags at item level" in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val jd: JourneyData = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        isUKResident = Some(true),
        amendmentCount = Some(0)
      )

      val userInformation: UserInformation = UserInformation(
        "Harry",
        "Potter",
        "passport",
        "SX12345",
        "abc@gmail.com",
        "LHR",
        "",
        parseLocalDate("2018-05-31"),
        parseLocalTime("01:20 pm")
      )

      val calculatorResponse: CalculatorResponse = CalculatorResponse(
        alcohol = Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ALC/A1/CIDER",
                    "250.10",
                    None,
                    Some(BigDecimal("2.00")),
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "2 litres cider",
                      "Cider",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    Some(false),
                    None,
                    Some(false),
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
              Band(
                "B",
                List(
                  Item(
                    "ALC/A2/BEER",
                    "304.11",
                    None,
                    Some(BigDecimal("3.00")),
                    Calculation("74.00", "79.06", "91.43", "244.49"),
                    Metadata(
                      "3 litres beer",
                      "Beer",
                      "400.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    Some(true),
                    None,
                    Some(true),
                    None
                  ),
                  Item(
                    "ALC/A3/WINE",
                    "152.05",
                    None,
                    Some(BigDecimal("4.00")),
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "4 litres wine",
                      "Wine",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    Some(false),
                    None,
                    Some(true),
                    None
                  )
                ),
                Calculation("100.54", "192.94", "149.92", "443.40")
              )
            ),
            Calculation("100.54", "192.94", "149.92", "443.40")
          )
        ),
        otherGoods = Some(
          OtherGoods(
            List(
              Band(
                "A",
                List(
                  Item(
                    "OGD/CLTHS/CHILD",
                    "250.10",
                    None,
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "children's clothes",
                      "Children's Clothes",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      Some(Country("EU", "European Union", "EU", isEu = true, isCountry = false, Nil))
                    ),
                    None,
                    Some(true),
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
              Band(
                "B",
                List(
                  Item(
                    "OGD/BKS/MISC",
                    "304.11",
                    None,
                    None,
                    Calculation("74.00", "79.06", "91.43", "244.49"),
                    Metadata(
                      "books or publications",
                      "Books or Publications",
                      "400.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      Some(Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil))
                    ),
                    None,
                    Some(false),
                    None,
                    None
                  ),
                  Item(
                    "OGD/BKS/MISC",
                    "152.05",
                    None,
                    None,
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "books or publications",
                      "Books or Publications",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      Some(Country("EU", "European Union", "EU", isEu = true, isCountry = false, Nil))
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
        tobacco = Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "TOB/A1/CIGRT",
                    "250.10",
                    Some(200),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "200 cigarettes",
                      "Cigarettes",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    Some(true),
                    None,
                    Some(true),
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
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
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    Some(true),
                    None,
                    Some(true),
                    None
                  ),
                  Item(
                    "TOB/A1/HAND",
                    "152.05",
                    Some(0),
                    Some(BigDecimal("0.12")),
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "120g rolling tobacco",
                      "Rolling Tobacco",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    Some(true),
                    None,
                    Some(true),
                    None
                  )
                ),
                Calculation("100.54", "192.94", "149.92", "443.40")
              )
            ),
            Calculation("100.54", "192.94", "149.92", "443.40")
          )
        ),
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )

      val dm: JsObject = declarationService.buildPartialDeclarationOrAmendmentMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(
        "journeyData"              -> Json.toJsObject(jd.copy(userInformation = Some(userInformation))),
        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate"       -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr(Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR"))
          ),
          "requestDetail" -> Json.obj(
            "customerReference"  -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> true),
            "personalDetails"    -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails"     -> Json.obj("emailAddress" -> "abc@gmail.com"),
            "declarationHeader"  -> Json.obj(
              "portOfEntry"           -> "LHR",
              "portOfEntryName"       -> "Heathrow Airport",
              "expectedDateOfArrival" -> "2018-05-31",
              "timeOfEntry"           -> "13:20",
              "messageTypes"          -> Json.obj("messageType" -> "DeclarationCreate"),
              "travellingFrom"        -> "Great Britain",
              "onwardTravelGBNI"      -> "NI",
              "uccRelief"             -> false,
              "ukVATPaid"             -> false,
              "ukExcisePaid"          -> false
            ),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco"     -> "100.54",
              "totalCustomsTobacco"    -> "192.94",
              "totalVATTobacco"        -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity"             -> "200",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00",
                  "ukVATPaid"            -> true,
                  "ukExcisePaid"         -> true
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity"             -> "250",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43",
                  "ukVATPaid"            -> true,
                  "ukExcisePaid"         -> true
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight"               -> "120.00",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49",
                  "ukVATPaid"            -> true,
                  "ukExcisePaid"         -> true
                )
              )
            ),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol"     -> "100.54",
              "totalCustomsAlcohol"    -> "192.94",
              "totalVATAlcohol"        -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider",
                  "volume"               -> "2.00",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00",
                  "ukVATPaid"            -> false,
                  "ukExcisePaid"         -> false
                ),
                Json.obj(
                  "commodityDescription" -> "Beer",
                  "volume"               -> "3.00",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43",
                  "ukVATPaid"            -> true,
                  "ukExcisePaid"         -> true
                ),
                Json.obj(
                  "commodityDescription" -> "Wine",
                  "volume"               -> "4.00",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49",
                  "ukVATPaid"            -> false,
                  "ukExcisePaid"         -> true
                )
              )
            ),
            "declarationOther"   -> Json.obj(
              "totalExciseOther"     -> "100.54",
              "totalCustomsOther"    -> "192.94",
              "totalVATOther"        -> "149.92",
              "declarationItemOther" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Children's Clothes",
                  "quantity"             -> "1",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00",
                  "madeIn"               -> "EU",
                  "euCustomsRelief"      -> true
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity"             -> "1",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43",
                  "madeIn"               -> "US",
                  "euCustomsRelief"      -> false
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity"             -> "1",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49",
                  "madeIn"               -> "EU",
                  "euCustomsRelief"      -> true
                )
              )
            ),
            "liabilityDetails"   -> Json.obj(
              "totalExciseGBP"  -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP"     -> "149.92",
              "grandTotalGBP"   -> "443.40"
            )
          )
        )
      )
    }

    "format the idValue if idType is telephone and generate the correct payload GBNI journey in " in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val jd: JourneyData = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        isUKVatPaid = Some(true),
        isUKVatExcisePaid = Some(false),
        isUKResident = Some(true),
        amendmentCount = Some(0)
      )

      val userInformation: UserInformation = UserInformation(
        "Harry",
        "Potter",
        "telephone",
        "7417532125",
        "",
        "LHR",
        "",
        parseLocalDate("2018-05-31"),
        parseLocalTime("8:2 am")
      )

      val calculatorResponse: CalculatorResponse = CalculatorResponse(
        alcohol = None,
        otherGoods = None,
        tobacco = Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "TOB/A1/CIGRT",
                    "250.10",
                    Some(200),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "200 cigarettes",
                      "Cigarettes",
                      "300.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              ),
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
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "TOB/A1/HAND",
                    "152.05",
                    Some(0),
                    Some(BigDecimal("0.12")),
                    Calculation("26.54", "113.88", "58.49", "198.91"),
                    Metadata(
                      "120g rolling tobacco",
                      "Rolling Tobacco",
                      "200.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
                      Country("US", "United States of America", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.2", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
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
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )

      val dm: JsObject = declarationService.buildPartialDeclarationOrAmendmentMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(
        "journeyData"              -> Json.toJsObject(jd.copy(userInformation = Some(userInformation))),
        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate"       -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr(Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR"))
          ),
          "requestDetail" -> Json.obj(
            "customerReference"  -> Json
              .obj("idType" -> "telephone", "idValue" -> "XPASSID7417532125", "ukResident" -> true),
            "personalDetails"    -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails"     -> Json.obj(),
            "declarationHeader"  -> Json.obj(
              "portOfEntry"           -> "LHR",
              "portOfEntryName"       -> "Heathrow Airport",
              "expectedDateOfArrival" -> "2018-05-31",
              "timeOfEntry"           -> "08:02",
              "messageTypes"          -> Json.obj("messageType" -> "DeclarationCreate"),
              "travellingFrom"        -> "Great Britain",
              "onwardTravelGBNI"      -> "NI",
              "uccRelief"             -> false,
              "ukVATPaid"             -> true,
              "ukExcisePaid"          -> false
            ),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco"     -> "100.54",
              "totalCustomsTobacco"    -> "192.94",
              "totalVATTobacco"        -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity"             -> "200",
                  "goodsValue"           -> "300.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "250.10",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "0.00",
                  "customsGBP"           -> "0.00",
                  "vatGBP"               -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity"             -> "250",
                  "goodsValue"           -> "400.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "304.11",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "74.00",
                  "customsGBP"           -> "79.06",
                  "vatGBP"               -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight"               -> "120.00",
                  "goodsValue"           -> "200.00",
                  "valueCurrency"        -> "USD",
                  "valueCurrencyName"    -> "USA dollars (USD)",
                  "originCountry"        -> "US",
                  "originCountryName"    -> "United States of America",
                  "exchangeRate"         -> "1.20",
                  "exchangeRateDate"     -> "2018-10-29",
                  "goodsValueGBP"        -> "152.05",
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> "26.54",
                  "customsGBP"           -> "113.88",
                  "vatGBP"               -> "58.49"
                )
              )
            ),
            "liabilityDetails"   -> Json.obj(
              "totalExciseGBP"  -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP"     -> "149.92",
              "grandTotalGBP"   -> "443.40"
            )
          )
        )
      )
    }

    "format the idValue to uppercase if idType is telephone and contains lowercase characters in " in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val jd: JourneyData = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        isUKVatPaid = Some(true),
        isUKResident = Some(true)
      )

      val userInformation: UserInformation = UserInformation(
        "Harry",
        "Potter",
        "telephone",
        "74a17b53c2125",
        "",
        "LHR",
        "",
        parseLocalDate("2018-05-31"),
        parseLocalTime("8:2 am")
      )

      val dm: JsObject = declarationService.buildPartialDeclarationOrAmendmentMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      val idValue: String = dm.value
        .apply("simpleDeclarationRequest")
        .\("requestDetail")
        .\("customerReference")
        .\("idValue")
        .asOpt[String]
        .getOrElse("")

      idValue shouldEqual "XPASSID74A17B53C2125"

    }

    "format the idValue to uppercase if idType is not telephone and contains lowercase characters in " in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val jd: JourneyData = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        isUKVatPaid = Some(true),
        isUKResident = Some(true)
      )

      val userInformation: UserInformation = UserInformation(
        "Harry",
        "Potter",
        "other",
        "74a17b53c2125",
        "",
        "LHR",
        "",
        parseLocalDate("2018-05-31"),
        parseLocalTime("8:2 am")
      )

      val dm: JsObject = declarationService.buildPartialDeclarationOrAmendmentMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      val idValue: String = dm.value
        .apply("simpleDeclarationRequest")
        .\("requestDetail")
        .\("customerReference")
        .\("idValue")
        .asOpt[String]
        .getOrElse("")

      idValue shouldEqual "74A17B53C2125"
    }

    "generate correct acknowledgment reference number for amendment" in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val previousDeclarationRequest: PreviousDeclarationRequest =
        PreviousDeclarationRequest("Potter", "XJPR5768524625")

      val calculation: Calculation = Calculation("160.45", "25012.50", "15134.59", "40307.54")

      val liabilityDetails: LiabilityDetails = LiabilityDetails("32.0", "0.0", "126.4", "158.40")

      val productPath: ProductPath = ProductPath("other-goods/adult/adult-footwear")

      val otherGoodsSearchItem: OtherGoodsSearchItem =
        OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))

      val country: Country = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())

      val purchasedProductInstances: List[PurchasedProductInstance] = List(
        PurchasedProductInstance(
          productPath,
          "UnOGll",
          None,
          None,
          Some(country),
          None,
          Some("GBP"),
          Some(500),
          Some(otherGoodsSearchItem),
          Some(false),
          Some(false),
          None,
          Some(false),
          None,
          None
        )
      )

      val declarationResponse: DeclarationResponse =
        DeclarationResponse(calculation, liabilityDetails, purchasedProductInstances, amendmentCount = Some(2))

      val jd: JourneyData = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        isUKVatPaid = Some(true),
        isUKResident = Some(true),
        calculatorResponse = Some(calculatorResponse),
        previousDeclarationRequest = Some(previousDeclarationRequest),
        declarationResponse = Some(declarationResponse)
      )

      val userInformation: UserInformation = UserInformation(
        "Harry",
        "Potter",
        "other",
        "74a17b53c2125",
        "",
        "LHR",
        "",
        parseLocalDate("2018-05-31"),
        parseLocalTime("8:2 am")
      )

      val dm: JsObject = declarationService.buildPartialDeclarationOrAmendmentMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      val acknowledgementReference: String = dm.value
        .apply("simpleDeclarationRequest")
        .\("requestCommon")
        .\("acknowledgementReference")
        .as[String]

      acknowledgementReference shouldEqual "XJPR57685246253"
    }

  }

  "Calling DeclarationService.storeChargeReference" should {

    "store charge reference information" in new Setup {

      override def journeyDataInCache: Option[JourneyData] = None

      val bodyCapture: ArgumentCaptor[JourneyData] = ArgumentCaptor.forClass(classOf[JourneyData])

      when(mockCache.store(any())(any())).thenReturn(Future.successful(JourneyData()))

      val r: JourneyData =
        declarationService.storeChargeReference(JourneyData(), userInformation, "XJPR5768524625").futureValue

      r shouldBe JourneyData(chargeReference = Some("XJPR5768524625"), userInformation = Some(userInformation))

      verify(mockCache, times(1)).store(bodyCapture.capture())(any())

      bodyCapture.getValue shouldBe JourneyData(
        userInformation = Some(userInformation),
        chargeReference = Some("XJPR5768524625")
      )
    }
  }

  "Calling DeclarationService.updateDeclaration" should {

    "return a DeclarationServiceFailureResponse for update if the declaration returns 500" in new Setup
      with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(INTERNAL_SERVER_ERROR, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/update-payment"

      val r: DeclarationServiceResponse = declarationService.updateDeclaration("XJPR5768524625").futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(PaymentNotification("Successful", "XJPR5768524625"))
    }

    "return a DeclarationServiceFailureResponse for update if declaration returns a bad request" in new Setup
      with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(BAD_REQUEST, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/update-payment"

      val r: DeclarationServiceResponse = declarationService.updateDeclaration("XJPR5768524625").futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(PaymentNotification("Successful", "XJPR5768524625"))
    }

    "return a DeclarationServiceFailureResponse for update if the declaration is not found" in new Setup
      with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(NOT_FOUND, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/update-payment"

      val r: DeclarationServiceResponse = declarationService.updateDeclaration("XJPR5768524625").futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(PaymentNotification("Successful", "XJPR5768524625"))
    }

    "return a DeclarationServiceSuccessResponse for update if the declaration returns 202" in new Setup
      with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(ACCEPTED, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/update-payment"

      val r: DeclarationServiceResponse = declarationService.updateDeclaration("XJPR5768524625").futureValue

      r shouldBe DeclarationServiceSuccessResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(PaymentNotification("Successful", "XJPR5768524625"))
    }
  }

  "Calling DeclarationService.retrieveDeclaration" should {

    val expectedJson: JsObject = Json.obj(
      "euCountryCheck"              -> "greatBritain",
      "arrivingNI"                  -> true,
      "isOver17"                    -> true,
      "isUKResident"                -> false,
      "isPrivateTravel"             -> true,
      "amendmentCount"              -> 1,
      "calculation"                 -> Json
        .obj("excise" -> "160.45", "customs" -> "25012.50", "vat" -> "15134.59", "allTax" -> "40307.54"),
      "liabilityDetails"            -> Json.obj(
        "totalExciseGBP"  -> "32.0",
        "totalCustomsGBP" -> "0.0",
        "totalVATGBP"     -> "126.4",
        "grandTotalGBP"   -> "158.40"
      ),
      "oldPurchaseProductInstances" -> Json.arr(
        Json.obj(
          "path"         -> "other-goods/adult/adult-footwear",
          "iid"          -> "UnOGll",
          "country"      -> Json.obj(
            "code"            -> "IN",
            "countryName"     -> "title.india",
            "alphaTwoCode"    -> "IN",
            "isEu"            -> false,
            "isCountry"       -> true,
            "countrySynonyms" -> Json.arr()
          ),
          "currency"     -> "GBP",
          "cost"         -> 500,
          "isVatPaid"    -> false,
          "isCustomPaid" -> false,
          "isUccRelief"  -> false
        )
      )
    )

    val expectedJsonWithDeltaCalcAndAmendState: JsObject = expectedJson ++ Json.obj(
      "amendState"       -> Some("pending-payment"),
      "deltaCalculation" -> Some(Calculation("0.00", "0.00", "0.00", "0.00"))
    )

    val previousDeclarationRequest = PreviousDeclarationRequest("Potter", "someReference")

    "return a DeclarationServiceFailureResponse if the backend returns 400" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(BAD_REQUEST, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/retrieve-declaration"

      val r: DeclarationServiceResponse = declarationService.retrieveDeclaration(previousDeclarationRequest).futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(previousDeclarationRequest)
    }

    "return a DeclarationServiceFailureResponse if the backend returns 500" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(INTERNAL_SERVER_ERROR, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/retrieve-declaration"

      val r: DeclarationServiceResponse = declarationService.retrieveDeclaration(previousDeclarationRequest).futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(previousDeclarationRequest)
    }

    "return a DeclarationServiceFailureResponse if the backend returns NOT FOUND" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(NOT_FOUND, "")

      override def url: String = "http://localhost:9073/bc-passengers-declarations/retrieve-declaration"

      val r: DeclarationServiceResponse = declarationService.retrieveDeclaration(previousDeclarationRequest).futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(previousDeclarationRequest)
    }

    "return a DeclarationServiceSuccessResponse if the backend returns 200" in new Setup with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(OK, expectedJson.toString)

      override def url: String = "http://localhost:9073/bc-passengers-declarations/retrieve-declaration"

      val calculation: Calculation = Calculation("160.45", "25012.50", "15134.59", "40307.54")

      val liabilityDetails: LiabilityDetails = LiabilityDetails("32.0", "0.0", "126.4", "158.40")

      val productPath: ProductPath = ProductPath("other-goods/adult/adult-footwear")

      val country: Country = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())

      val purchasedProductInstances: List[PurchasedProductInstance] = List(
        PurchasedProductInstance(
          productPath,
          "UnOGll",
          None,
          None,
          Some(country),
          None,
          Some("GBP"),
          Some(500),
          None,
          Some(false),
          Some(false),
          None,
          Some(false),
          None,
          Some(true)
        )
      )

      val declarationResponse: DeclarationResponse =
        DeclarationResponse(calculation, liabilityDetails, purchasedProductInstances, amendmentCount = Some(1))

      val jd: JourneyData = JourneyData(
        prevDeclaration = Some(true),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        ageOver17 = Some(true),
        isUKResident = Some(false),
        privateCraft = Some(true),
        previousDeclarationRequest = Some(previousDeclarationRequest),
        declarationResponse = Some(declarationResponse)
      )

      val r: DeclarationServiceResponse = declarationService.retrieveDeclaration(previousDeclarationRequest).futureValue

      r shouldBe DeclarationServiceRetrieveSuccessResponse(jd)

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(previousDeclarationRequest)
    }

    "return a DeclarationServiceSuccessResponse with amend state and delta calculation if the backend returns 200" in new Setup
      with EndpointSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      override def httpResponse: HttpResponse = HttpResponse.apply(OK, expectedJsonWithDeltaCalcAndAmendState.toString)

      override def url: String = "http://localhost:9073/bc-passengers-declarations/retrieve-declaration"

      val calculation: Calculation = Calculation("160.45", "25012.50", "15134.59", "40307.54")

      val liabilityDetails: LiabilityDetails = LiabilityDetails("32.0", "0.0", "126.4", "158.40")

      val productPath: ProductPath = ProductPath("other-goods/adult/adult-footwear")

      val country: Country = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())

      val purchasedProductInstances: List[PurchasedProductInstance] = List(
        PurchasedProductInstance(
          productPath,
          "UnOGll",
          None,
          None,
          Some(country),
          None,
          Some("GBP"),
          Some(500),
          None,
          Some(false),
          Some(false),
          None,
          Some(false),
          None,
          Some(true)
        )
      )

      val declarationResponse: DeclarationResponse =
        DeclarationResponse(calculation, liabilityDetails, purchasedProductInstances, amendmentCount = Some(1))

      val jd: JourneyData = JourneyData(
        prevDeclaration = Some(true),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        ageOver17 = Some(true),
        isUKResident = Some(false),
        privateCraft = Some(true),
        previousDeclarationRequest = Some(previousDeclarationRequest),
        declarationResponse = Some(declarationResponse),
        amendState = Some("pending-payment"),
        deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00"))
      )

      val r: DeclarationServiceResponse = declarationService.retrieveDeclaration(previousDeclarationRequest).futureValue

      r shouldBe DeclarationServiceRetrieveSuccessResponse(jd)

      verify(mockHttpClient, times(1)).post(urlCapture.capture())(any())
      verify(mockRequestBuilder, times(1)).withBody(bodyCapture.capture())(using any(), any(), any())
      verify(mockRequestBuilder, times(1)).execute(using any(), any())

      urlCapture.getValue  shouldBe url"$url"
      bodyCapture.getValue shouldBe Json.toJson(previousDeclarationRequest)
    }
  }
}
