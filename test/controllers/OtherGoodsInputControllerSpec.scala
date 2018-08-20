package controllers

import models.{JourneyData, ProductPath}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services.{PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future


class OtherGoodsInputControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn {
        Future.successful(cachedJourneyData)
      }

      when(injected[PurchasedProductService].storeQuantity(any(), any(), any())(any(),any())) thenReturn {
        Future.successful(CacheMap("fakeid", Map.empty))
      }

      rt(app, req)
    }

  }


  "Calling displayQuantityInput" should {

    "return a 200 with a quantity input view given a correct product path" in new LocalSetup {

      override lazy val cachedJourneyData = None

      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/quantity") ).get

      status(result) shouldBe OK
    }
  }

  "Calling processQuantityInput" should {

    "return a 400 with a quantity input view given bad form input" in new LocalSetup {

      override val cachedJourneyData = None

      val result = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "NaN") ).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[PurchasedProductService], times(0)).storeQuantity(any(), any(), any())(any(),any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override val cachedJourneyData = Some(JourneyData())

      val result = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "2") ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/other-goods/books/currency/0")

      verify(injected[PurchasedProductService], times(1)).storeQuantity(any(), meq(ProductPath("other-goods/books")), meq(2))(any(),any())
    }
  }


}
