package controllers

import models.JourneyData
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.TravelDetailsService
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future

class DashboardControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[TravelDetailsService])
  }

  val controller: DashboardController = app.injector.instanceOf[DashboardController]

  "Calling GET /bc-passengers-frontend/dashboard" should {
    "start a new session if any travel details are missing" in {

      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful(Some(JourneyData(country = Some("Egypt"), ageOver17 = Some(true), None)))

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/dashboard")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/new-session")

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }
  }


  "respond with 200 and display the page is all travel details exist" in {

    when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful(Some(JourneyData(country = Some("Egypt"), ageOver17 = Some(true), privateCraft = Some(false))))

    val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/dashboard")).get

    status(response) shouldBe OK

    val content = contentAsString(response)
    val doc = Jsoup.parse(content)

    doc.getElementsByTag("h1").text() shouldBe "Tell us about your purchases"
    Option(doc.getElementById("start-again")) should not be None

  }
}
