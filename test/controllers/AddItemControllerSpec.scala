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
import org.scalatest.Inspectors.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class AddItemControllerSpec extends BaseSpec {

  private val journeyData = JourneyData(
    prevDeclaration = Some(false),
    euCountryCheck = Some("nonEuOnly"),
    arrivingNICheck = Some(true),
    bringingOverAllowance = Some(true),
    ageOver17 = Some(true),
    privateCraft = Some(false)
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

  "GET /add-an-item" should {
    "display the goods type page" in {
      val result = route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/add-an-item")).get

      status(result)                                                                        shouldBe OK
      Jsoup.parse(contentAsString(result)).select("h1").text()                              shouldBe "Which type of goods do you want to add?"
      Jsoup.parse(contentAsString(result)).select("input[name=goodsType][checked]").isEmpty shouldBe true
    }

    forAll(
      Seq(
        ("vaping-products/vape", false, "vaping-products"),
        ("other-ni-goods/vaping-products-liquid", true, "other-ni-goods")
      )
    ) { case (path, arrivingNI, goodsType) =>
      s"preselect $goodsType when changing the type of $path for arrivingNI=$arrivingNI" in {
        val item   = PurchasedProductInstance(ProductPath(path), "iid0")
        when(injected[Cache].fetch(any())).thenReturn(
          Future.successful(
            Some(
              journeyData.copy(arrivingNICheck = Some(arrivingNI), purchasedProductInstances = List(item))
            )
          )
        )
        val result = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/add-an-item")
            .withSession(ControllerHelpers.itemBeingReplacedSessionKey -> item.iid)
        ).get

        status(result)                                                                              shouldBe OK
        Jsoup.parse(contentAsString(result)).select("input[name=goodsType][checked]").attr("value") shouldBe goodsType
        verify(injected[Cache], never()).store(any())(any())
      }
    }
  }

  "POST /add-an-item" should {
    "retain the existing vaping item when its goods type is unchanged" in {
      val item   = PurchasedProductInstance(ProductPath("vaping-products/vape"), "iid0", cost = Some(BigDecimal(30)))
      val cyaUrl = "/check-tax-on-goods-you-bring-into-the-uk/check-your-item/vaping-products/vape/iid0"
      when(injected[Cache].fetch(any())).thenReturn(
        Future.successful(
          Some(
            journeyData.copy(arrivingNICheck = Some(false), purchasedProductInstances = List(item))
          )
        )
      )
      val result = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/add-an-item")
          .withSession(
            ControllerHelpers.itemBeingReplacedSessionKey     -> item.iid,
            ControllerHelpers.itemReplacementCyaUrlSessionKey -> cyaUrl
          )
          .withFormUrlEncodedBody("goodsType" -> "vaping-products")
      ).get

      status(result)                                                         shouldBe SEE_OTHER
      redirectLocation(result)                                               shouldBe Some(cyaUrl)
      session(result).get(ControllerHelpers.itemBeingReplacedSessionKey)     shouldBe None
      session(result).get(ControllerHelpers.itemReplacementCyaUrlSessionKey) shouldBe None
      verify(injected[Cache], never()).store(any())(any())
    }

    forAll(
      Seq(
        "alcohol" -> "/check-tax-on-goods-you-bring-into-the-uk/select-new-goods/alcohol",
        "tobacco" -> "/check-tax-on-goods-you-bring-into-the-uk/select-new-goods/tobacco"
      )
    ) { case (goodsType, destination) =>
      s"redirect to $goodsType selection" in {
        val result = route(
          app,
          enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/add-an-item")
            .withFormUrlEncodedBody("goodsType" -> goodsType)
        ).get

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(destination)
      }
    }

    "redirect to the other goods input page when other goods are selected" in {
      val result = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/add-an-item")
          .withFormUrlEncodedBody("goodsType" -> "other-goods")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/other-goods/tell-us"
      )
    }

    "redirect to the vaping products input page when vaping products are selected" in {
      val result = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/add-an-item")
          .withFormUrlEncodedBody("goodsType" -> "vaping-products")
      ).get

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/check-tax-on-goods-you-bring-into-the-uk/enter-goods/vaping-products/vape/tell-us"
      )
    }

    "show an error when no goods type is selected" in {
      val result = route(app, enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/add-an-item")).get

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Select which type of goods you want to add")
    }
  }
}
