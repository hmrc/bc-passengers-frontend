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
import models.JourneyData
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, route, status, _}
import repositories.BCPassengersSessionRepository
import services.TravelDetailsService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class UKResidentControllerSpec extends BaseSpec {

  val mockTravelDetailService: TravelDetailsService = mock(classOf[TravelDetailsService])
  val mockCache: Cache                              = mock(classOf[Cache])
  val mockAppConfig: AppConfig                      = mock(classOf[AppConfig])

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[TravelDetailsService].toInstance(mockTravelDetailService))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance(mockAppConfig))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTravelDetailService)
    reset(mockCache)
    reset(mockAppConfig)
  }
  "loadUKResidentPage" should {
    "load the page" in {
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(Some(JourneyData(Some(false), Some("greatBritain"), Some(true), Some(true), Some(true))))
      )
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Are you a UK resident?"
    }

    "loading the page and populate data" in {
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(JourneyData(Some(false), Some("greatBritain"), Some(true), Some(true), Some(true), Some(true)))
        )
      )
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                        shouldBe "Are you a UK resident?"
      doc.select("#isUKResident-value-yes").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postUKResidentPage" should {

    "redirect to .../goods-brought-into-northern-ireland when non-UK resident travels from GB to NI " in {

      val cachedJourneyData = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            Some(true),
            None,
            None,
            Some(false)
          )
        )
      )

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockTravelDetailService.storeUKResident(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")
          .withFormUrlEncodedBody("isUKResident" -> "false")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland"
      )

      verify(mockTravelDetailService, times(1)).storeUKResident(any())(meq(false))(any())
    }

    "redirect to .../gb-ni-vat-excise-check when UK resident travels from GB to NI" in {

      val cachedJourneyData = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            Some(true),
            None,
            None,
            Some(true)
          )
        )
      )

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockTravelDetailService.storeUKResident(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")
          .withFormUrlEncodedBody("isUKResident" -> "true")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")

      verify(mockTravelDetailService, times(1)).storeUKResident(any())(meq(true))(any())
    }

    "return a bad request when user selects an invalid value" in {

      val cachedJourneyData = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            Some(true),
            Some(true),
            Some(true),
            Some(true)
          )
        )
      )

      when(mockCache.fetch(any())) thenReturn cachedJourneyData

      val response = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")
          .withFormUrlEncodedBody("ukResident" -> "dummy")
      ).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                shouldBe "Are you a UK resident?"
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      doc
        .getElementsByClass("govuk-error-summary")
        .select("a[href=#isUKResident-value-yes]")
        .html()                                        shouldBe "Select yes if you are a UK resident"
      doc
        .getElementById("isUKResident-error")
        .html()                                        shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Select yes if you are a UK resident"
      verify(mockTravelDetailService, times(0)).storeUKResident(any())(any())(any())
    }

  }

}
