/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

class PublicControllerSpec extends BaseSpec {

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .overrides(bind[AppConfig].toInstance(mock(classOf[AppConfig])))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  "Calling /time-out" should {
    "return 200 and start button redirects to where-goods-bought page when amendment feature is off" in {
      when(injected[AppConfig].isAmendmentsEnabled).thenReturn(false)
      val result = route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/time-out")).get

      status(result) shouldBe OK
      Jsoup
        .parse(contentAsString(result))
        .body()
        .html()
        .contains("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }

    "return 200 and start button redirects to previous-declaration page when amendment feature is on" in {
      when(injected[AppConfig].isAmendmentsEnabled).thenReturn(true)
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
