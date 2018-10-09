package services

import models.{JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import util.BaseSpec

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

  "Calling PurchasedProductService.makeWorkingInstance" should {

    "make the provided purchased product instance the working product" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0",  country = Some("Egypt"), currency = Some("USD")),
        PurchasedProductInstance(ProductPath("some/other/item"), iid = "iid1", country = Some("Jamaica"), currency = Some("JMD"))),
        workingInstance = None))

      val productToMakeWorkingInstance = PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some("Egypt"), currency = Some("USD"))

      await(s.makeWorkingInstance(journeyDataInCache.get, productToMakeWorkingInstance))
      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(purchasedProductInstances = List(PurchasedProductInstance(ProductPath("some/item/path"),"iid0",None,None,Some("Egypt"),Some("USD"),None),
          PurchasedProductInstance(ProductPath("some/other/item"),"iid1",None,None,Some("Jamaica"),Some("JMD"),None)),
          workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some("Egypt"), currency = Some("USD")))))
      )(any())

    }

  }

  "Calling PurchasedProductService.removePurchasedProductInstance" should {

    "remove the working instance" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData( purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("12.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("4.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("24.99")))
      )))

      await(s.removePurchasedProductInstance(journeyDataInCache.get, ProductPath("alcohol/beer"), "iid1"))

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData( purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("24.99")))
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

  "Calling PurchasedProductService.storeCountry" should {

    "store a new working instance, containing the country" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(s.storeCountry(
        JourneyData(), ProductPath("some/item/path"), "iid0", "United States"))

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some("United States")))))
      )(any())

    }
  }

  "Calling PurchasedProductService.storeNoOfSticks" should {

    "store a new working instance, containing the noOfSticks" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(s.storeNoOfSticks(JourneyData(), ProductPath("some/item/path"), "iid0", 100))

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", noOfSticks = Some(100)))))
      )(any())

    }
  }

  "Calling PurchasedProductService.updateNoOfSticks" should {

    "update a current selected product, containing the updated noOfSticks" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, Some(50), Some("Egypt"), Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("another/item/path"), "iid1", Some(BigDecimal("4.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("24.99")))
      )))

      await(s.updateNoOfSticks(journeyDataInCache.get, ProductPath("some/item/path"), "iid0", 100)
      )

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, Some(100), Some("Egypt"), Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("another/item/path"), "iid1", Some(BigDecimal("4.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("24.99")))
        ))
      ))(any())

    }
  }

  "Calling PurchasedProductService.updateWeightOrVolume" should {

    "update a current selected product, containing the updated weightOrVolume" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("some/item/path"), "iid0", Some(BigDecimal("500")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("15.50"))),
          PurchasedProductInstance(ProductPath("another/item/path"), "iid1", Some(BigDecimal("4.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("24.99")))
        )))

      await(s.updateWeightOrVolume(journeyDataInCache.get, ProductPath("some/item/path"), "iid0", BigDecimal("1000"))
      )

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("some/item/path"), "iid0", Some(BigDecimal("1000")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("15.50"))),
          PurchasedProductInstance(ProductPath("another/item/path"), "iid1", Some(BigDecimal("4.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("24.99")))
        ))
      ))(any())
    }
  }

  "Calling PurchasedProductService.updateNoOfSticksAndWeightOrVolume" should {

    "update a current selected product, containing the updated noOfSticks and the updated weightOrVolume" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("some/item/path"), "iid0", Some(BigDecimal("500")), Some(100), Some("Egypt"), Some("USD"), Some(BigDecimal("15.50"))),
          PurchasedProductInstance(ProductPath("another/item/path"), "iid1", Some(BigDecimal("4.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("24.99")))
        )))

      await(s.updateNoOfSticksAndWeightOrVolume(journeyDataInCache.get, ProductPath("some/item/path"), "iid0", 200, BigDecimal("1000"))
      )

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("some/item/path"), "iid0", Some(BigDecimal("1000")), Some(200), Some("Egypt"), Some("USD"), Some(BigDecimal("15.50"))),
          PurchasedProductInstance(ProductPath("another/item/path"), "iid1", Some(BigDecimal("4.0")), None, Some("Egypt"), Some("USD"), Some(BigDecimal("24.99")))
        ))
      ))(any())
    }
  }
}
