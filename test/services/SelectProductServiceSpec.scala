package services

import models.{JourneyData, ProductPath}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import util.BaseSpec
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SelectProductServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[LocalSessionCache].toInstance(MockitoSugar.mock[LocalSessionCache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[LocalSessionCache])
  }


  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val selectProductService = {
      val service = app.injector.instanceOf[SelectProductService]
      val mock = service.localSessionCache
      when(mock.fetchAndGetJourneyData(any())) thenReturn Future.successful( journeyDataInCache )
      when(mock.cacheJourneyData(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
      service
    }

    lazy val localSessionCacheMock = selectProductService.localSessionCache

  }

  "Calling addSelectedProducts" should {


    "add selected products setting in keystore when journey data does exist there currently" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(Some("Australia"), Some(false), Some(false), Nil) )

      val selectedProducts = List(ProductPath("tobacco/cigarettes"), ProductPath("tobacco/cigars"))

      await(selectProductService.addSelectedProducts(journeyDataInCache.get, selectedProducts))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(Some("Australia"), Some(false), Some(false), List(List("tobacco", "cigarettes"), List("tobacco", "cigars")) )) )(any())
    }

    "store selected products at the start the list in keystore when products already exist" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(None, None, None, List(List("tobacco", "cigarettes"), List("tobacco", "cigars")) ))

      val selectedProducts = List(ProductPath("alcohol/beer"))

      await(selectProductService.addSelectedProducts(journeyDataInCache.get, selectedProducts))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(None, None, None, List(List("alcohol", "beer"), List("tobacco", "cigarettes"), List("tobacco", "cigars")) )) )(any())
    }

    "remove products of the same category before adding more selected products to keystore" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(None, None, None, List(
        List("alcohol", "beer"),
        List("alcohol", "cider"),
        List("tobacco", "cigarettes"),
        List("tobacco", "cigars")
      ) ))

      val selectedProducts = List(ProductPath("alcohol/beer"))

      await(selectProductService.addSelectedProducts(journeyDataInCache.get, selectedProducts))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(None, None, None, List(
        List("alcohol", "beer"),
        List("tobacco", "cigarettes"),
        List("tobacco", "cigars")
      ) )) )(any())
    }

    "remove products of the same category (but not those of a sibling subcategory) before adding more selected products to keystore" in new LocalSetup {

      override val journeyDataInCache = Some( JourneyData(None, None, None, List(
        List("other-goods", "carpets-cotton-fabric", "carpets"),
        List("other-goods", "carpets-cotton-fabric", "cotton"),
        List("other-goods", "electronic-devices", "televisions")
      ) ))

      val selectedProducts = List(ProductPath("other-goods/carpets-cotton-fabric/fabrics"))

      await(selectProductService.addSelectedProducts(journeyDataInCache.get, selectedProducts))

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(None, None, None, List(
        List("other-goods", "carpets-cotton-fabric", "fabrics"),
        List("other-goods", "electronic-devices", "televisions")
      ) )) )(any())
    }

  }

  "Calling removeSelectedProduct" should {

    "remove the first selected product and update keystore" in new LocalSetup {

      override val journeyDataInCache = Some(JourneyData(None, None, None, List(List("tobacco", "cigarettes"), List("tobacco", "cigars"))))

      await(selectProductService.removeSelectedProduct())

      verify(localSessionCacheMock, times(1)).cacheJourneyData( meq(JourneyData(None, None, None, List(List("tobacco", "cigars")) )) )(any())
    }
  }
}
