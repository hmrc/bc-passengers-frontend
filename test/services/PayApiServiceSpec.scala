/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import connectors.Cache
import controllers.LocalContext
import models._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.http.WsAllMethods
import uk.gov.hmrc.http.HttpResponse
import util.BaseSpec

import scala.concurrent.Future

class PayApiServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
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

  val exampleJson: JsValue = Json.parse(
    s"""{
       |  "pid": "somePID",
       |  "payments": [
       |    {
       |      "chargeReference": "XYPRRVWV52PVDI",
       |      "customerName": "Harry Potter",
       |      "amount": "1362.46",
       |      "taxRegimeDisplay": "PNGR",
       |      "taxType": "PNGR",
       |      "paymentSpecificData": {
       |        "chargeReference": "XYPRRVWV52PVDI",
       |        "vat": "725.03",
       |        "customs": "534.89",
       |        "excise": "102.54"
       |      }
       |    }
       |  ],
       |  "navigation": {
       |    "back": "http://localhost:9083/check-tax-on-goods-you-bring-into-the-uk/user-information",
       |    "reset": "http://localhost:9083/check-tax-on-goods-you-bring-into-the-uk",
       |    "finish": "http://localhost:9083/check-tax-on-goods-you-bring-into-the-uk",
       |    "callback": "http://localhost:9211/payments/notifications/send-card-payments"
       |  }
       |}
    """.stripMargin)

  trait LocalSetup {
    def httpResponse: HttpResponse
    val ppi: PurchasedProductInstance = PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"))
    val localContext: LocalContext = LocalContext(IdentifierRequest(EnhancedFakeRequest("GET", "anything"), "somePID"), "123", Some(JourneyData(purchasedProductInstances = List(ppi))))
    val userInformation: UserInformation = UserInformation("Harry", "Potter","passport", "SX12345", "abc@gmail.com", "LHR", "", LocalDate.parse("2018-11-12"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa")))
    val calculatorResponse: CalculatorResponse = CalculatorResponse(Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "91.23",None,Some(5), Calculation("2.00","0.30","18.70","21.00"),Metadata("5 litres cider", "Cider", "120.00",Currency("USD", "USA dollars (USD)", Some("USD"), Nil), Country("US", "United States of America", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))), Calculation("2.00","0.30","18.70","21.00"))), Calculation("2.00","0.30","18.70","21.00"))),
      Some(Tobacco(List(Band("B",List(Item("TOB/A1/CIGRT","304.11",Some(250),None, Calculation("74.00","79.06","91.43","244.49"),Metadata("250 cigarettes", "Cigarettes", "400.00",Currency("USD", "USA dollars (USD)", Some("USD"), Nil), Country("US", "United States of America", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29"))), Item("TOB/A1/HAND","152.05",Some(0),Some(0.12), Calculation("26.54","113.88","58.49","198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA dollars (USD)", Some("USD"), Nil), Country("US", "United States of America", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))), Calculation("100.54","192.94","149.92","443.40"))), Calculation("100.54","192.94","149.92","443.40"))),
      Some(OtherGoods(List(Band("C",List(Item("OGD/DIGI/TV","1140.42",None,None,
        Calculation("0.00","0.00","0.00","0.00"),Metadata("Televisions", "Televisions","1500.00",Currency("USD", "USA dollars (USD)", Some("USD"), Nil), Country("US", "United States of America", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29"))), Item("OGD/DIGI/TV","1300.00",None,None,
        Calculation("0.00","182.00","296.40","478.40"),Metadata("Televisions", "Televisions","1300.00",Currency("GBP", "British pounds (GBP)", None, Nil), Country("GB", "United Kingdom of Great Britain and Northern Ireland", "GB", isEu = true, Nil), ExchangeRate("1.20", "2018-10-29")))),
        Calculation("0.00","341.65","556.41","898.06"))),
        Calculation("0.00","341.65","556.41","898.06"))
      ),
      Calculation("102.54","534.89","725.03","1362.46"),
      withinFreeAllowance = false,
      limits = Map.empty,
      isAnyItemOverAllowance = true
    )

    val receiptDateTime: DateTime = DateTime.parse("2018-11-12T13:56:01+0000")
    lazy val s: PayApiService = {
      val service = injected[PayApiService]
      when(service.wsAllMethods.POST[JsValue,HttpResponse](any(),any(),any())(any(),any(),any(),any())) thenReturn Future.successful(httpResponse)
      service
    }
  }

  "Calling requestPaymentUrl" should {

    implicit val messages: Messages = injected[MessagesApi].preferred(EnhancedFakeRequest("POST", "/nowhere")(app))

    "return PayApiServiceFailureResponse when client returns 400" in new LocalSetup {

      override lazy val httpResponse: HttpResponse = HttpResponse.apply(BAD_REQUEST,"")

      val r: PayApiServiceResponse = await(s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse)(hc,messages,localContext))
      r shouldBe PayApiServiceFailureResponse
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://localhost:9125/tps-payments-backend/tps-payments"),meq(exampleJson),any())(any(),any(),any(),any())
    }

    "return PayApiServiceFailureResponse when client returns 500" in new LocalSetup {

      override lazy val httpResponse: HttpResponse = HttpResponse.apply(BAD_REQUEST,"")

      val r: PayApiServiceResponse = await(s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse)(hc,messages,localContext))
      r shouldBe PayApiServiceFailureResponse
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://localhost:9125/tps-payments-backend/tps-payments"),meq(exampleJson),any())(any(),any(),any(),any())
    }

    "return a PayApiServiceSuccessResponse with a payment url when http client returns 201" in new LocalSetup {
      val js : JsValue = Json.parse("\"tpsSessionId\"")
      override lazy val httpResponse: HttpResponse = HttpResponse.apply(CREATED, js.toString())

      val r: PayApiServiceResponse = await(s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse)(hc,messages,localContext))
      r shouldBe PayApiServiceSuccessResponse("http://localhost:9124/tps-payments/make-payment/pngr/tpsSessionId")
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://localhost:9125/tps-payments-backend/tps-payments"),meq(exampleJson),any())(any(),any(),any(),any())
    }
  }

  "Calling generateChargeRef" should {

    "always return a valid charge ref" in {

      def isValidChargeReference(chargeReference: String): Boolean = {

        val alphaNums = chargeReference.slice(4, chargeReference.length).toList
        val alphaNumsToConvert = chargeReference.slice(2, chargeReference.length).toList

        val alphaCheckCharacter: Char = {
          val equivalentValues = ('A' to 'Z').zip(33 to 58).toMap
          val charIndexWeights = (3 to 14).zip(List(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)).toMap
          val remainderCheckChars = (0 to 22).zip(List('A','B','C','D','E','F','G','H','X','J','K','L','M','N','Y','P','Q','R','S','T','Z','V','W')).toMap
          val convertedAlphaNums = alphaNumsToConvert.map(alphaNum => equivalentValues.getOrElse(alphaNum, alphaNum.asDigit))
          val remainderFromConversion = convertedAlphaNums.zipWithIndex.map(x => x._1 * charIndexWeights(x._2 + 3)).sum % 23
          remainderCheckChars(remainderFromConversion)
        }

        chargeReference.toList match {
          case 'X' :: checkChar :: 'P' :: 'R' :: tail if checkChar == alphaCheckCharacter && tail == alphaNums => true
          case _ => false
        }
      }


      isValidChargeReference(ChargeReference.generate.value) shouldBe true
    }
  }
}
