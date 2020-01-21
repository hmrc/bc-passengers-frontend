package services

import connectors.Cache
import models.{JourneyData, ProductAlias, ProductPath}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import util.BaseSpec

import scala.concurrent.Future

class SelectProductServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[Cache])
  }


  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val selectProductService = {
      val service = app.injector.instanceOf[SelectProductService]
      val mock = service.cache
      when(mock.fetch(any())) thenReturn Future.successful( journeyDataInCache )
      when(mock.store(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
      service
    }

    lazy val cacheMock = selectProductService.cache

  }

  "Calling addSelectedProductsAsAliases" should {

    "add selected products setting in keystore when journey data does exist there currently" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("euOnly"), Some(false), Some(false), Some(false), Some(false), Some(false), Some(true), Nil) )

      var selectedAliases = List(
        ProductAlias("Cigarettes", ProductPath("tobacco/cigarettes")),
        ProductAlias("Cigars", ProductPath("tobacco/cigars"))
      )

      await(selectProductService.addSelectedProductsAsAliases(journeyDataInCache.get, selectedAliases.map(_.productPath)))

      verify(cacheMock, times(1)).store( meq(JourneyData(Some("euOnly"), Some(false), Some(false), Some(false), Some(false), Some(false), Some(true),
        List( ProductAlias("label.tobacco.cigarettes", ProductPath("tobacco/cigarettes")), ProductAlias("label.tobacco.cigars", ProductPath("tobacco/cigars")) ))) )(any())
    }

    "store selected products which are ProductTreeBranches at the start of the list in keystore, in the order they were sent, followed by ProductTreeLeaves" in new LocalSetup {

      override val journeyDataInCache = Some(JourneyData(None, None, None, None, None, None, None, List()))

      val selectedProducts = List(
        ProductPath("other-goods/antiques"),
        ProductPath("other-goods/carpets-fabric"),
        ProductPath("other-goods/furniture"),
        ProductPath("other-goods/adult"),
        ProductPath("other-goods/childrens")
      )

      await(selectProductService.addSelectedProductsAsAliases(journeyDataInCache.get, selectedProducts))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, None, None, None, None, None,
        List(
          ProductAlias("label.other-goods.carpets-fabric", ProductPath("other-goods/carpets-fabric")),
          ProductAlias("label.other-goods.adult", ProductPath("other-goods/adult")),
          ProductAlias("label.other-goods.childrens", ProductPath("other-goods/childrens")),
          ProductAlias("label.other-goods.antiques", ProductPath("other-goods/antiques")),
          ProductAlias("label.other-goods.furniture", ProductPath("other-goods/furniture"))
        )
      )))(any())
    }
  }

  "Calling removeSelectedProduct" should {

    "remove the first selected product and update keystore" in new LocalSetup {

      override val journeyDataInCache = Some(JourneyData(None, None, None, None, None, None, None,
        List(ProductAlias("tobacco.cigarettes", ProductPath("tobacco/cigarettes")), ProductAlias("tobacco.cigars", ProductPath("tobacco/cigars"))) ))

      await(selectProductService.removeSelectedAlias(journeyDataInCache.get))

      verify(cacheMock, times(1)).store( meq(JourneyData(None, None, None, None, None, None, None,
        List(
          ProductAlias("tobacco.cigars", ProductPath("tobacco/cigars"))
        ) ))
      )(any())
    }
  }
}
