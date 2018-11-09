package controllers

import util.BaseSpec
import models._
import org.joda.time.{DateTime, LocalDate}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services._
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.collection.JavaConversions._
import play.api.test.Helpers.{route => rt, _}


class UserInformationControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[UserInformationService].toInstance(MockitoSugar.mock[UserInformationService]))
    .overrides(bind[PayApiService].toInstance(MockitoSugar.mock[PayApiService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService], injected[UserInformationService])
  }

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]
    def payApiResponse: PayApiServiceResponse

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

    lazy val ui = UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-11-12"), "TOBEREMOVED", DateTime.parse("2018-11-12"))


    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(),any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[UserInformationService].storeUserInformation(any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[PayApiService].requestPaymentUrl(any(),any(), any(), any(), any())(any(),any())) thenReturn Future.successful(payApiResponse)

      rt(app, req)
    }
  }

  "Calling GET /bc-passengers-frontend/enter-details" should {

    "Display the user information page" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/user-information")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Enter your details"
    }
  }

  "Calling POST /bc-passengers-frontend/enter-details" should {

    "Return BAD REQUEST and display the user information form when invalid form input is sent" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "",
          "lastName" -> "Potter",
          "passportNumber" -> "801375812",
          "placeOfArrival" -> "Newcastle airport",
          "dateOfArrival.day" -> "01",
          "dateOfArrival.month" -> "02",
          "dateOfArrival.year" -> "2018"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when first name is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "123456789012345678901234567890123456",
          "lastName" -> "Potter",
          "passportNumber" -> "801375812",
          "placeOfArrival" -> "Newcastle airport",
          "dateOfArrival.day" -> "01",
          "dateOfArrival.month" -> "02",
          "dateOfArrival.year" -> "2018"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when last name is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "123456789012345678901234567890123456",
          "passportNumber" -> "801375812",
          "placeOfArrival" -> "Newcastle airport",
          "dateOfArrival.day" -> "01",
          "dateOfArrival.month" -> "02",
          "dateOfArrival.year" -> "2018"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when passport number is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "12345678901234567890123456789012345678901",
          "placeOfArrival" -> "Newcastle airport",
          "dateOfArrival.day" -> "01",
          "dateOfArrival.month" -> "02",
          "dateOfArrival.year" -> "2018"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when place of arrival is too long" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "12345678",
          "placeOfArrival" -> "12345678901234567890123456789012345678901",
          "dateOfArrival.day" -> "01",
          "dateOfArrival.month" -> "02",
          "dateOfArrival.year" -> "2018"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return BAD REQUEST and display the user information when the date is invalid" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false)))
      override lazy val payApiResponse = PayApiServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "12345678",
          "placeOfArrival" -> "Newcastle airport",
          "dateOfArrival.day" -> "01",
          "dateOfArrival.month" -> "abc",
          "dateOfArrival.year" -> "2018"
        )
      ).get

      status(response) shouldBe BAD_REQUEST
    }

    "Return INTERNAL_SERVER_ERROR but still store valid user information, when the payment service request fails" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(cr), userInformation = Some(ui)))
      override lazy val payApiResponse = PayApiServiceFailureResponse

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "801375812",
          "placeOfArrival" -> "Newcastle airport",
          "dateOfArrival.day" -> "01",
          "dateOfArrival.month" -> "02",
          "dateOfArrival.year" -> "2018"
        )
      ).get

      status(response) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }

    "Cache the submitted user information and redirect payment url when valid form input is sent and the payment service request is successful" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("both"), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(cr), userInformation = Some(ui)))
      override lazy val payApiResponse = PayApiServiceSuccessResponse("http://example.com/payment-journey")

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/user-information")
        .withFormUrlEncodedBody(
          "firstName" -> "Harry",
          "lastName" -> "Potter",
          "passportNumber" -> "801375812",
          "placeOfArrival" -> "Newcastle airport",
          "dateOfArrival.day" -> "01",
          "dateOfArrival.month" -> "02",
          "dateOfArrival.year" -> "2018"
        )
      ).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe "http://example.com/payment-journey"

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }
  }


}
