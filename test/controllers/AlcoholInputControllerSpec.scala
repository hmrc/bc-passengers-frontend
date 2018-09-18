package controllers

import models.JourneyData
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services.TravelDetailsService
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future

class AlcoholInputControllerSpec extends BaseSpec {

  val requiredJourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[TravelDetailsService])
  }

  trait LocalSetup {

    def requiredJourneyData: JourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))
    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn {
        Future.successful(cachedJourneyData)
      }

      rt(app, req)
    }
  }

  val controller: AlcoholInputController = app.injector.instanceOf[AlcoholInputController]

  "Calling GET /products/alcohol/.../start" should {

    "redirect to the volume input page" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol/cider/start")).get


      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should fullyMatch regex """^/bc-passengers-frontend/products/alcohol/cider/volume/[a-zA-Z0-9]{6}$""".r
    }
  }
}
