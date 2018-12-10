package controllers

import models._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services._
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future
import scala.language.postfixOps


class UserInformationControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[UserInformationService].toInstance(MockitoSugar.mock[UserInformationService]))
    .overrides(bind[PayApiService].toInstance(MockitoSugar.mock[PayApiService]))
    .overrides(bind[DeclarationService].toInstance(MockitoSugar.mock[DeclarationService]))
    .overrides(bind[DateTimeProviderService].toInstance(MockitoSugar.mock[DateTimeProviderService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService], injected[UserInformationService],
      injected[PayApiService], injected[DeclarationService], injected[DateTimeProviderService])
  }

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]
    def payApiResponse: PayApiServiceResponse
    def declarationServiceResponse: DeclarationServiceResponse

    lazy val cr = CalculatorResponse(
      Some(Alcohol(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(Tobacco(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Some(OtherGoods(List(
        Band("A", List(
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc","100.00", Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Calculation("0.00", "0.00", "0.00", "0.00")
    )

    lazy val ui = UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-11-12"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa")))
    
    lazy val dt = DateTime.parse("2018-11-23T06:21:00Z")
    
    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(),any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[UserInformationService].storeUserInformation(any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[PayApiService].requestPaymentUrl(any(),any(), any(), any(), any())(any())) thenReturn Future.successful(payApiResponse)
      when(injected[DeclarationService].submitDeclaration(any(),any(), any(), any())(any())) thenReturn Future.successful(declarationServiceResponse)
      when(injected[DateTimeProviderService].now) thenReturn dt
      rt(app, req)
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/enter-details" should {

    "Display the user information page" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/user-information")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Enter your details"
    }
  }

  "Calling POST /check-tax-on-goods-you-bring-into-the-uk/enter-details" should {

    "Return BAD REQUEST and display the user information form when invalid form input is sent" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when first name is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "123456789012345678901234567890123451234",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when last name is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "123456789012345678901234567890123451234",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when passport number is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "12345678901234567890123456789012345612345",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when place of arrival is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "123456789012345678901234567890123456123456",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when the date is invalid" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(cr)))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "abc",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return INTERNAL_SERVER_ERROR but still store valid user information, when the payment service request fails" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(cr), userInformation = Some(ui)))
      override lazy val payApiResponse = PayApiServiceFailureResponse
      override lazy val declarationServiceResponse = DeclarationServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }

    "Cache the submitted user information and redirect payment url when valid form input is sent and the payment service request is successful" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(cr), userInformation = Some(ui)))
      override lazy val payApiResponse = PayApiServiceSuccessResponse("http://example.com/payment-journey")
      override lazy val declarationServiceResponse = DeclarationServiceSuccessResponse(ChargeReference("XJPR5768524625"))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "123456789",
          "placeOfArrival" -> "Heathrow",
          "dateTimeOfArrival.dateOfArrival.day" -> "23",
          "dateTimeOfArrival.dateOfArrival.month" -> "11",
          "dateTimeOfArrival.dateOfArrival.year" -> "2018",
          "dateTimeOfArrival.timeOfArrival.hour" -> "12",
          "dateTimeOfArrival.timeOfArrival.minute" -> "00",
          "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
        )
      ).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe "http://example.com/payment-journey"

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }
  }


}
