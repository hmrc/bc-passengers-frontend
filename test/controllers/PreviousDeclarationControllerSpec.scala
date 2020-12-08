/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import models.JourneyData
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
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
import controllers.actions.{FakeIdentifierAction, IdentifierAction}

import scala.concurrent.Future

class PreviousDeclarationControllerSpec extends BaseSpec {

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
    when(injected[AppConfig].declareGoodsUrl) thenReturn "https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods"
    when(injected[AppConfig].isAmendmentsEnabled) thenReturn true
  }

  "loadPreviousDeclarationPage" should {
    "load the page when feature is on" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false)))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Have you previously made a declaration for your journey?"
    }

    "redirect to start page when the amendments feature is off" in {
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn false
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")).get
      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }

    "loading the page and populate data from keyStore" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Have you previously made a declaration for your journey?"
      doc.select("#prevDeclaration-false").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")).get
      status(result) shouldBe OK
      redirectLocation(result) shouldBe None
    }

  }

  "postPreviousDeclarationPage" should {

    "redirect to .../where-goods-bought when user says they have not made any previous declaration" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(false))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockTravelDetailService.storePrevDeclaration(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
        .withFormUrlEncodedBody("prevDeclaration" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")

      verify(mockTravelDetailService, times(1)).storePrevDeclaration(any())(meq(false))(any())
    }

    "return a bad request when user selects an invalid value in Previous Declaration page" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = None)))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn false

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration").withFormUrlEncodedBody("prevDeclaration" -> "dummy")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Have you previously made a declaration for your journey?"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#prevDeclaration]").html() shouldBe "Select yes if you have previously made a declaration for your journey"
      doc.getElementById("prevDeclaration").getElementsByClass("error-message").html() shouldBe "Select yes if you have previously made a declaration for your journey"
      verify(mockTravelDetailService, times(0)).storePrevDeclaration(any())(any())(any())
    }

    "return error summary box on the page head when trying to submit a blank form" in {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = None)))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")).get
      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#prevDeclaration]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#prevDeclaration]").html()).get shouldBe "Select yes if you have previously made a declaration for your journey"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"
    }

  }

}
