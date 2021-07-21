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
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class UKVatPaidControllerSpec extends BaseSpec {

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


  "loadItemUKVatPaidPage" should {
    "load the page" in {
      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-vat-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay UK VAT when buying this item?"
    }
    "load the page and populate uKVatPaid as true" in {
      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(true))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-vat-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay UK VAT when buying this item?"
      doc.select("#isUKVatPaid-true").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-vat-check")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }

    "redirect when not GBNI journey" in {
      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(false))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(false), purchasedProductInstances = List(ppi)))))

      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-vat-check")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postItemUKVatPaidPage" should {
    "redirect to the next step in the add goods journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(false))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-vat-check")
        .withFormUrlEncodedBody("isUKVatPaid" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check")

    }

    "redirect to the excise page in the add alcohol journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(false))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-vat-check")
        .withFormUrlEncodedBody("isUKVatPaid" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check")

    }

    "redirect to the excise page in the add tobacco journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("tobacco/cigarette"), isVatPaid = Some(false))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/brTuNh/gb-ni-vat-check")
        .withFormUrlEncodedBody("isUKVatPaid" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/brTuNh/gb-ni-excise-check")

    }

    "redirect to the ucc relief page in the add other goods journey when successfully submitted and non uk resident" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("other-goods/adult/adult-clothing"), isVatPaid = Some(false))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(false), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/brTuNh/gb-ni-vat-check")
        .withFormUrlEncodedBody("isUKVatPaid" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/brTuNh/gb-ni-exemptions")

    }

    "redirect to the tell us page in the add other goods journey when successfully submitted and uk resident" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("other-goods/adult/adult-clothing"), isVatPaid = Some(false))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), isUKResident = Some(true), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/brTuNh/gb-ni-vat-check")
        .withFormUrlEncodedBody("isUKVatPaid" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "return a bad request when user selects an invalid value" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(false))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-vat-check")
        .withFormUrlEncodedBody("incorrect" -> "true")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay UK VAT when buying this item?"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#isUKVatPaid]").html() shouldBe "Select yes if you paid UK VAT when buying this item"
      doc.getElementById("isUKVatPaid").getElementsByClass("error-message").html() shouldBe "<span class=\"visually-hidden\">Error: </span> Select yes if you paid UK VAT when buying this item"
      verify(mockCache, times(0)).store(any())(any())
    }
  }
}
