/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import connectors.Cache
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import scala.language.postfixOps

class UnauthorisedControllerSpec extends BaseSpec {


  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[IdentifierAction].to[FakeIdentifierAction])
    .build()

  "Unauthorised Controller" must {

    "return OK" in {

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/unauthorised-user")).get

      status(result) shouldBe OK

    }
  }
}
