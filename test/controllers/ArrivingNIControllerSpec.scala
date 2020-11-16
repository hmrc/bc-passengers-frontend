/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.JourneyData
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, route, status}
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.TravelDetailsService
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class ArrivingNIControllerSpec extends BaseSpec {

  val mockTravelDetailService: TravelDetailsService = MockitoSugar.mock[TravelDetailsService]
  val mockCache: Cache = MockitoSugar.mock[Cache]
  val mockAppConfig: AppConfig = MockitoSugar.mock[AppConfig]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[TravelDetailsService].toInstance(mockTravelDetailService))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance(mockAppConfig))
    .overrides(bind[IdentifierAction].to[FakeIdentifierAction])
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTravelDetailService, mockCache, mockAppConfig)
  }
  "loadArrivingNIPage" should {
    "load the page" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false), Some("nonEuOnly")))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Is your final destination Northern Ireland?"
    }

    "loading the page and populate data from keyStore" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false), Some("nonEuOnly"),Some(true)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Is your final destination Northern Ireland?"
      doc.select("#arrivingNI-true").hasAttr("checked") shouldBe true
    }

    "redirect to the start page where journey data is missing" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postArrivingNIPage" should {

    "redirect to .../goods-brought-into-great-britain-iom when user says they have arrived from EU and final destination is not NI" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
      when(mockTravelDetailService.storeArrivingNI(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom")


      verify(mockTravelDetailService, times(1)).storeArrivingNI(any())(meq(false))(any())
    }

    "redirect to .../goods-bought-into-northern-ireland-inside-EU  when user says they have arrived from EU when isVatResJourneyEnabled is false" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn false
      when(mockTravelDetailService.storeArrivingNI(any())(any())(any())) thenReturn cachedJourneyData



      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-into-northern-ireland-inside-eu")


      verify(mockTravelDetailService, times(1)).storeArrivingNI(any())(meq(true))(any())
    }

    "return a bad request when user selects an invalid value in Arriving NI page" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("None"))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn false

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "dummy")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Is your final destination Northern Ireland?"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#arrivingNI]").html() shouldBe "Select yes if your final destination is Northern Ireland"
      doc.getElementById("arrivingNI").getElementsByClass("error-message").html() shouldBe "Select yes if your final destination is Northern Ireland"
      verify(mockTravelDetailService, times(0)).storeArrivingNI(any())(any())(any())
    }

    "redirect to .../gb-ni-vat-check when user says they have arrived from GB and final destination is NI" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockTravelDetailService.storeArrivingNI(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/arriving-ni").withFormUrlEncodedBody("arrivingNI" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-check")


      verify(mockTravelDetailService, times(1)).storeArrivingNI(any())(meq(true))(any())
    }

  }

}
