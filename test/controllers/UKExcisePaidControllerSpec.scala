/*
 * Copyright 2023 HM Revenue & Customs
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
import org.jsoup.Jsoup
import models.{JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.TravelDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class UKExcisePaidControllerSpec extends BaseSpec {

  val mockTravelDetailService: TravelDetailsService = MockitoSugar.mock[TravelDetailsService]
  val mockCache: Cache                              = MockitoSugar.mock[Cache]
  val mockAppConfig: AppConfig                      = MockitoSugar.mock[AppConfig]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
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

  "loadUKExcisePaidPage" should {
    "load the page" in {
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(Some(JourneyData(Some(false), Some("greatBritain"), Some(true), None, None, Some(true))))
      )
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc
        .getElementsByTag("h1")
        .text() shouldBe "Did you pay both UK VAT and excise duty when buying all of your goods?"
    }

    "loading the page and populate data" in {
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(JourneyData(Some(false), Some("greatBritain"), Some(true), None, Some(true), Some(true)))
        )
      )
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc
        .getElementsByTag("h1")
        .text()                                                     shouldBe "Did you pay both UK VAT and excise duty when buying all of your goods?"
      doc.select("#isUKVatExcisePaid-value-yes").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postUKExcisePaidPage" should {

    "redirect to .../goods-brought-into-northern-ireland when UK resident says they have only arrived from GB and going to NI " +
      "and has answered NO if they paid UK VAT/Excise" in {

        val cachedJourneyData = Future.successful(
          Some(
            JourneyData(
              prevDeclaration = Some(false),
              euCountryCheck = Some("greatBritain"),
              Some(true),
              None,
              Some(false),
              Some(true)
            )
          )
        )

        when(mockCache.fetch(any())) thenReturn cachedJourneyData
        when(mockTravelDetailService.storeUKExcisePaid(any())(any())(any())) thenReturn cachedJourneyData

        val response = route(
          app,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")
            .withFormUrlEncodedBody("isUKVatExcisePaid" -> "false")
        ).get

        status(response)           shouldBe SEE_OTHER
        redirectLocation(response) shouldBe Some(
          "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland"
        )

        verify(mockTravelDetailService, times(1)).storeUKExcisePaid(any())(meq(false))(any())
      }

    "redirect to .../gb-ni-no-need-to-use-service when user has paid UK VAT and Excise and is a UK Resident" in {

      val cachedJourneyData = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(false),
            euCountryCheck = Some("greatBritain"),
            Some(true),
            None,
            Some(true),
            Some(true)
          )
        )
      )

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockTravelDetailService.storeUKExcisePaid(any())(any())(any())) thenReturn cachedJourneyData

      val response = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")
          .withFormUrlEncodedBody("isUKVatExcisePaid" -> "true")
      ).get

      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/gb-ni-no-need-to-use-service")

      verify(mockTravelDetailService, times(1)).storeUKExcisePaid(any())(meq(true))(any())
    }

    "return a bad request when user selects an invalid value" in {

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

      val response = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-vat-excise-check")
          .withFormUrlEncodedBody("isUKVatExcisePaid" -> "dummy")
      ).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc     = Jsoup.parse(content)

      doc
        .getElementsByTag("h1")
        .text()                                        shouldBe "Did you pay both UK VAT and excise duty when buying all of your goods?"
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      doc
        .select("a[href=#isUKVatExcisePaid-value-yes]")
        .html()                                        shouldBe "Select yes if you paid both UK VAT and excise duty when buying all of your goods"
      doc
        .getElementById("isUKVatExcisePaid-error")
        .html()                                        shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Select yes if you paid both UK VAT and excise duty when buying all of your goods"
      verify(mockTravelDetailService, times(0)).storeUKExcisePaid(any())(any())(any())
    }

  }

  "loadUKExcisePaidItemPage" should {
    "load the page" in {
      val ppi                    = PurchasedProductInstance(iid = "brTuNh", path = ProductPath("alcohol/beer"), isVatPaid = Some(true))
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(
            JourneyData(
              euCountryCheck = Some("greatBritain"),
              arrivingNICheck = Some(true),
              purchasedProductInstances = List(ppi)
            )
          )
        )
      )
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check"
        )
      ).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Did you pay UK excise duty when buying this item?"
    }

    "load the page and populate isExcisePaid as true" in {
      val ppi                    = PurchasedProductInstance(
        iid = "brTuNh",
        path = ProductPath("alcohol/beer"),
        isVatPaid = Some(true),
        isExcisePaid = Some(true)
      )
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(
            JourneyData(
              euCountryCheck = Some("greatBritain"),
              arrivingNICheck = Some(true),
              purchasedProductInstances = List(ppi)
            )
          )
        )
      )
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check"
        )
      ).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                            shouldBe "Did you pay UK excise duty when buying this item?"
      doc.select("#uKExcisePaidItem-value-yes").hasAttr("checked") shouldBe true
    }

    "redirect to start page when journey data is empty" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check"
        )
      ).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }
  }

  "postUKExcisePaidItemPage" should {
    def test(iid: String): Unit =
      s"redirect to the next step in the add goods journey when successfully submitted with PurchasedProductInstance iid $iid" in {
        val ppi: PurchasedProductInstance                  = PurchasedProductInstance(
          iid = iid,
          path = ProductPath("alcohol/beer"),
          isVatPaid = Some(false),
          isExcisePaid = Some(true)
        )
        val jd: JourneyData                                = JourneyData(
          euCountryCheck = Some("greatBritain"),
          arrivingNICheck = Some(true),
          purchasedProductInstances = List(ppi)
        )
        val cachedJourneyData: Future[Option[JourneyData]] = Future.successful(Some(jd))
        when(mockCache.fetch(any())) thenReturn cachedJourneyData
        when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
        val response                                       = route(
          app,
          enhancedFakeRequest(
            "POST",
            "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check"
          )
            .withFormUrlEncodedBody("uKExcisePaidItem" -> "true")
        ).get

        status(response)           shouldBe SEE_OTHER
        redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
      }

    Seq("brTuNh", "brXuNh").foreach(test)

    "return a bad request when user selects an invalid value" in {

      val ppi               = PurchasedProductInstance(
        iid = "brTuNh",
        path = ProductPath("alcohol/beer"),
        isVatPaid = Some(false),
        isExcisePaid = Some(true)
      )
      val jd                = JourneyData(
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        purchasedProductInstances = List(ppi)
      )
      val cachedJourneyData = Future.successful(Some(jd))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.store(any())(any())) thenReturn Future.successful(jd)
      val response          = route(
        app,
        enhancedFakeRequest(
          "POST",
          "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/alcohol/beer/brTuNh/gb-ni-excise-check"
        )
          .withFormUrlEncodedBody("incorrect" -> "true")
      ).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                shouldBe "Did you pay UK excise duty when buying this item?"
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      doc
        .select("a[href=#uKExcisePaidItem-value-yes]")
        .html()                                        shouldBe "Select yes if you paid UK excise duty when buying this item"
      doc
        .getElementById("uKExcisePaidItem-error")
        .html()                                        shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Select yes if you paid UK excise duty when buying this item"
      verify(mockCache, times(0)).store(any())(any())
    }
  }
}
