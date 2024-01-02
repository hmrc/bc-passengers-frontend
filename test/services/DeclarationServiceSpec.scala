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
import java.time.{LocalDate, LocalDateTime}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.http.WsAllMethods
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import util.{BaseSpec, _}

import scala.concurrent.Future

class DeclarationServiceSpec extends BaseSpec with ScalaFutures {
  // scalastyle:off magic.number
  implicit val messages: MessagesApi = injected[MessagesApi]

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[WsAllMethods].toInstance(MockitoSugar.mock[WsAllMethods]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[AuditConnector].toInstance(MockitoSugar.mock[AuditConnector]))
    .configure(
      "microservice.services.bc-passengers-declarations.host" -> "bc-passengers-declarations.service",
      "microservice.services.bc-passengers-declarations.port" -> "80"
    )
    .build()

  override def beforeEach(): Unit = {
    reset(injected[WsAllMethods])
    reset(injected[Cache])
    reset(injected[AuditConnector])
    super.beforeEach()
  }

  trait LocalSetup {
    def journeyDataInCache: Option[JourneyData]

    lazy val declarationService: DeclarationService = {
      val service = app.injector.instanceOf[DeclarationService]
      val mock    = service.cache
      when(mock.fetch(any())) thenReturn Future.successful(journeyDataInCache)
      when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
      when(mock.storeJourneyData(any())(any())) thenReturn Future.successful(Some(JourneyData()))
      service
    }
  }

  val userInformation: UserInformation = UserInformation(
    "Harry",
    "Potter",
    "passport",
    "SX12345",
    "abc@gmail.com",
    "LHR",
    "",
    LocalDate.parse("2018-05-31"),
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

    "return a DeclarationServiceFailureResponse if the backend returns 400" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(BAD_REQUEST, ""))

      val cid: String         = "fe28db96-d9db-4220-9e12-f2d267267c29"
      val ui: UserInformation = userInformation.copy(identificationType = "passport", identificationNumber = "SX12345")

      val r: DeclarationServiceResponse = declarationService
        .submitDeclaration(ui, calculatorResponse, jd, LocalDateTime.parse("2018-05-31T12:14:08"), cid)
        .futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-declaration"),
        meq(expectedTelephoneValueSendJson),
        meq(
          Seq(
            "X-Correlation-ID" -> cid
          )
        )
      )(any(), any(), any(), any())

      verify(injected[AuditConnector], times(1)).sendExtendedEvent(any())(meq(hc), any())
    }

    "return a DeclarationServiceFailureResponse if the backend returns 500" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, ""))

      val cid: String = "fe28db96-d9db-4220-9e12-f2d267267c29"

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

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-declaration"),
        meq(expectedSendJson),
        meq(
          Seq(
            "X-Correlation-ID" -> cid
          )
        )
      )(any(), any(), any(), any())

      verify(injected[AuditConnector], times(1)).sendExtendedEvent(any())(meq(hc), any())
    }

    "return a DeclarationServiceSuccessResponse if the backend returns 202" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn
        Future.successful(HttpResponse.apply(ACCEPTED, expectedJsObj.toString()))

      val cid: String = "fe28db96-d9db-4220-9e12-f2d267267c29"

      val r: DeclarationServiceResponse = await(
        declarationService
          .submitDeclaration(
            userInformation,
            calculatorResponse,
            jd,
            LocalDateTime.parse("2018-05-31T12:14:08"),
            cid
          )
      )

      r shouldBe DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-declaration"),
        meq(expectedSendJson),
        meq(
          Seq(
            "X-Correlation-ID" -> cid
          )
        )
      )(any(), any(), any(), any())

      verify(injected[AuditConnector], times(1)).sendExtendedEvent(any())(meq(hc), any())
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

    "return a DeclarationServiceFailureResponse if the backend returns 400" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(BAD_REQUEST, ""))

      val cid: String         = "fe28db96-d9db-4220-9e12-f2d267267c29"
      val ui: UserInformation = userInformation.copy(identificationType = "passport", identificationNumber = "SX12345")

      val r: DeclarationServiceResponse = declarationService
        .submitAmendment(ui, calculatorResponse, jd, LocalDateTime.parse("2018-05-31T12:14:08"), cid)
        .futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-amendment"),
        meq(expectedTelephoneValueSendJson),
        meq(
          Seq(
            "X-Correlation-ID" -> cid
          )
        )
      )(any(), any(), any(), any())

      verify(injected[AuditConnector], times(1)).sendExtendedEvent(any())(meq(hc), any())
    }

    "return a DeclarationServiceFailureResponse if the backend returns 500" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, ""))

      val cid: String = "fe28db96-d9db-4220-9e12-f2d267267c29"

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

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-amendment"),
        meq(expectedJsObj),
        meq(
          Seq(
            "X-Correlation-ID" -> cid
          )
        )
      )(any(), any(), any(), any())

      verify(injected[AuditConnector], times(1)).sendExtendedEvent(any())(meq(hc), any())
    }

    "return a DeclarationServiceSuccessResponse if the backend returns 202" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn
        Future.successful(HttpResponse.apply(ACCEPTED, expectedJsObj.toString()))

      val cid: String = "fe28db96-d9db-4220-9e12-f2d267267c29"

      val r: DeclarationServiceResponse = await(
        declarationService
          .submitAmendment(
            userInformation,
            calculatorResponse,
            jd,
            LocalDateTime.parse("2018-05-31T12:14:08"),
            cid
          )
      )

      r shouldBe DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-amendment"),
        meq(expectedJsObj),
        meq(
          Seq(
            "X-Correlation-ID" -> cid
          )
        )
      )(any(), any(), any(), any())

      verify(injected[AuditConnector], times(1)).sendExtendedEvent(any())(meq(hc), any())
    }
  }

  "Calling DeclarationService.buildPartialDeclarationMessage" should {

    "truncate a product description to 40 characters if the product description is too big in the metadata." in new LocalSetup {

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
        LocalDate.parse("2018-05-31"),
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

    "generate the correct payload and set euCountryCheck is nonEuOnly and arrivingNI flag is true" in new LocalSetup {

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

    "generate the correct payload and adhere to the schema when journeyData a calculation with all product categories in" in new LocalSetup {

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

    "generate the correct payload and including ukVATPaid, ukExcisePaid, uccRelief, isMade and eu flags at item level" in new LocalSetup {

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

    "format the idValue if idType is telephone and generate the correct payload GBNI journey in " in new LocalSetup {

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
        LocalDate.parse("2018-05-31"),
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

    "format the idValue to uppercase if idType is telephone and contains lowercase characters in " in new LocalSetup {

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

    "format the idValue to uppercase if idType is not telephone and contains lowercase characters in " in new LocalSetup {

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

    "generate correct acknowledgment reference number for amendment" in new LocalSetup {

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

    "store charge reference information" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(declarationService.storeChargeReference(JourneyData(), userInformation, "XJPR5768524625"))

      verify(declarationService.cache, times(1)).store(
        meq(JourneyData(userInformation = Some(userInformation), chargeReference = Some("XJPR5768524625")))
      )(any())

    }
  }

  "Calling DeclarationService.updateDeclaration" should {

    "return a DeclarationServiceFailureResponse for update if the declaration returns 500" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[PaymentNotification, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, ""))

      val r: DeclarationServiceResponse = declarationService.updateDeclaration("XJPR5768524625").futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[PaymentNotification, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/update-payment"),
        meq(PaymentNotification("Successful", "XJPR5768524625")),
        any()
      )(any(), any(), any(), any())

    }

    "return a DeclarationServiceFailureResponse for update if declaration returns a bad request" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[PaymentNotification, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(BAD_REQUEST, ""))

      val r: DeclarationServiceResponse = declarationService.updateDeclaration("XJPR5768524625").futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[PaymentNotification, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/update-payment"),
        meq(PaymentNotification("Successful", "XJPR5768524625")),
        any()
      )(any(), any(), any(), any())

    }

    "return a DeclarationServiceFailureResponse for update if the declaration is not found" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[PaymentNotification, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(NOT_FOUND, ""))

      val r: DeclarationServiceResponse = declarationService.updateDeclaration("XJPR5768524625").futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[PaymentNotification, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/update-payment"),
        meq(PaymentNotification("Successful", "XJPR5768524625")),
        any()
      )(any(), any(), any(), any())

    }

    "return a DeclarationServiceSuccessResponse for update if the declaration returns 202" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods].POST[PaymentNotification, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(ACCEPTED, ""))

      val r: DeclarationServiceResponse = declarationService.updateDeclaration("XJPR5768524625").futureValue

      r shouldBe DeclarationServiceSuccessResponse

      verify(injected[WsAllMethods], times(1)).POST[PaymentNotification, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/update-payment"),
        meq(PaymentNotification("Successful", "XJPR5768524625")),
        any()
      )(any(), any(), any(), any())

    }
  }

  "Calling DeclarationService.retrieveDeclaration" should {

    val previousDeclarationRequest = PreviousDeclarationRequest("Potter", "someReference")

    "return a DeclarationServiceFailureResponse if the backend returns 400" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods]
          .POST[PreviousDeclarationRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(BAD_REQUEST, ""))

      val r: DeclarationServiceResponse = declarationService.retrieveDeclaration(previousDeclarationRequest).futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[PreviousDeclarationRequest, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/retrieve-declaration"),
        meq(previousDeclarationRequest),
        any()
      )(any(), any(), any(), any())

    }

    "return a DeclarationServiceFailureResponse if the backend returns 500" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods]
          .POST[PreviousDeclarationRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, ""))

      val r: DeclarationServiceResponse = declarationService.retrieveDeclaration(previousDeclarationRequest).futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[PreviousDeclarationRequest, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/retrieve-declaration"),
        meq(previousDeclarationRequest),
        any()
      )(any(), any(), any(), any())

    }

    "return a DeclarationServiceFailureResponse if the backend returns NOT FOUND" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      when(
        injected[WsAllMethods]
          .POST[PreviousDeclarationRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn Future.successful(HttpResponse.apply(NOT_FOUND, ""))

      val r: DeclarationServiceResponse = declarationService.retrieveDeclaration(previousDeclarationRequest).futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[PreviousDeclarationRequest, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/retrieve-declaration"),
        meq(previousDeclarationRequest),
        any()
      )(any(), any(), any(), any())

    }

    "return a DeclarationServiceSuccessResponse if the backend returns 200" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

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
          None
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

      when(
        injected[WsAllMethods]
          .POST[PreviousDeclarationRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn
        Future.successful(HttpResponse.apply(OK, expectedJson.toString()))

      val r: DeclarationServiceResponse = await(declarationService.retrieveDeclaration(previousDeclarationRequest))

      r shouldBe DeclarationServiceRetrieveSuccessResponse(jd)

      verify(injected[WsAllMethods], times(1)).POST[PreviousDeclarationRequest, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/retrieve-declaration"),
        meq(previousDeclarationRequest),
        any()
      )(any(), any(), any(), any())

    }

    "return a DeclarationServiceSuccessResponse with amend state and delta calculation if the backend returns 200" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

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
        ),
        "amendState"                  -> Some("pending-payment"),
        "deltaCalculation"            -> Some(Calculation("0.00", "0.00", "0.00", "0.00"))
      )

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
          None
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

      when(
        injected[WsAllMethods]
          .POST[PreviousDeclarationRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      ) thenReturn
        Future.successful(HttpResponse.apply(OK, expectedJson.toString()))

      val r: DeclarationServiceResponse = await(declarationService.retrieveDeclaration(previousDeclarationRequest))

      r shouldBe DeclarationServiceRetrieveSuccessResponse(jd)

      verify(injected[WsAllMethods], times(1)).POST[PreviousDeclarationRequest, HttpResponse](
        meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/retrieve-declaration"),
        meq(previousDeclarationRequest),
        any()
      )(any(), any(), any(), any())

    }
  }
  // scalastyle:on magic.number
}
