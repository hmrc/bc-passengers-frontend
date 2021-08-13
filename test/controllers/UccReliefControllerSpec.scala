/*
 * Copyright 2021 HM Revenue & Customs
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
import models.{JourneyData, ProductPath, PurchasedProductInstance}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, route, status, _}
import repositories.BCPassengersSessionRepository
import services.TravelDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class UccReliefControllerSpec extends BaseSpec {

  val mockTravelDetailService: TravelDetailsService = MockitoSugar.mock[TravelDetailsService]
  val mockCache: Cache = MockitoSugar.mock[Cache]
  val mockAppConfig: AppConfig = MockitoSugar.mock[AppConfig]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[TravelDetailsService].toInstance(mockTravelDetailService))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance(mockAppConfig))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTravelDetailService, mockCache, mockAppConfig)
  }

  "loadUccReliefItemPage" should {
    "load the page" in {
      val ppi = PurchasedProductInstance(iid = "someIid", path = ProductPath("other-goods/adult/adult-clothing"))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(false), purchasedProductInstances = List(ppi)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/someIid/gb-ni-exemptions")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Tax and duty exemptions for non-UK residents"
    }

    "load the page and populate uccRelief as true" in {
      val ppi = PurchasedProductInstance(iid = "someIid", path = ProductPath("other-goods/adult/adult-clothing"), isUccRelief = Some(true))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(false), purchasedProductInstances = List(ppi)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/someIid/gb-ni-exemptions")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Tax and duty exemptions for non-UK residents"
      doc.select("#value-yes").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/someIid/gb-ni-exemptions")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }

    "redirect when not UK resident" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(isUKResident = Some(true)))))

      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/someIid/gb-ni-exemptions")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postUccReliefItemPage" should {
    "redirect to the next step in the add other goods journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "someIid", path = ProductPath("other-goods/adult/adult-clothing"), isUccRelief = Some(false))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(false), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/someIid/gb-ni-exemptions")
        .withFormUrlEncodedBody("isUccRelief" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "return a bad request when user selects an invalid value" in  {

      val ppi = PurchasedProductInstance(iid = "someIid", path = ProductPath("other-goods/adult/adult-clothing"), isUccRelief = Some(false))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(false), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/someIid/gb-ni-exemptions")
        .withFormUrlEncodedBody("incorrect" -> "true")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Tax and duty exemptions for non-UK residents"
      doc.select("#error-summary-title").text() shouldBe "There is a problem"
      doc.select("a[href=#isUccRelief-error]").html() shouldBe "Select yes if this item is covered by the tax and duty exemptions for non-UK residents"
      doc.getElementById("isUccRelief-error").html() shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Select yes if this item is covered by the tax and duty exemptions for non-UK residents"
      verify(mockTravelDetailService, times(0)).storeUccRelief(any())(any())(any())
    }
  }

}
