package services

import connectors.Cache
import models._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import services.http.WsAllMethods
import uk.gov.hmrc.http.HttpResponse
import util.BaseSpec

import scala.concurrent.Future

class PayApiServiceSpec extends BaseSpec {

  override lazy val app = GuiceApplicationBuilder()
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

  val exampleChargeRef = ChargeReference("XYPRRVWV52PVDI")

  val exampleJson = Json.parse(
    s"""{
       |    "chargeReference": "XYPRRVWV52PVDI",
       |    "taxToPayInPence": 9700000,
       |    "dateOfArrival": "2018-11-12T12:20:00",
       |    "passengerName": "Harry Potter",
       |    "placeOfArrival": "Heathrow",
       |    "returnUrl": "http://localhost:9514/feedback/passengers",
       |    "returnUrlFailed": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/calculation",
       |    "returnUrlCancelled": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/calculation",
       |    "backUrl": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/user-information",
       |    "items": [
       |        {
       |            "name": "5 litres cider",
       |            "costInGbp": "21.00"
       |        },
       |        {
       |            "name": "250 cigarettes",
       |            "costInGbp": "244.49"
       |        },
       |        {
       |            "name": "120g rolling tobacco",
       |            "costInGbp": "198.91"
       |        },
       |        {
       |            "name": "Televisions",
       |            "costInGbp": "478.40"
       |        }
       |    ]
       |}
    """.stripMargin)

  val exampleJsonForBstArrival = Json.parse(
    s"""{
       |    "chargeReference": "XYPRRVWV52PVDI",
       |    "taxToPayInPence": 9700000,
       |    "dateOfArrival": "2018-07-12T12:20:00",
       |    "passengerName": "Harry Potter",
       |    "placeOfArrival": "Heathrow",
       |    "returnUrl": "http://localhost:9514/feedback/passengers",
       |    "returnUrlFailed": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/calculation",
       |    "returnUrlCancelled": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/calculation",
       |    "backUrl": "http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/user-information",
       |    "items": [
       |        {
       |            "name": "5 litres cider",
       |            "costInGbp": "21.00"
       |        },
       |        {
       |            "name": "250 cigarettes",
       |            "costInGbp": "244.49"
       |        },
       |        {
       |            "name": "120g rolling tobacco",
       |            "costInGbp": "198.91"
       |        },
       |        {
       |            "name": "Televisions",
       |            "costInGbp": "478.40"
       |        }
       |    ]
       |}
    """.stripMargin)

  trait LocalSetup {
    def httpResponse: HttpResponse
    val userInformation = UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-11-12"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa")))
    val calculatorResponse = CalculatorResponse(Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "91.23",None,Some(5), Calculation("2.00","0.30","18.70","21.00"),Metadata("5 litres cider", "Cider", "120.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))), Calculation("2.00","0.30","18.70","21.00"))), Calculation("2.00","0.30","18.70","21.00"))),
      Some(Tobacco(List(Band("B",List(Item("TOB/A1/CIGRT","304.11",Some(250),None, Calculation("74.00","79.06","91.43","244.49"),Metadata("250 cigarettes", "Cigarettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29"))), Item("TOB/A1/HAND","152.05",Some(0),Some(0.12), Calculation("26.54","113.88","58.49","198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))), Calculation("100.54","192.94","149.92","443.40"))), Calculation("100.54","192.94","149.92","443.40"))),
      Some(OtherGoods(List(Band("C",List(Item("OGD/DIGI/TV","1140.42",None,None,
        Calculation("0.00","0.00","0.00","0.00"),Metadata("Televisions", "Televisions","1500.00",Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29"))), Item("OGD/DIGI/TV","1300.00",None,None,
        Calculation("0.00","182.00","296.40","478.40"),Metadata("Televisions", "Televisions","1300.00",Currency("GBP", "British Pound (GBP)", None, Nil), Country("GB", "United Kingdom of Great Britain and Northern Ireland (the)", "GB", isEu = true, Nil), ExchangeRate("1.20", "2018-10-29")))),
        Calculation("0.00","341.65","556.41","898.06"))),
        Calculation("0.00","341.65","556.41","898.06"))
      ),
      Calculation("102.54","534.89","725.03","1362.46"),
      withinFreeAllowance = false,
      limits = Map.empty
    )

    val receiptDateTime = DateTime.parse("2018-11-12T13:56:01+0000")
    lazy val s = {
      val service = injected[PayApiService]
      when(service.wsAllMethods.POST[JsValue,HttpResponse](any(),any(),any())(any(),any(),any(),any())) thenReturn Future.successful(httpResponse)
      service
    }
  }

  "Calling requestPaymentUrl" should {

    "return PayApiServiceFailureResponse when client returns 400" in new LocalSetup {

      override lazy val httpResponse = HttpResponse(BAD_REQUEST)

      val r = await(s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, receiptDateTime))
      r shouldBe PayApiServiceFailureResponse
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),meq(exampleJson),any())(any(),any(),any(),any())
    }

    "return PayApiServiceFailureResponse when client returns 500" in new LocalSetup {

      override lazy val httpResponse = HttpResponse(BAD_REQUEST)

      val r = await(s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, receiptDateTime))
      r shouldBe PayApiServiceFailureResponse
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),meq(exampleJson),any())(any(),any(),any(),any())
    }

    "return a PayApiServiceSuccessResponse with a payment url when http client returns 201" in new LocalSetup {

      override lazy val httpResponse = HttpResponse(CREATED, Some(
        Json.obj("nextUrl" -> "https://example.com")
      ))

      val r = await(s.requestPaymentUrl(exampleChargeRef, userInformation, calculatorResponse, 9700000, receiptDateTime))
      r shouldBe PayApiServiceSuccessResponse("https://example.com")
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),meq(exampleJson),any())(any(),any(),any(),any())
    }

    "return a PayApiServiceSuccessResponse with a payment url when http client returns 201 (when in BST)" in new LocalSetup {

      val uiWithBstArrival = userInformation.copy(dateOfArrival = LocalDate.parse("2018-7-12"), timeOfArrival = LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa")))

      override lazy val httpResponse = HttpResponse(CREATED, Some(
        Json.obj("nextUrl" -> "https://example.com")
      ))

      val r = await(s.requestPaymentUrl(exampleChargeRef, uiWithBstArrival, calculatorResponse, 9700000, receiptDateTime))
      r shouldBe PayApiServiceSuccessResponse("https://example.com")
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://pay-api.service:80/pay-api/pngr/pngr/journey/start"),meq(exampleJsonForBstArrival),any())(any(),any(),any(),any())
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
