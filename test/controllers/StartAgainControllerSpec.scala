/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.Cache
import models.JourneyData
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.*
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class StartAgainControllerSpec extends BaseSpec {

  private val mockCache: Cache = mock(classOf[Cache])

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  private val journeyData = JourneyData(
    euCountryCheck = Some("nonEuOnly"),
    arrivingNICheck = Some(false),
    bringingOverAllowance = Some(true),
    ageOver17 = Some(true),
    privateCraft = Some(false)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCache)
    when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData)))
  }

  "show" should {
    "display the start again confirmation page" in {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/start-again-are-you-sure")
      ).get

      status(result) shouldBe OK
      Jsoup.parse(contentAsString(result)).getElementsByTag("h1").text() shouldBe
        "Are you sure you want to start again?"
    }
  }

  "submit" should {
    "start a new session when the user selects Yes" in {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/start-again-are-you-sure")
          .withFormUrlEncodedBody("startAgain" -> "true")
      ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }

    "return to the calculation page when the user selects No" in {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/start-again-are-you-sure")
          .withFormUrlEncodedBody("startAgain" -> "false")
      ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tax-due")
    }

    "display an error when the user does not make a selection" in {
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/start-again-are-you-sure")
          .withFormUrlEncodedBody()
      ).get

      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.select(".govuk-error-summary__list a").text() shouldBe
        "Select yes if you want to delete this declaration and start again"
      document.select(".govuk-error-summary__list a").attr("href") shouldBe "#startAgain-yes"
    }
  }
}
