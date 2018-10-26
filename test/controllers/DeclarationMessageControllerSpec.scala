package controllers

import models.JourneyData
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services.{DeclarationMessageService, PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future

class DeclarationMessageControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[DeclarationMessageService].toInstance(MockitoSugar.mock[DeclarationMessageService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  val controller: DeclarationMessageController = app.injector.instanceOf[DeclarationMessageController]

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[DeclarationMessageService])
  }

  trait LocalSetup {
    def requiredJourneyData: JourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(Some(requiredJourneyData))
      when(injected[DeclarationMessageService].declarationMessage(any(), any(), any() ,any())) thenReturn Json.obj()

      rt(app, req)
    }
  }

  "Calling GET .../declaration" should {

    "return a built declaration message as json" in new LocalSetup {

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/declaration")).get

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      headers(result).get("X-Correlation-ID") should not be None
      headers(result)(defaultAwaitTimeout)("X-Correlation-ID") should fullyMatch regex "[0-9a-f]{8}[0-9a-f]{4}[0-9a-f]{4}[0-9a-f]{4}[0-9a-f]{12}"
    }
  }
}
