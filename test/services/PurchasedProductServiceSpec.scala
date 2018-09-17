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


  "Calling PurchasedProductService.storeCurrency" should {

    "create a new PurchasedProductInstance on the PurchasedProduct which matches the supplied path, containing the currency" in new LocalSetup {

      override def journeyDataInCache: Option[JourneyData] = None

      await(s.storeCurrency(
        JourneyData(purchasedProducts = List(PurchasedProduct(ProductPath("some/item/path")))),
        ProductPath("some/item/path"),
        "iid0", "USD"
      ))

      verify(s.localSessionCache, times(1)).cacheJourneyData(
        meq(JourneyData(purchasedProducts = List(PurchasedProduct(
          path = ProductPath("some/item/path"),
          purchasedProductInstances = List(
            PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", currency = Some("USD"))
          ))
        )))
      )(any())

    }

  }

}
