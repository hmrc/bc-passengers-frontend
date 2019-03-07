package services

import connectors.Cache
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
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
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[Cache])
  }


  trait LocalSetup {
    def journeyDataInCache: Option[JourneyData]

    lazy val s = {
      val service = app.injector.instanceOf[PurchasedProductService]
      val mock = service.cache
      when(mock.fetch(any())) thenReturn Future.successful( journeyDataInCache )
      when(mock.store(any())(any())) thenReturn Future.successful( CacheMap("fakeid", Map.empty) )
      service
    }
  }


  "Calling PurchasedProductService.clearWorkingInstance" should {

    "remove the working instance" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData(workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", currency = Some("USD")))))

      await(s.clearWorkingInstance(journeyDataInCache.get))

      verify(s.cache, times(1)).store(
        meq(JourneyData(workingInstance = None))
      )(any())

    }

  }

  "Calling PurchasedProductService.makeWorkingInstance" should {

    "make the provided purchased product instance the working product" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0",  country = Some(Country("Egypt", "EG", isEu = false, Nil)), currency = Some("USD")),
        PurchasedProductInstance(ProductPath("some/other/item"), iid = "iid1", country = Some(Country("Jamaica", "JM", isEu = false, Nil)), currency = Some("JMD"))),
        workingInstance = None))

      val productToMakeWorkingInstance = PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Nil)), currency = Some("USD"))

      await(s.makeWorkingInstance(journeyDataInCache.get, productToMakeWorkingInstance))
      verify(s.cache, times(1)).store(
        meq(JourneyData(purchasedProductInstances = List(PurchasedProductInstance(ProductPath("some/item/path"),"iid0",None,None,Some(Country("Egypt", "EG", isEu = false, Nil)),Some("USD"),None),
          PurchasedProductInstance(ProductPath("some/other/item"),"iid1",None,None,Some(Country("Jamaica", "JM", isEu = false, Nil)),Some("JMD"),None)),
          workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Nil)), currency = Some("USD")))))
      )(any())

    }

  }

  "Calling PurchasedProductService.removePurchasedProductInstance" should {

    "remove the working instance" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData( purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("24.99")))
      )))

      await(s.removePurchasedProductInstance(journeyDataInCache.get, ProductPath("alcohol/beer"), "iid1"))

      verify(s.cache, times(1)).store(
        meq(JourneyData( purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("24.99")))
        )))
      )(any())

    }

  }

  "Calling PurchasedProductService.storeCurrency" should {

    "store a new working instance, containing the currency" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(s.storeCurrency(JourneyData(), ProductPath("some/item/path"), "iid0", "USD"))

      verify(s.cache, times(1)).store(
        meq(JourneyData(workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", currency = Some("USD")))))
      )(any())

    }
  }

  "Calling PurchasedProductService.storeCountry" should {

    "store a new working instance, containing the country" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(s.storeCountry(
        JourneyData(), ProductPath("some/item/path"), "iid0", Country("United States of America (the)", "US", isEu = false, Nil)))

      verify(s.cache, times(1)).store(
        meq(JourneyData(workingInstance = Some(PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some(Country("United States of America (the)", "US", isEu = false, Nil))))))
      )(any())

    }
  }

  "Calling PurchasedProductService.updateCountry" should {

    "update a current selected product, containing the updated country and the updated weightOrVolume" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = Some(JourneyData(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("some/item/path"), "iid0", Some(BigDecimal("500")), Some(100), Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("15.50"))),
        PurchasedProductInstance(ProductPath("another/item/path"), "iid1", Some(BigDecimal("4.0")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("24.99")))
      )))

      await(s.updateCountry(journeyDataInCache.get, ProductPath("some/item/path"), "iid0", Country("Jamaica", "JM", isEu = false, Nil))
      )

      verify(s.cache, times(1)).store(
        meq(JourneyData(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("some/item/path"), "iid0", Some(BigDecimal("500")), Some(100), Some(Country("Jamaica", "JM", isEu = false, Nil)), Some("USD"), Some(BigDecimal("15.50"))),
          PurchasedProductInstance(ProductPath("another/item/path"), "iid1", Some(BigDecimal("4.0")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("24.99")))
        ))
        ))(any())
    }
  }
}
