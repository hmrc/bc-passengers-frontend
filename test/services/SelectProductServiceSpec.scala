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

package services

import connectors.Cache
import models.{JourneyData, ProductAlias, ProductPath}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import util.BaseSpec

import scala.concurrent.Future

class SelectProductServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .build()

  override def beforeEach(): Unit =
    reset(app.injector.instanceOf[Cache])

  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val selectProductService: SelectProductService = {
      val service = app.injector.instanceOf[SelectProductService]
      val mock    = service.cache
      when(mock.fetch(any())) thenReturn Future.successful(journeyDataInCache)
      when(mock.store(any())(any())) thenReturn Future.successful(JourneyData())
      service
    }

    lazy val cacheMock: Cache = selectProductService.cache

  }

  "Calling addSelectedProductsAsAliases" should {

    "add selected products setting in keystore when journey data does exist there currently" in new LocalSetup {

      override val journeyDataInCache: Option[JourneyData] = Some(
        JourneyData(
          None,
          Some("euOnly"),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(true),
          Nil
        )
      )

      val selectedAliases = List(
        ProductAlias("Cigarettes", ProductPath("tobacco/cigarettes")),
        ProductAlias("Cigars", ProductPath("tobacco/cigars"))
      )

      await(
        selectProductService.addSelectedProductsAsAliases(journeyDataInCache.get, selectedAliases.map(_.productPath))
      )

      verify(cacheMock, times(1)).store(
        meq(
          JourneyData(
            None,
            Some("euOnly"),
            Some(false),
            Some(false),
            Some(false),
            Some(false),
            Some(false),
            Some(false),
            Some(false),
            Some(false),
            Some(false),
            Some(false),
            Some(true),
            List(
              ProductAlias("label.tobacco.cigarettes", ProductPath("tobacco/cigarettes")),
              ProductAlias("label.tobacco.cigars", ProductPath("tobacco/cigars"))
            )
          )
        )
      )(any())
    }

    "store selected products which are ProductTreeBranches at the start of the list in keystore, in the order they were sent, followed by ProductTreeLeaves" in new LocalSetup {

      override val journeyDataInCache: Option[JourneyData] =
        Some(JourneyData(None, None, None, None, None, None, None, None, None, None, None, None, None, List()))

      val selectedProducts: List[ProductPath] = List(
        ProductPath("other-goods/antiques"),
        ProductPath("other-goods/carpets-fabric"),
        ProductPath("other-goods/furniture"),
        ProductPath("other-goods/adult"),
        ProductPath("other-goods/childrens")
      )

      await(selectProductService.addSelectedProductsAsAliases(journeyDataInCache.get, selectedProducts))

      verify(cacheMock, times(1)).store(
        meq(
          JourneyData(
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            List(
              ProductAlias("label.other-goods.carpets-fabric", ProductPath("other-goods/carpets-fabric")),
              ProductAlias("label.other-goods.adult", ProductPath("other-goods/adult")),
              ProductAlias("label.other-goods.childrens", ProductPath("other-goods/childrens")),
              ProductAlias("label.other-goods.antiques", ProductPath("other-goods/antiques")),
              ProductAlias("label.other-goods.furniture", ProductPath("other-goods/furniture"))
            )
          )
        )
      )(any())
    }
  }

  "Calling removeSelectedProduct" should {

    "remove the first selected product and update keystore" in new LocalSetup {

      override val journeyDataInCache: Option[JourneyData] = Some(
        JourneyData(
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          List(
            ProductAlias("tobacco.cigarettes", ProductPath("tobacco/cigarettes")),
            ProductAlias("tobacco.cigars", ProductPath("tobacco/cigars"))
          )
        )
      )

      await(selectProductService.removeSelectedAlias(journeyDataInCache.get))

      verify(cacheMock, times(1)).store(
        meq(
          JourneyData(
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            List(
              ProductAlias("tobacco.cigars", ProductPath("tobacco/cigars"))
            )
          )
        )
      )(any())
    }
  }
}
