/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import models.JourneyData
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

class UKResidentControllerSpec extends BaseSpec {

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
  "loadUKResidentPage" should {
    "load the page" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some("greatBritain"), Some(true), Some(true), Some(true)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Are you a UK citizen?"
    }

    "loading the page and populate data" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some("greatBritain"),Some(true),Some(true),Some(true),Some(true)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Are you a UK citizen?"
      doc.select("#isUKResident-true").hasAttr("checked") shouldBe true
    }

    "redirect to where-goods-bought page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }
  }

  "postUKResidentPage" should {

    "return a bad request when user selects an invalid value" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), Some(true),Some(true),Some(true),Some(true))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-uk-resident-check").withFormUrlEncodedBody("ukResident" -> "dummy")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Are you a UK citizen?"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#isUKResident]").html() shouldBe "Select yes if you are a UK resident"
      doc.getElementById("isUKResident").getElementsByClass("error-message").html() shouldBe "Select yes if you are a UK resident"
      verify(mockTravelDetailService, times(0)).storeUKResident(any())(any())(any())
    }

  }

}