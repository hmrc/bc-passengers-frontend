/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import models.{JourneyData, PreviousDeclarationDetails}
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

class DeclarationRetrievalControllerSpec extends BaseSpec {

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
    when(injected[AppConfig].declareGoodsUrl) thenReturn "https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods"
    when(injected[AppConfig].isAmendmentsEnabled) thenReturn true
  }

  "loadDeclarationRetrievalPage" should {

    "load the page when amendments feature is on" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(true)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.getElementsByTag("h1").text() shouldBe "Add goods to your previous declaration"
    }

    "redirect to start page when the amendments feature is off" in {
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn false
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }

    "redirect to .../check-tax-on-goods-you-bring-into-the-uk when page is accessed after user says they have not made any previous declaration" in {
      val journeyData = JourneyData(prevDeclaration = Some(false), previousDeclarationDetails = None)
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn true
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData)))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }

    "load the page with values from keyStore" in {
      val previousDeclarationDetails = PreviousDeclarationDetails("Smith","1234","XAPR1234567890")
      val journeyData = JourneyData(prevDeclaration = Some(true), previousDeclarationDetails = Some(previousDeclarationDetails))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData)))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.getElementsByTag("h1").text() shouldBe "Add goods to your previous declaration"
      doc.getElementById("lastName").`val`() shouldBe "Smith"
      doc.getElementById("identificationNumber").`val`() shouldBe "1234"
      doc.getElementById("referenceNumber").`val`() shouldBe "XAPR1234567890"
    }

    "load the page with empty form when no data in keyStore" in {
      val journeyData = JourneyData(prevDeclaration = Some(true), previousDeclarationDetails = None)
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData)))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.getElementsByTag("h1").text() shouldBe "Add goods to your previous declaration"
      doc.getElementById("lastName").`val`() shouldBe ""
      doc.getElementById("identificationNumber").`val`() shouldBe ""
      doc.getElementById("referenceNumber").`val`() shouldBe ""
    }
  }

  "postDeclarationRetrievalPage" should {
    "redirect to .../where-goods-bought when user says they have not made any previous declaration" in  {
      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(false))))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockTravelDetailService.storePrevDeclarationDetails(any())(any())(any())) thenReturn cachedJourneyData
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")

    }

    "return a bad request when user does not fill in required fields" in  {
      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(true))))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
        .withFormUrlEncodedBody("lastName" -> "","identificationNumber" -> "","referenceNumber" -> "")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Add goods to your previous declaration"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#lastName]").html() shouldBe "Enter your last name"
      doc.getElementById("errors").select("a[href=#identificationNumber]").html() shouldBe "Enter the identification number you used for your previous declaration"
      doc.getElementById("errors").select("a[href=#referenceNumber]").html() shouldBe "Enter your reference number"
      doc.getElementById("lastName").parent().getElementsByClass("error-message").html() shouldBe "Enter your last name"
      doc.getElementById("identificationNumber").parent().getElementsByClass("error-message").html() shouldBe "Enter the identification number you used for your previous declaration"
      doc.getElementById("referenceNumber").parent().getElementsByClass("error-message").html() shouldBe "Enter your reference number"

    }

    "return a bad request when user enters invalid fields" in  {
      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(true))))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
        .withFormUrlEncodedBody("" +
          "lastName" -> "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
          "identificationNumber" -> "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
          "referenceNumber" -> "XXXX0123456789")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Add goods to your previous declaration"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#lastName]").html() shouldBe "Last name must be 35 characters or less"
      doc.getElementById("errors").select("a[href=#identificationNumber]").html() shouldBe "Identification number must be 40 characters or less"
      doc.getElementById("errors").select("a[href=#referenceNumber]").html() shouldBe "Enter your reference number in the correct format"
      doc.getElementById("lastName").parent().getElementsByClass("error-message").html() shouldBe "Last name must be 35 characters or less"
      doc.getElementById("identificationNumber").parent().getElementsByClass("error-message").html() shouldBe "Identification number must be 40 characters or less"
      doc.getElementById("referenceNumber").parent().getElementsByClass("error-message").html() shouldBe "Enter your reference number in the correct format"
      verify(mockTravelDetailService, times(0)).storePrevDeclaration(any())(any())(any())

    }

    "redirect to where goods both following a successful POST" in  {
      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(true))))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
      when(mockTravelDetailService.storePrevDeclarationDetails(any())(any())(any())) thenReturn cachedJourneyData
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
        .withFormUrlEncodedBody("" +
          "lastName" -> "Smith",
          "identificationNumber" -> "12345",
          "referenceNumber" -> "XXPR0123456789")).get
      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
      verify(mockTravelDetailService, times(1)).storePrevDeclarationDetails(any())(any())(any())
    }
  }

}