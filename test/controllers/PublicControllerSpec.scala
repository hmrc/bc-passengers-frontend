/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import org.jsoup.Jsoup
import org.mockito.Mockito.when
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
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
    .overrides(bind[AppConfig].toInstance(MockitoSugar.mock[AppConfig]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[IdentifierAction].to[FakeIdentifierAction])
    .build()

  "Calling /time-out" should {
    "return 200 and start button redirects to where-goods-bought page when amendment feature is off" in {
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn false
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/time-out")).get

      status(result) shouldBe OK
      Jsoup.parse(contentAsString(result)).getElementsByClass("button").attr("href") shouldBe "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought"
    }

    "return 200 and start button redirects to previous-declaration page when amendment feature is on" in {
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn true
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/time-out")).get

      status(result) shouldBe OK
      Jsoup.parse(contentAsString(result)).getElementsByClass("button").attr("href") shouldBe "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration"
    }
  }

}


