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
import org.mockito.Mockito.{mock, reset, when}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class GoodsCheckYourAnswersControllerSpec extends BaseSpec {

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

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach(): Unit = {
    reset(injected[Cache])
    when(injected[Cache].fetch(any())).thenReturn(Future.successful(Some(journeyData)))
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
  }

  "POST /check-your-item (DDCE-9157: threshold check moved here, toggle ON)" should {
    "redirect to the 'You have added X item' dashboard when the alcohol is under the threshold" in {
      val wineItem =
        PurchasedProductInstance(ProductPath("alcohol/wine"), "iid1", weightOrVolume = Some(BigDecimal(20)))
      when(injected[Cache].fetch(any()))
        .thenReturn(Future.successful(Some(journeyData.copy(purchasedProductInstances = List(wineItem)))))

      val result = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/wine/iid1")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }

    "redirect to the 'you cannot use this service' page when the alcohol is over the threshold" in {
      val wineItem =
        PurchasedProductInstance(ProductPath("alcohol/wine"), "iid1", weightOrVolume = Some(BigDecimal(95)))
      when(injected[Cache].fetch(any()))
        .thenReturn(Future.successful(Some(journeyData.copy(purchasedProductInstances = List(wineItem)))))
      when(injected[Cache].store(any())(any())).thenReturn(Future.successful(journeyData))

      val result = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/wine/iid1")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/wine/upper-limits/volume"
      )
    }
  }

  "POST /check-your-item (toggle OFF)" should {
    "continue to the existing item completion route (legacy no-op)" in {
      val offApp = GuiceApplicationBuilder()
        .configure("features.wine-still-or-sparkling" -> false)
        .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
        .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
        .overrides(bind[Cache].toInstance(mock(classOf[Cache])))
        .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
        .build()
      when(offApp.injector.instanceOf[Cache].fetch(any())).thenReturn(Future.successful(Some(journeyData)))

      val result = route(
        offApp,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/alcohol/beer/iid0")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/next-step")
    }
  }
}
