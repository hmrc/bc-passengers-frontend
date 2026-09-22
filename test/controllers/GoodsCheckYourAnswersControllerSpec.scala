/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.Cache
import models.{JourneyData, ProductPath, PurchasedProductInstance}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, reset, verify, when}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter, WineStillOrSparklingFeature}

import scala.concurrent.Future

class GoodsCheckYourAnswersControllerSpec extends BaseSpec with WineStillOrSparklingFeature {

  private val mockCache: Cache = mock(classOf[Cache])

  private val item        = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0")
  private val journeyData = JourneyData(
    prevDeclaration = Some(false),
    euCountryCheck = Some("nonEuOnly"),
    arrivingNICheck = Some(true),
    bringingOverAllowance = Some(true),
    ageOver17 = Some(true),
    privateCraft = Some(false),
    purchasedProductInstances = List(item)
  )

  private def journeyDataWith(instance: PurchasedProductInstance): JourneyData =
    journeyData.copy(purchasedProductInstances = List(instance))

  private def appBuilder: GuiceApplicationBuilder = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])

  override given app: Application = appBuilder.build()

  private lazy val appWithWineStillOrSparklingEnabled: Application =
    appBuilder.configure(wineStillOrSparklingKey -> true).build()

  private lazy val appWithWineStillOrSparklingDisabled: Application =
    appBuilder.configure(wineStillOrSparklingKey -> false).build()

  override def beforeEach(): Unit = {
    reset(mockCache)
    when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData)))
    when(mockCache.store(any())(any())).thenReturn(Future.successful(JourneyData()))
  }

  "GET /check-your-item" should {
    "display the item CYA page" in {
      val result = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/beer/iid0")
      ).get

      status(result)                                           shouldBe OK
      Jsoup.parse(contentAsString(result)).select("h1").text() shouldBe "Check your answers"
    }

    "display the item CYA page resolving the currency when the item has one" in {
      val itemWithCurrency = PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", currency = Some("GBP"))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyDataWith(itemWithCurrency))))

      val result = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/beer/iid0")
      ).get

      status(result)                                           shouldBe OK
      Jsoup.parse(contentAsString(result)).select("h1").text() shouldBe "Check your answers"
    }

    "redirect to the dashboard when the requested item cannot be found" in {
      val result = route(
        app,
        enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/beer/missing")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")
    }
  }

  "POST /check-your-item" should {
    "continue to the item completion route when the item is within the limit" in {
      val result =
        route(
          app,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/beer/iid0")
        ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }

    "continue to the item completion route when wine-still-or-sparkling is disabled even if the item is over the limit" in {
      val overLimitWine =
        PurchasedProductInstance(ProductPath("alcohol/wine"), "iid0", weightOrVolume = Some(BigDecimal(95)))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyDataWith(overLimitWine))))

      val result =
        route(
          appWithWineStillOrSparklingDisabled,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/wine/iid0")
        ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }

    "continue to the item completion route when wine-still-or-sparkling is enabled and the merged wine option is within the 90 litre limit" in {
      val withinLimitWine =
        PurchasedProductInstance(ProductPath("alcohol/wine"), "iid0", weightOrVolume = Some(BigDecimal(90)))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyDataWith(withinLimitWine))))

      val result =
        route(
          appWithWineStillOrSparklingEnabled,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/wine/iid0")
        ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }

    "remove the item and redirect to the over-limit page when wine-still-or-sparkling is ON and the merged wine option is over the 90 litre limit" in {
      val overLimitWine =
        PurchasedProductInstance(ProductPath("alcohol/wine"), "iid0", weightOrVolume = Some(BigDecimal(95)))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyDataWith(overLimitWine))))

      val result =
        route(
          appWithWineStillOrSparklingEnabled,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/wine/iid0")
        ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/wine/upper-limits/volume"
      )
    }

    "remove the item and redirect to the over-limit page when wine-still-or-sparkling is OFF and the merged wine option is over the 90 litre limit" in {
      val overLimitWine =
        PurchasedProductInstance(ProductPath("alcohol/wine"), "iid0", weightOrVolume = Some(BigDecimal(95)))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyDataWith(overLimitWine))))

      val result =
        route(
          appWithWineStillOrSparklingDisabled,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/wine/iid0")
        ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step"
      )
    }

    "continue to the item completion route when wine-still-or-sparkling is ON and the item is not alcohol" in {
      val tobacco = PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid0")
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyDataWith(tobacco))))

      val result =
        route(
          appWithWineStillOrSparklingEnabled,
          enhancedFakeRequest(
            "POST",
            "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/tobacco/cigarettes/iid0"
          )
        ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }
  }

  "GET /check-your-item/change-type" should {
    "start a replacement journey without changing the existing item" in {
      val result = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/beer/iid0/change-type"
        )
      ).get

      status(result)                                                         shouldBe SEE_OTHER
      redirectLocation(result)                                               shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/add-an-item")
      session(result).get(ControllerHelpers.itemBeingReplacedSessionKey)     shouldBe Some("iid0")
      session(result).get(ControllerHelpers.itemReplacementCyaUrlSessionKey) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/beer/iid0"
      )
      verify(injected[Cache], never()).store(any())(any())
    }
  }

  "GET /check-your-item/change-product" should {
    "start selection for the item's product category" in {
      val result = route(
        app,
        enhancedFakeRequest(
          "GET",
          "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/beer/iid0/change-product"
        )
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-new-goods/alcohol")
    }
  }
}
