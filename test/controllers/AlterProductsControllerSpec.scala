package controllers

import models.{JourneyData, ProductPath, PurchasedProduct, PurchasedProductInstance}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services.{PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future

class AlterProductsControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService])
  }

  val controller: AlterProductsController = app.injector.instanceOf[AlterProductsController]

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(), any(), any())(any(), any())) thenReturn Future.successful(JourneyData())
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)

      rt(app, req)
    }
  }

  "Calling GET /bc-passengers-frontend/dashboard/.../remove" should {

    "show the confirm page" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Egypt"), ageOver17 = Some(true), privateCraft = Some(false), purchasedProducts = List(
        PurchasedProduct(ProductPath("alcohol/beer"), List(
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some("USD"), Some(BigDecimal("4.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some("USD"), Some(BigDecimal("24.99")))
        ))
      )))

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol/beer/iid1/remove")).get

      status(response) shouldBe OK

      verify(injected[PurchasedProductService], times(0)).removePurchasedProductInstance(any(), any(), any())(any(), any())

    }
  }

  "Calling POST /bc-passengers-frontend/dashboard/.../remove" should {

    "remove the product from purchased products if true was submitted" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Egypt"), ageOver17 = Some(true), privateCraft = Some(false), purchasedProducts = List(
        PurchasedProduct(ProductPath("alcohol/beer"), List(
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some("USD"), Some(BigDecimal("4.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some("USD"), Some(BigDecimal("24.99")))
        ))
      )))

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/iid1/remove").withFormUrlEncodedBody("confirmRemove" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/dashboard")

      verify(injected[PurchasedProductService], times(1)).removePurchasedProductInstance(any(), any(), any())(any(), any())

    }

    "not remove the product from purchased products if false was submitted" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Egypt"), ageOver17 = Some(true), privateCraft = Some(false), purchasedProducts = List(
        PurchasedProduct(ProductPath("alcohol/beer"), List(
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some("USD"), Some(BigDecimal("4.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some("USD"), Some(BigDecimal("24.99")))
        ))
      )))

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/iid1/remove").withFormUrlEncodedBody("confirmRemove" -> "false")).get

      status(response) shouldBe SEE_OTHER

      verify(injected[PurchasedProductService], times(0)).removePurchasedProductInstance(any(), any(), any())(any(), any())

    }

    "re-display the input form with a 400 status if no form data was submitted" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(country = Some("Egypt"), ageOver17 = Some(true), privateCraft = Some(false), purchasedProducts = List(
        PurchasedProduct(ProductPath("alcohol/beer"), List(
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some("USD"), Some(BigDecimal("12.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some("USD"), Some(BigDecimal("4.99"))),
          PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some("USD"), Some(BigDecimal("24.99")))
        ))
      )))

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/iid1/remove")).get

      status(response) shouldBe BAD_REQUEST

      verify(injected[PurchasedProductService], times(0)).removePurchasedProductInstance(any(), any(), any())(any(), any())

    }
  }

}
