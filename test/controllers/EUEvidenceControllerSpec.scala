/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
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

class EUEvidenceControllerSpec extends BaseSpec {

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


  "loadEUEvidenceItemPage" should {
    "load the page" in {
      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), originCountry = Some(Country("ES0", "Spain", "ES", isEu=true, isCountry=true, Nil)), hasEvidence = Some(true))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(ppi)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/eu-evidence-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Do you have evidence this item was originally produced or made in the EU?"
    }
    "load the page and populate hasEvidence as true" in {
      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), originCountry = Some(Country("ES0", "Spain", "ES", isEu=true, isCountry=true, Nil)), hasEvidence = Some(true))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(ppi)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/eu-evidence-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Do you have evidence this item was originally produced or made in the EU?"
      doc.select("#eUEvidenceItem-true").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/eu-evidence-check")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }

    "redirect when not EUGB journey" in {
      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), originCountry = Some(Country("ES0", "Spain", "ES", isEu=true, isCountry=true, Nil)), hasEvidence = Some(false))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi)))))

      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/eu-evidence-check")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postEUEvidenceItemPage" should {
    "redirect to the next step in the add goods journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), originCountry = Some(Country("ES0", "Spain", "ES", isEu=true, isCountry=true, Nil)), hasEvidence = Some(false))
      val jd = JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/eu-evidence-check")
        .withFormUrlEncodedBody("eUEvidenceItem" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "redirect to the tell us page in the add alcohol journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), originCountry = Some(Country("ES0", "Spain", "ES", isEu=true, isCountry=true, Nil)), hasEvidence = Some(false))
      val jd = JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/eu-evidence-check")
        .withFormUrlEncodedBody("eUEvidenceItem" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "redirect to the tell us page in the add tobacco journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("tobacco/cigarette"), originCountry = Some(Country("ES0", "Spain", "ES", isEu=true, isCountry=true, Nil)), hasEvidence = Some(false))
      val jd = JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/tobacco/cigarettes/brTuNh/eu-evidence-check")
        .withFormUrlEncodedBody("eUEvidenceItem" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "redirect to the tell us page in the add other goods journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("other-goods/adult/adult-clothing"), originCountry = Some(Country("ES0", "Spain", "ES", isEu=true, isCountry=true, Nil)), hasEvidence = Some(false))
      val jd = JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/adult/adult-clothing/brTuNh/eu-evidence-check")
        .withFormUrlEncodedBody("eUEvidenceItem" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")

    }

    "return a bad request when user selects an invalid value" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), originCountry = Some(Country("ES0", "Spain", "ES", isEu=true, isCountry=true, Nil)), hasEvidence = Some(false))
      val jd = JourneyData(euCountryCheck = Some("euOnly"), arrivingNICheck = Some(false), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/eu-evidence-check")
        .withFormUrlEncodedBody("incorrect" -> "true")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Do you have evidence this item was originally produced or made in the EU?"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#eUEvidenceItem]").html() shouldBe "Select yes if you have evidence this item was originally produced or made in the EU"
      doc.getElementById("eUEvidenceItem").getElementsByClass("error-message").html() shouldBe "<span class=\"visually-hidden\">Error: </span> Select yes if you have evidence this item was originally produced or made in the EU"
      verify(mockCache, times(0)).store(any())(any())
    }
  }
}
