package services

import models.ChargeReference
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => meq, _}
import org.scalatest.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import services.http.WsAllMethods
import uk.gov.hmrc.http.HttpResponse
import util.BaseSpec
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PayApiServiceSpec extends BaseSpec {

  override lazy val app = GuiceApplicationBuilder()
    .overrides(bind[WsAllMethods].toInstance(MockitoSugar.mock[WsAllMethods]))
    .overrides(bind[LocalSessionCache].toInstance(MockitoSugar.mock[LocalSessionCache]))
    .configure(
      "microservice.services.pay-api.host" -> "pay-api.service",
      "microservice.services.pay-api.port" -> "80"
    )
    .build()

  override def beforeEach(): Unit = {
    reset(app.injector.instanceOf[WsAllMethods])
    reset(app.injector.instanceOf[LocalSessionCache])
    super.beforeEach()
  }

  val exampleChargeRef = ChargeReference.generate

  val exampleJson = Json.parse(
    s"""{
      |  "taxType": "pngr",
      |  "reference": "${exampleChargeRef.value}",
      |  "description": "Customs Declaration Payment",
      |  "amountInPence": 9700000,
      |  "extras" : {},
      |  "backUrl": "http://localhost:9008/back",
      |  "returnUrl": "http://localhost:9008/return",
      |  "returnUrlFailure": "http://localhost:9008/return-fail",
      |  "returnUrlCancel": "http://localhost:9008/return-cancel"
      |}
    """.stripMargin)

  trait LocalSetup {
    def httpResponse: HttpResponse

    lazy val s = {
      val service = injected[PayApiService]
      when(service.wsAllMethods.POST[JsValue,HttpResponse](any(),any(),any())(any(),any(),any(),any())) thenReturn Future.successful(httpResponse)
      service
    }
  }

  "Calling requestPaymentUrl" should {

    "return PayApiServiceFailureResponse when client returns 400" in new LocalSetup {

      override lazy val httpResponse = HttpResponse(BAD_REQUEST)

      val r = await(s.requestPaymentUrl(exampleChargeRef, 9700000))
      r shouldBe PayApiServiceFailureResponse
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://pay-api.service:80/pay-api/payment"),meq(exampleJson),any())(any(),any(),any(),any())
    }

    "return PayApiServiceFailureResponse when client returns 500" in new LocalSetup {

      override lazy val httpResponse = HttpResponse(BAD_REQUEST)

      val r = await(s.requestPaymentUrl(exampleChargeRef, 9700000))
      r shouldBe PayApiServiceFailureResponse
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://pay-api.service:80/pay-api/payment"),meq(exampleJson),any())(any(),any(),any(),any())
    }

    "return a PayApiServiceSuccessResponse with a payment url when http client returns 201" in new LocalSetup {

      override lazy val httpResponse = HttpResponse(CREATED, Some(
        Json.obj("links" -> Json.obj("nextUrl" -> "https://example.com"))
      ))

      val r = await(s.requestPaymentUrl(exampleChargeRef, 9700000))
      r shouldBe PayApiServiceSuccessResponse("https://example.com")
      verify(s.wsAllMethods, times(1)).POST[JsValue,HttpResponse](meq("http://pay-api.service:80/pay-api/payment"),meq(exampleJson),any())(any(),any(),any(),any())
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
