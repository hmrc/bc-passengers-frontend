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

    def requiredJourneyData: JourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))
    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn {
        Future.successful(cachedJourneyData)
      }

      rt(app, req)
    }
  }

  "Calling displayQuantityInput" should {

    "return a 200 with a quantity input view given a correct product path" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/quantity") ).get

      status(result) shouldBe OK
    }
  }

  "Calling processQuantityInput" should {

    "return a 400 with a quantity input view given bad form input" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "NaN") ).get

      status(result) shouldBe BAD_REQUEST
    }

    "return a 303 given valid form input" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "2") ).get

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should fullyMatch regex """^/bc-passengers-frontend/products/other-goods/books/currency/[a-zA-Z0-9]{6}[?]ir=2$""".r
    }
  }
}
