package controllers

import models.JourneyData
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
import services.{PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future

class DashboardControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService])
  }

  val controller: DashboardController = app.injector.instanceOf[DashboardController]

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(),any(),any())(any(),any())) thenReturn Future.successful(JourneyData())
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)

      rt(app, req)
    }
  }

  "Calling GET /bc-passengers-frontend/dashboard" should {
    "start a new session if any travel details are missing" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Egypt"), ageOver17 = Some(true), None))

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/dashboard")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/new-session")

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }
  }


  "respond with 200 and display the page is all travel details exist" in new LocalSetup {

    override lazy val cachedJourneyData = Some(JourneyData(country = Some("Egypt"), ageOver17 = Some(true), privateCraft = Some(false)))

    val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/dashboard").withFormUrlEncodedBody("firstName" -> "Harry", "lastName" -> "Potter", "passportNumber" -> "801375812", "placeOfArrival" -> "Newcastle airport")).get

    status(response) shouldBe OK

    val content = contentAsString(response)
    val doc = Jsoup.parse(content)

    doc.getElementsByTag("h1").text() shouldBe "Tell us about your purchases"
    Option(doc.getElementById("start-again")) should not be None

  }
}
