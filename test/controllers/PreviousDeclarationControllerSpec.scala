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
import services.PreviousDeclarationService
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class PreviousDeclarationControllerSpec extends BaseSpec {

  val mockPreviousDeclarationService: PreviousDeclarationService = MockitoSugar.mock[PreviousDeclarationService]
  val mockCache: Cache = MockitoSugar.mock[Cache]
  val mockAppConfig: AppConfig = MockitoSugar.mock[AppConfig]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[PreviousDeclarationService].toInstance(mockPreviousDeclarationService))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance(mockAppConfig))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPreviousDeclarationService, mockCache, mockAppConfig)
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

      doc.getElementsByTag("h1").text() shouldBe "What do you want to do?"
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

      doc.getElementsByTag("h1").text() shouldBe "What do you want to do?"
      doc.select("#prevDeclaration-no").hasAttr("checked") shouldBe true
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
      when(mockPreviousDeclarationService.storePrevDeclaration(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
        .withFormUrlEncodedBody("prevDeclaration" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")

      verify(mockPreviousDeclarationService, times(1)).storePrevDeclaration(any())(meq(false))(any())
    }

    "redirect to .../declaration-retrieval when user selects they have made a previous declaration" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(false))))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockPreviousDeclarationService.storePrevDeclaration(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
        .withFormUrlEncodedBody("prevDeclaration" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")

      verify(mockPreviousDeclarationService, times(1)).storePrevDeclaration(any())(meq(true))(any())
    }

    "return a bad request when user selects an invalid value in Previous Declaration page" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = None)))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn false

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration").withFormUrlEncodedBody("prevDeclaration" -> "dummy")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "What do you want to do?"
      doc.select("#error-summary-title").text() shouldBe "There is a problem"
      doc.select("a[href=#prevDeclaration-error]").html() shouldBe "Select if you want to check tax on goods and declare them or add goods to a previous declaration"
      doc.getElementById("prevDeclaration-error").getElementsByClass("govuk-visually-hidden").html() shouldBe "Error:"
      verify(mockPreviousDeclarationService, times(0)).storePrevDeclaration(any())(any())(any())
    }

    "return error summary box on the page head when trying to submit a blank form" in {

      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = None)))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")).get
      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("prevDeclaration-error").select("a[href=#prevDeclaration]")).isEmpty shouldBe false
      Option(doc.select("a[href=#prevDeclaration-error]").html()).get shouldBe "Select if you want to check tax on goods and declare them or add goods to a previous declaration"
      Option(doc.select("h2").hasClass("govuk-error-summary__title")).get shouldBe true
      Option(doc.getElementById("error-summary-title").select("h2").html()).get shouldBe "There is a problem"
    }

  }
}
