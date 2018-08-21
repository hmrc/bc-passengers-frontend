package vertical

import models.{JourneyData, ProductPath, PurchasedProduct, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import services.LocalSessionCache

import scala.concurrent.Future

class AlcoholVerticalSpec extends VerticalBaseSpec {

  "Calling GET /products/alcohol/.../volume" should {

    "return a 200" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol/beer/volume/0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/alcohol/.../volume" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/volume/0")
        .withFormUrlEncodedBody("volume" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())

    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData())

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/volume/0")
        .withFormUrlEncodedBody("volume" -> "2.5")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/alcohol/beer/currency/0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = Some(List(
        PurchasedProduct(Some(ProductPath("alcohol/beer")), purchasedProductInstances = Some(List(
          PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5))))))
      )))))(any())
    }
  }

  "Calling GET /products/alcohol/.../currency/0" should {

    // TODO: Revise after enforcer is added to other goods controller

    "return a 500 when the product path is not found" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol/not/a/real/product/currency/0")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when cached JourneyData exists" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(purchasedProducts = Some(List(
        PurchasedProduct(Some(ProductPath("alcohol/beer")), purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5))))))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol/beer/currency/0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/alcohol/.../currency/0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(purchasedProducts = Some(List(
        PurchasedProduct(Some(ProductPath("alcohol/beer")), purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5))))))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/currency/0")
        .withFormUrlEncodedBody("currency" -> "Not a currency code")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "return a 303 given valid form input" in new LocalSetup {

    override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(purchasedProducts = Some(List(
      PurchasedProduct(Some(ProductPath("alcohol/beer")), purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5))))))))))

    val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/currency/0")
      .withFormUrlEncodedBody("currency" -> "USD")).get

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/alcohol/beer/cost/0")

    verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
    verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = Some(List(
      PurchasedProduct(Some(ProductPath("alcohol/beer")), purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5)), currency = Some("USD")))))
    )))))(any())
  }


  "Calling GET /products/alcohol/.../cost/0" should {

    "return a 500 when there is no cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET",
        "/bc-passengers-frontend/products/alcohol/beer/cost/0")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 500 when there is no currency in the cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("alcohol/beer")),
            purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5)))))
          )
        ))
      ))

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol/beer/cost/0")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when there is a currency in the cached journey data for this product instance" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("alcohol/beer")),
            purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5)), currency = Some("USD"))))
          )
        ))
      ))

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol/beer/cost/0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/alcohol/.../cost/0" should {

    "return a 500 when given a bad form input and there is no cached journey data" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = None

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/cost/0")
          .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 500 when given bad form input and there is no currency in the cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("alcohol/beer")),
            purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5)))))
          )
        ))
      ))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/cost/0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 400 when given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("alcohol/beer")),
            purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5)), currency = Some("USD"))))
          )
        ))
      ))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/cost/0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())

    }

    "return a 303 when given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("alcohol/beer")),
            purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5)), currency = Some("USD"))))
          )
        ))
      ))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol/beer/cost/0")
        .withFormUrlEncodedBody("cost" -> "5.99")).get

      status(result) shouldBe SEE_OTHER

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = Some(List(
        PurchasedProduct(Some(ProductPath("alcohol/beer")), purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(2.5)), currency = Some("USD"), cost = Some(BigDecimal(5.99))))))
      )))))(any())
    }
  }
}
