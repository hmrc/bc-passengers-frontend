/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.AppConfig
import connectors.Cache
import org.jsoup.Jsoup
import org.mockito.Mockito.when
import org.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.status
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository

class PublicControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[AppConfig].toInstance(MockitoSugar.mock[AppConfig]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  "Calling /time-out" should {
    "return 200 and start button redirects to where-goods-bought page when amendment feature is off" in {
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn false
      val result = route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/time-out")).get

      status(result) shouldBe OK
      Jsoup
        .parse(contentAsString(result))
        .body()
        .html()
        .contains("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }

    "return 200 and start button redirects to previous-declaration page when amendment feature is on" in {
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn true
      val result = route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/time-out")).get

      status(result) shouldBe OK
      Jsoup
        .parse(contentAsString(result))
        .body()
        .html()
        .contains("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
    }
  }

}
