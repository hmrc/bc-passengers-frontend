package services

import models.{JourneyData, ProductPath, PurchasedProduct, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.cache.client.CacheMap
import util.BaseSpec
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PurchasedProductServiceSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[LocalSessionCache].toInstance(MockitoSugar.mock[LocalSessionCache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[LocalSessionCache])
  }


  trait LocalSetup {

    def journeyDataInCache: Option[JourneyData]

    lazy val s = {
      val service = app.injector.instanceOf[PurchasedProductService]
      val mock = service.localSessionCache
      when(mock.fetchAndGetJourneyData(any())) thenReturn Future.successful( journeyDataInCache )
      when(mock.cacheJourneyData(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
      service
    }
  }


  "Calling PurchasedProductService.clearWorkingInstance" should {

    "remove the working instance" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData(workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", currency = Some("USD")))))

      await(s.clearWorkingInstance(journeyDataInCache.get))

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(workingInstance = None))
      )(any())

    }

  }

  "Calling PurchasedProductService.removePurchasedProductInstance" should {

    "remove the working instance" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData( purchasedProducts = List(
        PurchasedProduct(ProductPath("alcohol/beer"), List(
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some("USD"), Some(BigDecimal("4.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some("USD"), Some(BigDecimal("24.99")))
        ))
      )))

      await(s.removePurchasedProductInstance(journeyDataInCache.get, ProductPath("alcohol/beer"), "iid1"))

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData( purchasedProducts = List(
          PurchasedProduct(ProductPath("alcohol/beer"), List(
            PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some("USD"), Some(BigDecimal("12.99"))),
            PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some("USD"), Some(BigDecimal("24.99")))
          ))
        )))
      )(any())

    }

  }

  "Calling PurchasedProductService.storeCurrency" should {

    "store a new working instance, containing the currency" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(s.storeCurrency(JourneyData(), ProductPath("some/item/path"), "iid0", "USD"))

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", currency = Some("USD")))))
      )(any())

    }

  }

}
