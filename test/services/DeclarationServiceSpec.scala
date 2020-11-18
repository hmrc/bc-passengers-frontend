/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package services

import connectors.Cache
import models._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.http.WsAllMethods
import uk.gov.hmrc.http.HttpResponse
import util.{BaseSpec, _}

import scala.concurrent.Future

class DeclarationServiceSpec extends BaseSpec with ScalaFutures {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[WsAllMethods].toInstance(MockitoSugar.mock[WsAllMethods]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .configure(
      "microservice.services.bc-passengers-declarations.host" -> "bc-passengers-declarations.service",
      "microservice.services.bc-passengers-declarations.port" -> "80"
    )
    .build()

  override def beforeEach(): Unit = {
    reset(app.injector.instanceOf[WsAllMethods])
    reset(app.injector.instanceOf[Cache])
    super.beforeEach()
  }

  val declarationService: DeclarationService = app.injector.instanceOf[DeclarationService]

  val userInformation: UserInformation = UserInformation("Harry", "Potter","passport", "SX12345", "abc@gmail.com", "LHR", "", LocalDate.parse("2018-05-31"),  LocalTime.parse("01:20 pm", DateTimeFormat.forPattern("hh:mm aa")))
  
  val calculatorResponse: CalculatorResponse = CalculatorResponse(
    Some(Alcohol(
      List(
        Band("B", List(
          Item("ALC/A1/CIDER", "91.23", None, Some(5), Calculation("2.00", "0.30", "18.70", "21.00"), Metadata("5 litres cider", "label.alcohol.cider", "120.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
        ),
        Calculation("2.00", "0.30", "18.70", "21.00"))
      ),
      Calculation("2.00", "0.30", "18.70", "21.00"))
    ),
    Some(Tobacco(
      List(
        Band("B", List(
          Item("TOB/A1/CIGRT", "304.11", Some(250), None, Calculation("74.00", "79.06", "91.43", "244.49"), Metadata("250 cigarettes", "label.tobacco.cigarettes", "400.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
          Item("TOB/A1/HAND", "152.05", Some(0), Some(0.12), Calculation("26.54", "113.88", "58.49", "198.91"), Metadata("120g rolling tobacco", "label.tobacco.rolling-tobacco", "200.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
        ),
        Calculation("100.54", "192.94", "149.92", "443.40"))
      ),
      Calculation("100.54", "192.94", "149.92", "443.40"))
    ),
    Some(OtherGoods(
      List(
        Band("C",List(
          Item("OGD/DIGI/TV", "1140.42", None, None, Calculation("0.00", "159.65", "260.01", "419.66"), Metadata("Televisions", "label.other-goods.electronic-devices.televisions", "1500.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
          Item("OGD/DIGI/TV", "1300.00", None, None, Calculation("0.00", "182.00", "296.40", "478.40"), Metadata("Televisions", "label.other-goods.electronic-devices.televisions", "1300.00", Currency("GBP", "British Pound (GBP)", None, Nil), Country("GB", "United Kingdom of Great Britain and Northern Ireland (the)", "GB", isEu = true, Nil), ExchangeRate("1.2", "2018-10-29")))
        ),
        Calculation("0.00", "341.65", "556.41", "898.06"))
      ),
      Calculation("0.00", "341.65", "556.41", "898.06"))
    ),
    Calculation("102.54", "534.89", "725.03", "1362.46"),
    withinFreeAllowance = false,
    limits = Map.empty,
    isAnyItemOverAllowance = true
  )

  val expectedJsObj: JsObject = Json.obj(
    "simpleDeclarationRequest" -> Json.obj(
      "requestCommon" -> Json.obj(
        "receiptDate" -> "2018-05-31T12:14:08Z",
        "acknowledgementReference" -> "XJPR57685246250",
        "requestParameters" -> Json.arr(
          Json.obj(
            "paramName" -> "REGIME",
            "paramValue" -> "PNGR"
          )
        )
      ),
      "requestDetail" -> Json.obj(
        "customerReference" -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
        "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
        "contactDetails" -> Json.obj("emailAddress" -> "abc@gmail.com"),
        "declarationHeader" -> Json.obj("chargeReference" -> "XJPR5768524625", "portOfEntry" -> "LHR", "expectedDateOfArrival" -> "2018-05-31", "timeOfEntry" -> "13:20", "messageTypes" -> Json.obj("messageType" -> "DeclarationCreate"), "travellingFrom" -> "NON_EU Only", "onwardTravelGBNI" -> "GB", "uccRelief" -> false, "ukVATPaid" -> false, "ukExcisePaid" -> false),
        "declarationTobacco" -> Json.obj(
          "totalExciseTobacco" -> "100.54",
          "totalCustomsTobacco" -> "192.94",
          "totalVATTobacco" -> "149.92",
          "declarationItemTobacco" -> Json.arr(
            Json.obj(
              "commodityDescription" -> "Cigarettes",
              "quantity" -> "250",
              "goodsValue" -> "400.00",
              "valueCurrency" -> "USD",
              "originCountry" -> "US",
              "exchangeRate" -> "1.20",
              "exchangeRateDate" -> "2018-10-29",
              "goodsValueGBP" -> "304.11",
              "VATRESClaimed" -> false,
              "exciseGBP" -> "74.00",
              "customsGBP" -> "79.06",
              "vatGBP" -> "91.43"
            ),
            Json.obj(
              "commodityDescription" -> "Rolling tobacco",
              "weight" -> "120.00",
              "goodsValue" -> "200.00",
              "valueCurrency" -> "USD",
              "originCountry" -> "US",
              "exchangeRate" -> "1.20",
              "exchangeRateDate" -> "2018-10-29",
              "goodsValueGBP" -> "152.05",
              "VATRESClaimed" -> false,
              "exciseGBP" -> "26.54",
              "customsGBP" -> "113.88",
              "vatGBP" -> "58.49"
            )
          )
        ),
        "declarationAlcohol" -> Json.obj(
          "totalExciseAlcohol" -> "2.00",
          "totalCustomsAlcohol" -> "0.30",
          "totalVATAlcohol" -> "18.70",
          "declarationItemAlcohol" -> Json.arr(
            Json.obj(
              "commodityDescription" -> "Cider",
              "volume" -> "5",
              "goodsValue" -> "120.00",
              "valueCurrency" -> "USD",
              "originCountry" -> "US",
              "exchangeRate" -> "1.20",
              "exchangeRateDate" -> "2018-10-29",
              "goodsValueGBP" -> "91.23",
              "VATRESClaimed" -> false,
              "exciseGBP" -> "2.00",
              "customsGBP" -> "0.30",
              "vatGBP" -> "18.70"
            )
          )
        ),
        "declarationOther" -> Json.obj(
          "totalExciseOther" -> "0.00",
          "totalCustomsOther" -> "341.65",
          "totalVATOther" -> "556.41",
          "declarationItemOther" -> Json.arr(
            Json.obj(
              "commodityDescription" -> "Television",
              "quantity" -> "1",
              "goodsValue" -> "1500.00",
              "valueCurrency" -> "USD",
              "originCountry" -> "US",
              "exchangeRate" -> "1.20",
              "exchangeRateDate" -> "2018-10-29",
              "goodsValueGBP" -> "1140.42",
              "VATRESClaimed" -> false,
              "exciseGBP" -> "0.00",
              "customsGBP" -> "159.65",
              "vatGBP" -> "260.01"
            ),
            Json.obj(
              "commodityDescription" -> "Television",
              "quantity" -> "1",
              "goodsValue" -> "1300.00",
              "valueCurrency" -> "GBP",
              "originCountry" -> "GB",
              "exchangeRate" -> "1.20",
              "exchangeRateDate" -> "2018-10-29",
              "goodsValueGBP" -> "1300.00",
              "VATRESClaimed" -> false,
              "exciseGBP" -> "0.00",
              "customsGBP" -> "182.00",
              "vatGBP" -> "296.40"
            )
          )
        ),
        "liabilityDetails" -> Json.obj(
          "totalExciseGBP" -> "102.54",
          "totalCustomsGBP" -> "534.89",
          "totalVATGBP" -> "725.03",
          "grandTotalGBP" -> "1362.46"
        )
      )
    )
  )

  val expectedSendJson: JsObject = expectedJsObj.alterFields {
    case ("chargeReference", _) => None
    case ("acknowledgementReference", _) => None
  }


  "Calling DeclarationService.submitDeclaration" should {

    implicit val messages: Messages = injected[MessagesApi].preferred(EnhancedFakeRequest("POST", "/nowhere")(app))

    val jd: JourneyData = JourneyData(
      euCountryCheck = Some("nonEuOnly"),
      arrivingNICheck = Some(false)
    )

    "return a DeclarationServiceFailureResponse if the backend returns 400" in {

      when(injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())) thenReturn Future.successful(HttpResponse(BAD_REQUEST))

      val cid = "fe28db96-d9db-4220-9e12-f2d267267c29"

      val r = declarationService.submitDeclaration(userInformation, calculatorResponse, jd, DateTime.parse("2018-05-31T13:14:08+0100"), cid).futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-declaration"),
        meq(expectedSendJson), meq(Seq(
          "X-Correlation-ID" -> cid
        )))(any(), any(), any(), any())
    }

    "return a DeclarationServiceFailureResponse if the backend returns 500" in {

      when(injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())) thenReturn Future.successful(HttpResponse(INTERNAL_SERVER_ERROR))

      val cid = "fe28db96-d9db-4220-9e12-f2d267267c29"

      val r = declarationService.submitDeclaration(userInformation, calculatorResponse, jd, DateTime.parse("2018-05-31T13:14:08+0100"), cid).futureValue

      r shouldBe DeclarationServiceFailureResponse

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-declaration"),
        meq(expectedSendJson), meq(Seq(
          "X-Correlation-ID" -> cid
        )))(any(), any(), any(), any())
    }


    "return a DeclarationServiceSuccessResponse if the backend returns 202" in {

      when(injected[WsAllMethods].POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any())) thenReturn
        Future.successful(HttpResponse(ACCEPTED, Some(expectedJsObj)))

      val cid = "fe28db96-d9db-4220-9e12-f2d267267c29"

      val r = await(declarationService.submitDeclaration(userInformation, calculatorResponse, jd, DateTime.parse("2018-05-31T13:14:08+0100"), cid))

      r shouldBe DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      verify(injected[WsAllMethods], times(1)).POST[JsObject, HttpResponse](meq("http://bc-passengers-declarations.service:80/bc-passengers-declarations/submit-declaration"),
        meq(expectedSendJson), meq(Seq(
          "X-Correlation-ID" -> cid
        )))(any(), any(), any(), any())
    }
  }


  "Calling DeclarationService.buildPartialDeclarationMessage" should {

    implicit val messages: Messages = injected[MessagesApi].preferred(EnhancedFakeRequest("POST", "/nowhere")(app))

    "truncate a product description to 40 characters if the product description is too big in the metadata." in {

      val jd: JourneyData = JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false))

      val userInformation = UserInformation("Harry", "Potter","passport", "SX12345", "abc@gmail.com", "LHR", "", LocalDate.parse("2018-05-31"),  LocalTime.parse("8:2 am", DateTimeFormat.forPattern("hh:mm aa")))

      val calculatorResponse = CalculatorResponse(
        alcohol = Some(Alcohol(
          List(
            Band("A",
              List(
                Item("ALC/A1/CIDER", "250.10", None, Some(BigDecimal("2.00")), Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("2 litres cider", "Cider but for some reason has a really long product description", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )),
        otherGoods = None,
        tobacco = None,
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )

      val dm = declarationService.buildPartialDeclarationMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails" -> Json.obj("emailAddress" -> "abc@gmail.com"),
            "declarationHeader" -> Json.obj("portOfEntry" -> "LHR", "expectedDateOfArrival" -> "2018-05-31", "timeOfEntry" -> "08:02", "messageTypes" -> Json.obj("messageType" -> "DeclarationCreate"), "travellingFrom" -> "EU Only", "onwardTravelGBNI" -> "GB", "uccRelief" -> false, "ukVATPaid" -> false, "ukExcisePaid" -> false),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol" -> "100.54",
              "totalCustomsAlcohol" -> "192.94",
              "totalVATAlcohol" -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider but for some reason has a really l",
                  "volume" -> "2.00",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

    "generate the correct payload and set euCountryCheck is nonEuOnly and arrivingNI flag is true" in {

      val jd: JourneyData = JourneyData(euCountryCheck = Some("nonEuOnly"),
        arrivingNICheck = Some(true)
      )

      val userInformation = UserInformation("Harry", "Potter","passport", "SX12345", "abc@gmail.com", "LHR", "", LocalDate.parse("2018-05-31"),  LocalTime.parse("01:20 pm", DateTimeFormat.forPattern("hh:mm aa")))

      val calculatorResponse = CalculatorResponse(
        alcohol = Some(Alcohol(
          List(
            Band("A",
              List(
                Item("ALC/A1/CIDER", "250.10", None, Some(BigDecimal("2.00")), Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("2 litres cider", "Cider", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            ),
            Band("B",
              List(
                Item("ALC/A2/BEER", "304.11", None, Some(BigDecimal("3.00")), Calculation("74.00", "79.06", "91.43", "244.49"),Metadata("3 litres beer", "Beer", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
                Item("ALC/A3/WINE", "152.05", None, Some(BigDecimal("4.00")), Calculation("26.54", "113.88", "58.49", "198.91"), Metadata("4 litres wine", "Wine", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("100.54", "192.94", "149.92", "443.40")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )),
        otherGoods = Some(OtherGoods(
          List(
            Band("A",
              List(
                Item("OGD/CLTHS/CHILD", "250.10", None, None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("children's clothes", "Children's Clothes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            ),
            Band("B",
              List(
                Item("OGD/BKS/MISC", "304.11", None, None, Calculation("74.00", "79.06", "91.43", "244.49"),Metadata("books or publications", "Books or Publications", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
                Item("OGD/BKS/MISC", "152.05", None, None, Calculation("26.54", "113.88", "58.49", "198.91"), Metadata("books or publications", "Books or Publications", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("100.54", "192.94", "149.92", "443.40")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )),
        tobacco = Some(Tobacco(
          List(
            Band("A",
              List(
                Item("TOB/A1/CIGRT", "250.10", Some(200), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("200 cigarettes", "Cigarettes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            ),
            Band("B",
              List(
                Item("TOB/A1/CIGRT", "304.11",Some(250),None, Calculation("74.00", "79.06", "91.43", "244.49"),Metadata("250 cigarettes", "Cigarettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
                Item("TOB/A1/HAND", "152.05",Some(0),Some(BigDecimal("0.12")), Calculation("26.54", "113.88", "58.49", "198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("100.54", "192.94", "149.92", "443.40")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )),
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )


      val dm = declarationService.buildPartialDeclarationMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails" -> Json.obj("emailAddress" -> "abc@gmail.com"),
            "declarationHeader" -> Json.obj("portOfEntry" -> "LHR", "expectedDateOfArrival" -> "2018-05-31", "timeOfEntry" -> "13:20", "messageTypes" -> Json.obj("messageType" -> "DeclarationCreate"), "travellingFrom" -> "NON_EU Only", "onwardTravelGBNI" -> "NI", "uccRelief" -> false, "ukVATPaid" -> false, "ukExcisePaid" -> false),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco" -> "100.54",
              "totalCustomsTobacco" -> "192.94",
              "totalVATTobacco" -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity" -> "200",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity" -> "250",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight" -> "120.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol" -> "100.54",
              "totalCustomsAlcohol" -> "192.94",
              "totalVATAlcohol" -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider",
                  "volume" -> "2.00",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Beer",
                  "volume" -> "3.00",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Wine",
                  "volume" -> "4.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "declarationOther" -> Json.obj(
              "totalExciseOther" -> "100.54",
              "totalCustomsOther" -> "192.94",
              "totalVATOther" -> "149.92",
              "declarationItemOther" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Children's Clothes",
                  "quantity" -> "1",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity" -> "1",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity" -> "1",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

    "generate the correct payload and adhere to the schema when journeyData a calculation with all product categories in" in {

      val jd: JourneyData = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        isUKVatPaid = Some(true),
        isUKExcisePaid = Some(true),
        isUKResident = Some(false),
        isUccRelief = Some(true),
      )

      val userInformation = UserInformation("Harry", "Potter", "passport", "SX12345", "abc@gmail.com", "", "LHR", LocalDate.parse("2018-05-31"),  LocalTime.parse("01:20 pm", DateTimeFormat.forPattern("hh:mm aa")))

      val calculatorResponse = CalculatorResponse(
        alcohol = Some(Alcohol(
          List(
            Band("A",
              List(
                Item("ALC/A1/CIDER", "250.10", None, Some(BigDecimal("2.00")), Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("2 litres cider", "Cider", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            ),
            Band("B",
              List(
                Item("ALC/A2/BEER", "304.11", None, Some(BigDecimal("3.00")), Calculation("74.00", "79.06", "91.43", "244.49"),Metadata("3 litres beer", "Beer", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
                Item("ALC/A3/WINE", "152.05", None, Some(BigDecimal("4.00")), Calculation("26.54", "113.88", "58.49", "198.91"), Metadata("4 litres wine", "Wine", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("100.54", "192.94", "149.92", "443.40")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )),
        otherGoods = Some(OtherGoods(
          List(
            Band("A",
              List(
                Item("OGD/CLTHS/CHILD", "250.10", None, None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("children's clothes", "Children's Clothes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            ),
            Band("B",
              List(
                Item("OGD/BKS/MISC", "304.11", None, None, Calculation("74.00", "79.06", "91.43", "244.49"),Metadata("books or publications", "Books or Publications", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
                Item("OGD/BKS/MISC", "152.05", None, None, Calculation("26.54", "113.88", "58.49", "198.91"), Metadata("books or publications", "Books or Publications", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("100.54", "192.94", "149.92", "443.40")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )),
        tobacco = Some(Tobacco(
          List(
            Band("A",
              List(
                Item("TOB/A1/CIGRT", "250.10", Some(200), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("200 cigarettes", "Cigarettes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            ),
            Band("B",
              List(
                Item("TOB/A1/CIGRT", "304.11",Some(250),None, Calculation("74.00", "79.06", "91.43", "244.49"),Metadata("250 cigarettes", "Cigarettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
                Item("TOB/A1/HAND", "152.05",Some(0),Some(BigDecimal("0.12")), Calculation("26.54", "113.88", "58.49", "198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("100.54", "192.94", "149.92", "443.40")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )),
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )


      val dm = declarationService.buildPartialDeclarationMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("idType" -> "passport", "idValue" -> "SX12345", "ukResident" -> false),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails" -> Json.obj("emailAddress" -> "abc@gmail.com"),
            "declarationHeader" -> Json.obj("portOfEntry" -> "LHR", "expectedDateOfArrival" -> "2018-05-31", "timeOfEntry" -> "13:20", "messageTypes" -> Json.obj("messageType" -> "DeclarationCreate"), "travellingFrom" -> "Great Britain", "onwardTravelGBNI" -> "NI", "uccRelief" -> true, "ukVATPaid" -> true, "ukExcisePaid" -> true),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco" -> "100.54",
              "totalCustomsTobacco" -> "192.94",
              "totalVATTobacco" -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity" -> "200",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity" -> "250",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight" -> "120.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol" -> "100.54",
              "totalCustomsAlcohol" -> "192.94",
              "totalVATAlcohol" -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider",
                  "volume" -> "2.00",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Beer",
                  "volume" -> "3.00",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Wine",
                  "volume" -> "4.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "declarationOther" -> Json.obj(
              "totalExciseOther" -> "100.54",
              "totalCustomsOther" -> "192.94",
              "totalVATOther" -> "149.92",
              "declarationItemOther" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Children's Clothes",
                  "quantity" -> "1",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity" -> "1",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity" -> "1",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

    "format the idValue if idType is telephone and generate the correct payload GBNI journey in " in {

      val jd: JourneyData = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        isUKVatPaid = Some(true),
        isUKExcisePaid = Some(false),
        isUKResident = Some(true)
      )

      val userInformation = UserInformation("Harry", "Potter","telephone", "7417532125", "abc@gmail.com", "LHR", "", LocalDate.parse("2018-05-31"),  LocalTime.parse("8:2 am", DateTimeFormat.forPattern("hh:mm aa")))

      val calculatorResponse = CalculatorResponse(
        alcohol = None,
        otherGoods = None,
        tobacco = Some(Tobacco(
          List(
            Band("A",
              List(
                Item("TOB/A1/CIGRT", "250.10", Some(200), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("200 cigarettes", "Cigarettes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            ),
            Band("B",
              List(
                Item("TOB/A1/CIGRT", "304.11",Some(250),None, Calculation("74.00", "79.06", "91.43", "244.49"),Metadata("250 cigarettes", "Cigarettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29"))),
                Item("TOB/A1/HAND", "152.05",Some(0),Some(BigDecimal("0.12")), Calculation("26.54", "113.88", "58.49", "198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.2", "2018-10-29")))
              ),
              Calculation("100.54", "192.94", "149.92", "443.40")
            )
          ),
          Calculation("100.54", "192.94", "149.92", "443.40")
        )),
        calculation = Calculation("102.54", "192.94", "149.92", "443.40"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )

      val dm = declarationService.buildPartialDeclarationMessage(
        userInformation,
        calculatorResponse,
        jd,
        "2018-05-31T12:14:08Z"
      )

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("idType" -> "telephone", "idValue" -> "XPASSID7417532125", "ukResident" -> true),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "contactDetails" -> Json.obj("emailAddress" -> "abc@gmail.com"),
            "declarationHeader" -> Json.obj("portOfEntry" -> "LHR", "expectedDateOfArrival" -> "2018-05-31", "timeOfEntry" -> "08:02", "messageTypes" -> Json.obj("messageType" -> "DeclarationCreate"), "travellingFrom" -> "Great Britain", "onwardTravelGBNI" -> "NI", "uccRelief" -> false, "ukVATPaid" -> true, "ukExcisePaid" -> false),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco" -> "100.54",
              "totalCustomsTobacco" -> "192.94",
              "totalVATTobacco" -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity" -> "200",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity" -> "250",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight" -> "120.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "goodsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

  }
}
