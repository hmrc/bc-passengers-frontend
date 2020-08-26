package controllers

import connectors.Cache
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import repositories.BCPassengersSessionRepository
import services.PurchasedProductService
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class AlterProductsControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[Cache], injected[PurchasedProductService])
  }

  val controller: AlterProductsController = app.injector.instanceOf[AlterProductsController]

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].removePurchasedProductInstance(any(), any(), any())(any(), any())) thenReturn Future.successful(JourneyData())
      when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)

      rt(app, req)
    }
  }

  "Calling GET /check-tax-on-goods-you-bring-into-the-uk/tell-us/.../remove" should {

    "show the confirm page" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("nonEuOnly"), arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("24.99")))
      )))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/remove-goods/alcohol/beer/iid1/remove")).get

      status(response) shouldBe OK

      verify(injected[PurchasedProductService], times(0)).removePurchasedProductInstance(any(), any(), any())(any(), any())

    }
  }

  "Calling POST /check-tax-on-goods-you-bring-into-the-uk/tell-us/.../remove" should {

    "remove the product from purchased products if true was submitted" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("nonEuOnly"), arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("24.99")))
      )))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/remove-goods/alcohol/beer/iid1/remove").withFormUrlEncodedBody("confirmRemove" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")

      verify(injected[PurchasedProductService], times(1)).removePurchasedProductInstance(any(), any(), any())(any(), any())

    }

    "not remove the product from purchased products if false was submitted" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("nonEuOnly"), arrivingNICheck = Some(true),isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("24.99")))
      )))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/remove-goods/alcohol/beer/iid1/remove").withFormUrlEncodedBody("confirmRemove" -> "false")).get

      status(response) shouldBe SEE_OTHER

      verify(injected[PurchasedProductService], times(0)).removePurchasedProductInstance(any(), any(), any())(any(), any())

    }

    "re-display the input form with a 400 status if no form data was submitted" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(euCountryCheck = Some("nonEuOnly"),arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true),  ageOver17 = Some(true), privateCraft = Some(false), purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid0", Some(BigDecimal("16.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("4.0")), None, Some(Country("EG", "title.egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("24.99")))
      )))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/remove-goods/alcohol/beer/iid1/remove")).get

      status(response) shouldBe BAD_REQUEST

      verify(injected[PurchasedProductService], times(0)).removePurchasedProductInstance(any(), any(), any())(any(), any())

    }
  }

}
