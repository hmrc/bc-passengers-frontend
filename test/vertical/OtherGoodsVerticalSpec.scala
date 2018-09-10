package vertical

import models.{JourneyData, ProductPath, PurchasedProduct, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import services.LocalSessionCache

import scala.concurrent.Future

class OtherGoodsVerticalSpec extends VerticalBaseSpec {

  val requiredJourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))

  "Calling GET /products/other-goods/.../quantity" should {

    "return a 200" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/quantity")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/other-goods/.../quantity" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "2")).get

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should fullyMatch regex """^/bc-passengers-frontend/products/other-goods/books/currency/[a-zA-Z0-9]{6}[?]ir=2$""".r

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling GET /products/other-goods/.../currency/iid0" should {

    // TODO: Revise after enforcer is added to other goods controller

    "return a 404 when the product path is not found" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/not/a/real/product/currency/iid0?ir=1")).get

      status(result) shouldBe NOT_FOUND

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when cached JourneyData exists" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(purchasedProducts = List(PurchasedProduct(ProductPath("other-goods/books")))))
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/currency/iid0?ir=1")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/other-goods/.../currency/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/currency/iid0").withFormUrlEncodedBody("currency" -> "Not a currency code")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(purchasedProducts = List(PurchasedProduct(ProductPath("other-goods/books")))))
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/currency/iid0").withFormUrlEncodedBody("currency" -> "USD", "itemsRemaining" -> "6")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/other-goods/books/cost/iid0?ir=6")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(purchasedProducts = List(
        PurchasedProduct(ProductPath("other-goods/books"), purchasedProductInstances = List(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD")))))
      )))(any())
    }
  }

  "Calling GET /products/other-goods/.../cost/iid0" should {

    "start new session when there is no cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/cost/iid0?ir=1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/new-session")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect to the dashboard when there is no currency in the cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(
        purchasedProducts = List(
          PurchasedProduct(
            path = ProductPath("other-goods/books")
          )
        ))
      )
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/cost/iid0?ir=1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when there is a currency in the cached journey data for this product instance" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        purchasedProducts = List(
          PurchasedProduct(
            path = ProductPath("other-goods/books"),
            purchasedProductInstances = List(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD"))))
          )
        )
      )
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/cost/iid0?ir=1")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/other-goods/.../cost/iid0" should {

    "return a 500 when given bad form input and there is no cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/new-session")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 500 when given bad form input and there is no currency in the cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        purchasedProducts = List(
          PurchasedProduct(
            path = ProductPath("other-goods/books")
          )
        ))
      )
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        purchasedProducts = List(
          PurchasedProduct(
            path = ProductPath("other-goods/books"),
            purchasedProductInstances = List(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD"))))
          )
        )
      )
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/cost/iid0")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        purchasedProducts = List(
          PurchasedProduct(
            path = ProductPath("other-goods/books"),
            purchasedProductInstances = List(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD"))))
          )
        )
      )
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/cost/iid0").withFormUrlEncodedBody("cost" -> "2.99", "itemsRemaining" -> "10")).get

      status(result) shouldBe SEE_OTHER

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(purchasedProducts = List(
        PurchasedProduct(ProductPath("other-goods/books"), purchasedProductInstances = List(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD"), cost = Some(BigDecimal(2.99))))))
      )))(any())
    }
  }
}
