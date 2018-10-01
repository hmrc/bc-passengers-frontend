package controllers

import util.BaseSpec
import models.JourneyData
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import services.{PurchasedProductService, TravelDetailsService, UserInformationService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.collection.JavaConversions._
import play.api.test.Helpers.{route => rt, _}


class DonePageControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[UserInformationService].toInstance(MockitoSugar.mock[UserInformationService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService], injected[UserInformationService])
  }

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(),any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[UserInformationService].storeUserInformation(any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)

      rt(app, req)
    }
  }

  "Calling GET /bc-passengers-frontend/enter-details" should {

    "Display the user information page" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Uganda"), ageOver17 = Some(true), privateCraft = Some(false)))

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/user-information")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Enter your details"
    }
  }

  "Calling POST /bc-passengers-frontend/enter-details" should {

    "Return BAD REQUEST and display the user information form when invalid form input is sent" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Uganda"), ageOver17 = Some(true), privateCraft = Some(false)))

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

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Uganda"), ageOver17 = Some(true), privateCraft = Some(false)))

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

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Uganda"), ageOver17 = Some(true), privateCraft = Some(false)))

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

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Uganda"), ageOver17 = Some(true), privateCraft = Some(false)))

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

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Uganda"), ageOver17 = Some(true), privateCraft = Some(false)))

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

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Uganda"), ageOver17 = Some(true), privateCraft = Some(false)))

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

    "Redirect to ... and cache the submitted user information when valid form input is sent" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Uganda"), ageOver17 = Some(true), privateCraft = Some(false)))

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

      status(response) shouldBe OK

      verify(injected[UserInformationService], times(1)).storeUserInformation(any(), any())(any(), any())
    }
  }


}
