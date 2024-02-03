/*
 * Copyright 2024 HM Revenue & Customs
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
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, when}
import org.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, route, status, _}
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class LimitExceedControllerSpec extends BaseSpec {

  val mockCache: Cache         = MockitoSugar.mock[Cache]
  val mockAppConfig: AppConfig = MockitoSugar.mock[AppConfig]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance(mockAppConfig))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCache)
    reset(mockAppConfig)
  }

  lazy val oldAlcohol: PurchasedProductInstance                         =
    PurchasedProductInstance(
      path = ProductPath("alcohol/beer"),
      iid = "iid0",
      weightOrVolume = Some(1.54332),
      noOfSticks = None,
      country = Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
      originCountry = None,
      currency = Some("AUD"),
      cost = Some(BigDecimal(10.234)),
      searchTerm = None,
      isVatPaid = None,
      isCustomPaid = None,
      isEditable = Some(false)
    )
  lazy val oldPurchasedProductInstances: List[PurchasedProductInstance] = List(oldAlcohol)

  "loadLimitExceedPage" when {

    ".onPageLoadAddJourney" should {

      "load limit exceeded page for cider and display alcohol content" in {

        when(mockCache.fetch(any())).thenReturn(
          Future.successful(
            Some(
              JourneyData(
                Some(false),
                Some("greatBritain"),
                arrivingNICheck = Some(true),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false),

              )
            )
          )
        )

        val result: Future[Result] = route(
          app,
          FakeRequest(
            "GET",
            "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/cider/non-sparkling-cider/upper-limits/volume"
          ).withSession(SessionKeys.sessionId -> "fakesessionid", "user-amount-input-non-sparkling-cider" -> "111.5")
        ).get
        status(result) shouldBe OK

        val content = contentAsString(result)
        val doc     = Jsoup.parse(content)

        doc
          .getElementsByTag("h1")
          .text() shouldBe "There is a problem"
        doc
          .getElementById("entered-amount")
          .text() shouldBe "You have entered a total of 111.50 litre(s) of cider."
        content     should include(
          "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
            "They will calculate and take payment of the taxes and duties due."
        )
      }

      "load limit exceeded page for cigars and display tobacco content" in {
        when(mockCache.fetch(any())).thenReturn(
          Future.successful(
            Some(
              JourneyData(
                Some(false),
                Some("greatBritain"),
                arrivingNICheck = Some(true),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false)
              )
            )
          )
        )
        val result: Future[Result] = route(
          app,
          FakeRequest(
            "GET",
            "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/cigars/upper-limits/units-of-product"
          ).withSession(SessionKeys.sessionId -> "fakesessionid", "user-amount-input-cigars" -> "201")
        ).get
        status(result) shouldBe OK

        val content = contentAsString(result)
        val doc     = Jsoup.parse(content)

        doc.getElementsByTag("h1").text() shouldBe "There is a problem"
        doc
          .getElementById("entered-amount")
          .text()                         shouldBe "You have entered a total of 201 cigar(s)."
        content                             should include(
          "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
            "They will calculate and take payment of the taxes and duties due."
        )
      }

      "do not load limit exceed page if the path is incorrect" in {
        when(mockCache.fetch(any())).thenReturn(
          Future.successful(
            Some(
              JourneyData(
                Some(false),
                Some("greatBritain"),
                arrivingNICheck = Some(true),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false)
              )
            )
          )
        )
        val result: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/goods/yyy/zzz/upper-limits")
        ).get
        status(result) shouldBe NOT_FOUND
      }
    }

    ".onPageLoadEditAlcoholWeightOrVolume" should {

      "load limit exceeded page for cider and display alcohol content" in {

        when(mockCache.fetch(any())).thenReturn(
          Future.successful(
            Some(
              JourneyData(
                Some(false),
                Some("greatBritain"),
                arrivingNICheck = Some(true),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false),
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("alcohol/non-sparkling-cider"),
                    iid = "iid0",
                    weightOrVolume = Some(20.0),
                    noOfSticks = None,
                    country = None,
                    originCountry = None,
                    currency = Some("EUR"),
                    cost = Some(BigDecimal(12.99))
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("alcohol/non-sparkling-cider"),
                    "iid0",
                    Some(20.0),
                    None,
                    None,
                    None,
                    Some("EUR"),
                    Some(BigDecimal(12.99))
                  )
                )
              )
            )
          )
        )
        val result: Future[Result] = route(
          app,
          FakeRequest(
            "GET",
            "/check-tax-on-goods-you-bring-into-the-uk/goods/alcohol/cider/non-sparkling-cider/upper-limits/edit/volume"
          ).withSession(SessionKeys.sessionId -> "fakesessionid", "user-amount-input-non-sparkling-cider" -> "50.50")
        ).get
        status(result) shouldBe OK

        val content = contentAsString(result)
        val doc     = Jsoup.parse(content)

        doc
          .getElementsByTag("h1")
          .text() shouldBe "There is a problem"
        doc
          .getElementById("entered-amount")
          .text() shouldBe "You changed 20.00 litre(s) of cider to 50.50 litre(s) of cider"
        doc
          .getElementById("new-total-amount")
          .text() shouldBe "This means your total is now 50.50 litre(s) of cider"
        content     should include(
          "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
            "They will calculate and take payment of the taxes and duties due."
        )
      }

      "do not load limit exceed page if the path is incorrect" in {
        when(mockCache.fetch(any())).thenReturn(
          Future.successful(
            Some(
              JourneyData(
                Some(false),
                Some("greatBritain"),
                arrivingNICheck = Some(true),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false),
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("alcohol/non-sparkling-cider"),
                    iid = "iid0",
                    weightOrVolume = Some(20.0),
                    noOfSticks = None,
                    country = None,
                    originCountry = None,
                    currency = Some("EUR"),
                    cost = Some(BigDecimal(12.99))
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("alcohol/non-sparkling-cider"),
                    "iid0",
                    Some(20.0),
                    None,
                    None,
                    None,
                    Some("EUR"),
                    Some(BigDecimal(12.99))
                  )
                )
              )
            )
          )
        )
        val result: Future[Result] = route(
          app,
          enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/goods/yyy/zzz/upper-limits")
        ).get
        status(result) shouldBe NOT_FOUND
      }
    }

    ".onPageLoadEditNoOfSticks" should {

      "load limit exceeded page for cigarettes and display correct content for cigarettes" in {
        when(mockCache.fetch(any())).thenReturn(
          Future.successful(
            Some(
              JourneyData(
                Some(false),
                Some("greatBritain"),
                arrivingNICheck = Some(true),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false),
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    iid = "iid0",
                    weightOrVolume = None,
                    noOfSticks = Some(800),
                    country = None,
                    originCountry = None,
                    currency = Some("EUR"),
                    cost = Some(BigDecimal(12.99))
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigarettes"),
                    "iid0",
                    None,
                    noOfSticks = Some(800),
                    None,
                    None,
                    Some("EUR"),
                    Some(BigDecimal(12.99))
                  )
                )
              )
            )
          )
        )
        val result: Future[Result] = route(
          app,
          FakeRequest(
            "GET",
            "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/cigarettes/upper-limits/edit/units-of-product"
          ).withSession(SessionKeys.sessionId -> "fakesessionid", "user-amount-input-cigarettes" -> "801")
        ).get
        status(result) shouldBe OK

        val content = contentAsString(result)
        val doc = Jsoup.parse(content)

        doc.getElementsByTag("h1").text() shouldBe "There is a problem"
        doc
          .getElementById("entered-amount")
          .text() shouldBe "You changed 800 cigarette(s) to 801 cigarette(s)"
        doc
          .getElementById("new-total-amount")
          .text() shouldBe "This means your total is now 801 cigarette(s)"
        content should include(
          "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
            "They will calculate and take payment of the taxes and duties due."
        )
      }


      "load limit exceeded page for cigars and display tobacco content" in {
        when(mockCache.fetch(any())).thenReturn(
          Future.successful(
            Some(
              JourneyData(
                Some(false),
                Some("greatBritain"),
                arrivingNICheck = Some(true),
                isVatResClaimed = None,
                isBringingDutyFree = None,
                bringingOverAllowance = Some(true),
                ageOver17 = Some(true),
                privateCraft = Some(false),
                purchasedProductInstances = List(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigars"),
                    iid = "iid0",
                    weightOrVolume = None,
                    noOfSticks = Some(200),
                    country = None,
                    originCountry = None,
                    currency = Some("EUR"),
                    cost = Some(BigDecimal(12.99))
                  )
                ),
                workingInstance = Some(
                  PurchasedProductInstance(
                    ProductPath("tobacco/cigars"),
                    "iid0",
                    None,
                    noOfSticks = Some(200),
                    None,
                    None,
                    Some("EUR"),
                    Some(BigDecimal(12.99))
                  )
                )
              )
            )
          )
        )
        val result: Future[Result] = route(
          app,
          FakeRequest(
            "GET",
            "/check-tax-on-goods-you-bring-into-the-uk/goods/tobacco/cigars/upper-limits/edit/units-of-product"
          ).withSession(SessionKeys.sessionId -> "fakesessionid", "user-amount-input-cigars" -> "201")
        ).get
        status(result) shouldBe OK

        val content = contentAsString(result)
        val doc     = Jsoup.parse(content)

        doc.getElementsByTag("h1").text() shouldBe "There is a problem"
        doc
          .getElementById("entered-amount")
          .text()                         shouldBe "You changed 200 cigar(s) to 201 cigar(s)"
        doc
          .getElementById("new-total-amount")
          .text()                         shouldBe "This means your total is now 201 cigar(s)"
        content                             should include(
          "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
            "They will calculate and take payment of the taxes and duties due."
        )
      }
    }
  }
}
