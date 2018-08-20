package vertical

import models.{JourneyData, ProductPath, PurchasedProduct, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import services.LocalSessionCache


class OtherGoodsVerticalSpec extends VerticalBaseSpec {

  "Calling GET /products/.../quantity" should {

    "return a 200" in new LocalSetup {

      override lazy val cachedJourneyData = None
      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/quantity") ).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/.../quantity" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData = None
      val result = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "NaN") ).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData())
      val result = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "2") ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/other-goods/books/currency/0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = Some(List(
        PurchasedProduct(Some(ProductPath("other-goods/books")), quantity = Some(2))
      )))))(any())
    }
  }

  "Calling GET /products/.../currency/0" should {

    "return a 404 when the product path is not found" in new LocalSetup {

      override lazy val cachedJourneyData = None
      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/not/a/real/product/currency/0") ).get

      status(result) shouldBe NOT_FOUND

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when cached JourneyData exists" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(purchasedProducts = Some(List(PurchasedProduct(Some(ProductPath("other-goods/books")), Some(1))))))
      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/currency/0") ).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/.../currency/0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData = None
      val result = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/currency/0").withFormUrlEncodedBody("currency" -> "Not a currency code") ).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(purchasedProducts = Some(List(PurchasedProduct(Some(ProductPath("other-goods/books")), quantity = Some(2))))))
      val result = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/currency/0").withFormUrlEncodedBody("currency" -> "USD") ).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/other-goods/books/cost/0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = Some(List(
        PurchasedProduct(Some(ProductPath("other-goods/books")), quantity = Some(2), purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, currency = Some("USD")))))
      )))))(any())
    }
  }

  "Calling GET /products/.../cost/0" should {

    "return a 500 when there is no cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData = None
      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/cost/0") ).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 500 when there is no currency in the cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("other-goods/books")),
            quantity = Some(2)
          )
        ))
      ))
      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/cost/0") ).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when there is a currency in the cached journey data for this product instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("other-goods/books")),
            quantity = Some(2),
            purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, currency = Some("USD"))))
          )
        ))
      ))
      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/cost/0") ).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/.../cost/0" should {

    "return a 500 when given bad form input and there is no cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData = None
      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/cost/0") ).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 500 when given bad form input and there is no currency in the cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("other-goods/books")),
            quantity = Some(2)
          )
        ))
      ))
      val result = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/cost/0") ).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("other-goods/books")),
            quantity = Some(2),
            purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, currency = Some("USD"))))
          )
        ))
      ))
      val result = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/cost/0")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(
        purchasedProducts = Some(List(
          PurchasedProduct(
            path = Some(ProductPath("other-goods/books")),
            quantity = Some(2),
            purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, currency = Some("USD"))))
          )
        ))
      ))
      val result = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/cost/0").withFormUrlEncodedBody("cost" -> "2.99")).get

      status(result) shouldBe SEE_OTHER

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = Some(List(
        PurchasedProduct(Some(ProductPath("other-goods/books")), quantity = Some(2), purchasedProductInstances = Some(List(PurchasedProductInstance(index = 0, currency = Some("USD"), cost = Some(BigDecimal(2.99))))))
      )))))(any())
    }
  }
}
