package vertical

import models.{JourneyData, ProductPath, PurchasedProduct, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{times, verify}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.LocalSessionCache

import scala.concurrent.Future

class TobaccoVerticalSpec extends VerticalBaseSpec {

  "Calling GET /products/tobacco/.../no-of-sticks-weight" should {

    "return a 200" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco/cigars/no-of-sticks-weight/0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling GET /products/tobacco/.../no-of-sticks" should {

    "return a 200" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= None
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco/cigarettes/no-of-sticks/0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling GET /products/tobacco/.../weight" should {

    "return a 200" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= None

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco/rolling/weight/0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }


  "Calling POST /products/tobacco/.../no-of-sticks" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= None
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigarettes/no-of-sticks/0")
        .withFormUrlEncodedBody("noOfSticks" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData())
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigarettes/no-of-sticks/0")
        .withFormUrlEncodedBody("noOfSticks" -> "5")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/tobacco/cigarettes/currency/0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = List(
        PurchasedProduct(Some(ProductPath("tobacco/cigarettes")), purchasedProductInstances = List(
          PurchasedProductInstance(index = 0, noOfSticks = Some(5)))))
      )))(any())
    }
  }

  "Calling POST /products/tobacco/.../no-of-sticks-weight" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= None
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigars/no-of-sticks-weight/0")
        .withFormUrlEncodedBody("noOfSticks" -> "NaN", "weight" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData())
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigars/no-of-sticks-weight/0")
        .withFormUrlEncodedBody("noOfSticks" -> "5", "weight" -> "30.2")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/tobacco/cigars/currency/0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = List(
        PurchasedProduct(Some(ProductPath("tobacco/cigars")), purchasedProductInstances = List(
          PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(0.0302)), noOfSticks = Some(5))))
      ))))(any())
    }
  }

  "Calling POST /products/tobacco/.../weight" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= None
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/rolling/weight/0")
        .withFormUrlEncodedBody("weight" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData())
      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/rolling/weight/0")
        .withFormUrlEncodedBody("weight" -> "30.2")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/tobacco/rolling/currency/0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = List(
        PurchasedProduct(Some(ProductPath("tobacco/rolling")), purchasedProductInstances = List(
          PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(0.0302)))))
      ))))(any())
    }
  }

  "Calling GET /products/tobacco/.../currency/0" should {

    // TODO: Revise after refactor controller

    "return a 404 when the product path is not found" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= None
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco/not/a/real/product/currency/0")).get

      status(result) shouldBe NOT_FOUND

      verify(injected[LocalSessionCache], times(0)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when cached JourneyData: Option[JourneyData]exists" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5)))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco/cigars/currency/0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/tobacco/.../currency/0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5)))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigars/currency/0")
        .withFormUrlEncodedBody("currency" -> "Not a currency code")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5)))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigars/currency/0")
        .withFormUrlEncodedBody("currency" -> "USD")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/bc-passengers-frontend/products/tobacco/cigars/cost/0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5), currency = Some("USD"))))))))(any())
    }
  }

  "Calling GET /products/tobacco/.../cost/0" should {

    "return a 500 when there is no cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None
      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco/cigars/cost/0")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 500 when there is no currency in the cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5)))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco/cigars/cost/0")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when there is a currency in the cached journey data for this product instance" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5), currency = Some("USD")))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco/cigars/cost/0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/tobacco/.../cost/0" should {

    "return a 500 when given a bad form input and there is no cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = None

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigars/cost/0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 500 when given bad form input and there is no currency in the cached journey data" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5)))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigars/cost/0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 400 when given bad form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5), currency = Some("USD")))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigars/cost/0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 when given valid form input" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData]= Some(JourneyData(purchasedProducts = List(PurchasedProduct(Some(ProductPath("tobacco/cigars")),
        purchasedProductInstances = List(PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5), currency = Some("USD")))))))

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/tobacco/cigars/cost/0")
        .withFormUrlEncodedBody("cost" -> "9.99")).get

      status(result) shouldBe SEE_OTHER

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(JourneyData(purchasedProducts = List(
        PurchasedProduct(Some(ProductPath("tobacco/cigars")), purchasedProductInstances = List(
          PurchasedProductInstance(index = 0, weightOrVolume = Some(BigDecimal(30.2)), noOfSticks = Some(5), currency = Some("USD"), cost = Some(BigDecimal(9.99)))))))))(any())
    }
  }
}
