package controllers

import connectors.Cache
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.status
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository

class PublicControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  "Calling /time-out" should {
    "return 200" in {
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/time-out")).get

      status(result) shouldBe OK
    }
  }

}


