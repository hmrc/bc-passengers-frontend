/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import org.jsoup.Jsoup
import models.{JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq,_}
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

class UKExcisePaidControllerSpec extends BaseSpec {

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
  "loadUKExcisePaidPage" should {
    "load the page" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false), Some("greatBritain"), Some(true),None,None, Some(true)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay both UK VAT and excise duty when buying all of your goods?"
    }

    "loading the page and populate data" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false), Some("greatBritain"),Some(true),None,Some(true),Some(true)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay both UK VAT and excise duty when buying all of your goods?"
      doc.select("#isUKVatExcisePaid-true").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postUKExcisePaidPage" should {

    "redirect to .../goods-brought-into-northern-ireland when UK resident says they have only arrived from GB and going to NI and has answered NO if they paid UK VAT/Excise" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(false), euCountryCheck = Some("greatBritain"),Some(true),None,Some(false), Some(true))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockTravelDetailService.storeUKExcisePaid(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")
                                                                          .withFormUrlEncodedBody("isUKVatExcisePaid" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland")

      verify(mockTravelDetailService, times(1)).storeUKExcisePaid(any())(meq(false))(any())
    }

    "redirect to .../gb-ni-no-need-to-use-service when user has paid UK VAT and Excise and is a UK Resident" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(false), euCountryCheck = Some("greatBritain"),Some(true),None,Some(true), Some(true))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockTravelDetailService.storeUKExcisePaid(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")
        .withFormUrlEncodedBody("isUKVatExcisePaid" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/gb-ni-no-need-to-use-service")

      verify(mockTravelDetailService, times(1)).storeUKExcisePaid(any())(meq(true))(any())
    }

    "return a bad request when user selects an invalid value" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(false), euCountryCheck = Some("greatBritain"), Some(true),None,None,Some(true))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check").withFormUrlEncodedBody("isUKVatExcisePaid" -> "dummy")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay both UK VAT and excise duty when buying all of your goods?"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#isUKVatExcisePaid]").html() shouldBe "Select yes if you paid both UK VAT and excise duty when buying all of your goods"
      doc.getElementById("isUKVatExcisePaid").getElementsByClass("error-message").html() shouldBe "Select yes if you paid both UK VAT and excise duty when buying all of your goods"
      verify(mockTravelDetailService, times(0)).storeUKExcisePaid(any())(any())(any())
    }

  }

  "loadUKExcisePaidItemPage" should {
    "load the page" in {
      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(true))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay UK excise duty when buying this item?"
    }

    "load the page and populate isExcisePaid as true" in {
      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(true), isExcisePaid = Some(true))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay UK excise duty when buying this item?"
      doc.select("#uKExcisePaidItem-true").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postUKExcisePaidItemPage" should {
    "redirect to the next step in the add goods journey when successfully submitted" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(false), isExcisePaid = Some(true))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check")
        .withFormUrlEncodedBody("uKExcisePaidItem" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }

    "return a bad request when user selects an invalid value" in  {

      val ppi = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(false), isExcisePaid = Some(true))
      val jd = JourneyData(euCountryCheck = Some("greatBritain"), arrivingNICheck = Some(true), purchasedProductInstances = List(ppi))
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check")
        .withFormUrlEncodedBody("incorrect" -> "true")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay UK excise duty when buying this item?"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#uKExcisePaidItem]").html() shouldBe "Select yes if you paid UK excise duty when buying this item"
      doc.getElementById("uKExcisePaidItem").getElementsByClass("error-message").html() shouldBe "Select yes if you paid UK excise duty when buying this item"
      verify(mockCache, times(0)).store(any())(any())
    }
  }
}
