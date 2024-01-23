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

package services

import connectors.Cache
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.BCPassengersSessionRepository
import util.BaseSpec

import scala.concurrent.Future

class PurchasedProductServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .build()

  override def beforeEach(): Unit =
    reset(app.injector.instanceOf[Cache])

  trait LocalSetup {
    def journeyDataInCache: Option[JourneyData]

    lazy val s: PurchasedProductService = {
      val service = app.injector.instanceOf[PurchasedProductService]
      val mock    = service.cache
      when(mock.fetch(any())) thenReturn Future.successful(journeyDataInCache)
      when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
      service
    }
  }

  "Calling PurchasedProductService.clearWorkingInstance" should {

    "remove the working instance" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(
        JourneyData(workingInstance =
          Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", currency = Some("USD")))
        )
      )

      await(s.clearWorkingInstance(journeyDataInCache.get))

      verify(s.cache, times(1)).store(
        meq(JourneyData(workingInstance = None))
      )(any())

    }

  }

  "Calling PurchasedProductService.removePurchasedProductInstance" should {

    "remove the working instance" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(
        JourneyData(purchasedProductInstances =
          List(
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid0",
              Some(BigDecimal("16.0")),
              None,
              Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
              None,
              Some("USD"),
              Some(BigDecimal("12.99"))
            ),
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid1",
              Some(BigDecimal("2.0")),
              None,
              Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
              None,
              Some("USD"),
              Some(BigDecimal("4.99"))
            ),
            PurchasedProductInstance(
              ProductPath("alcohol/beer"),
              "iid2",
              Some(BigDecimal("4.0")),
              None,
              Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
              None,
              Some("USD"),
              Some(BigDecimal("24.99"))
            )
          )
        )
      )

      await(s.removePurchasedProductInstance(journeyDataInCache.get, "iid1"))

      verify(s.cache, times(1)).store(
        meq(
          JourneyData(purchasedProductInstances =
            List(
              PurchasedProductInstance(
                ProductPath("alcohol/beer"),
                "iid0",
                Some(BigDecimal("16.0")),
                None,
                Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
                None,
                Some("USD"),
                Some(BigDecimal("12.99"))
              ),
              PurchasedProductInstance(
                ProductPath("alcohol/beer"),
                "iid2",
                Some(BigDecimal("4.0")),
                None,
                Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
                None,
                Some("USD"),
                Some(BigDecimal("24.99"))
              )
            )
          )
        )
      )(any())
    }
  }
}
