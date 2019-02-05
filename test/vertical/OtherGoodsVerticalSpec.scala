package vertical

import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{LimitUsageSuccessResponse, LocalSessionCache}

import scala.concurrent.Future

class OtherGoodsVerticalSpec extends VerticalBaseSpec {

  val requiredJourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))

  "Calling GET /products/other-goods/.../quantity" should {

    "return a 200" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/quantity")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/other-goods/.../quantity" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect the user to the country input with ir set, given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "2")).get

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/country/[a-zA-Z0-9]{6}[?]ir=2$""".r

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling GET /products/other-goods/.../country/iid0?ir=1" should {

    "return a 404 when the product path is invalid" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/not/a/real/product/country/iid0?ir=1")).get

      status(result) shouldBe NOT_FOUND

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when the product path is valid" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/country/iid0?ir=1")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/other-goods/.../country/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/country/iid0?ir=1").withFormUrlEncodedBody("someWrongKey" -> "someWrongValue", "itemsRemaining" -> "1")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "store the country in the working product, and redirect to the cost input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/country/iid0?ir=1").withFormUrlEncodedBody("country" -> "Egypt", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/currency/iid0?ir=1")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Some("EGP"), Nil)))))))(any())
    }

    "redirect dashboard given existing journey data for an item and valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        purchasedProductInstances = List(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", country = Some(Country("Jamaica", "JM", isEu = false, Some("JMD"), Nil)), currency = Some("JMD"), cost = Some(20.0))),
        workingInstance = Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", country = Some(Country("Jamaica", "JM", isEu = false, Some("JMD"), Nil)), currency = Some("JMD"), cost = Some(20.0)))
      ))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/country/iid0?ir=1").withFormUrlEncodedBody("country" -> "Egypt", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(
        purchasedProductInstances = List(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Some("EGP"), Nil)), currency = Some("JMD"), cost = Some(20.0))),
        workingInstance = Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", country = Some(Country("Jamaica", "JM", isEu = false, Some("JMD"), Nil)), currency = Some("JMD"), cost = Some(20.0))))))(any())
    }
  }


  "Calling GET /products/other-goods/.../currency/iid0?ir=1" should {

    "return a 404 when the product path is invalid" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/not/a/real/product/currency/iid0?ir=1")).get

      status(result) shouldBe NOT_FOUND
      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when cached JourneyData exists" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/currency/iid0?ir=1")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/other-goods/.../currency/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/currency/iid0").withFormUrlEncodedBody("currency" -> "Not a currency code")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "store the currency in the working product, and redirect to the cost input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/currency/iid0").withFormUrlEncodedBody("currency" -> "USD", "itemsRemaining" -> "6")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0?ir=6")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD"))))))(any())
    }
  }

  "Calling GET /products/other-goods/.../cost/iid0" should {

    "start new session when there is no cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = None
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0?ir=1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect to the dashboard when there is no currency in the working instance" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        workingInstance = Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = None))
      ))
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0?ir=1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when there is a currency in the working instance for this product instance" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        workingInstance = Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD")))
      ))
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0?ir=1")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/other-goods/.../cost/iid0" should {

    "return a 500 when given bad form input and there is no cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = None
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 500 when given bad form input and there is no currency in the working instance" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        workingInstance = Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = None))
      ))
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        workingInstance = Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD")))
      ))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "store the cost, move the working product to purchased products and redirect to the currency page passing ir = 1 when items remaining was 2 and given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD"), country = Some(Country("USA", "US", false, Some("USA"), Nil))))))
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0").withFormUrlEncodedBody("cost" -> "2.99", "itemsRemaining" -> "2")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/country/[a-zA-Z0-9]{6}[?]ir=1$""".r


      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(purchasedProductInstances =
        List(PurchasedProductInstance(ProductPath("other-goods/books"), "iid0", currency = Some("USD"), country = Some(Country("USA", "US", false, Some("USA"), Nil)), cost = Some(BigDecimal("2.99")))))))(any())

    }

    "store the cost, move the working product to purchased products and redirect to the next step when items remaining was 1 and given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD"), country = Some(Country("USA", "US", false, Some("USA"), Nil))))))
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0").withFormUrlEncodedBody("cost" -> "2.99", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/next-step")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(purchasedProductInstances =
        List(PurchasedProductInstance(ProductPath("other-goods/books"), "iid0", currency = Some("USD"), country = Some(Country("USA", "US", false, Some("USA"), Nil)), cost = Some(BigDecimal("2.99")))))))(any())

    }

    "store the cost, replace the working product in purchased products if it already existed and redirect to the next step when items remaining was 1 and given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        purchasedProductInstances = List(PurchasedProductInstance(ProductPath("other-goods/books"), "iid0", currency = Some("USD"), cost = Some(BigDecimal("0.99")), country = Some(Country("USA", "US", false, Some("USA"), Nil)))),
        workingInstance = Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0", currency = Some("USD"), country = Some(Country("USA", "US", false, Some("USA"), Nil))))
      ))
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0").withFormUrlEncodedBody("cost" -> "2.99", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/next-step")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(purchasedProductInstances =
        List(PurchasedProductInstance(ProductPath("other-goods/books"), "iid0", currency = Some("USD"), cost = Some(BigDecimal("2.99")), country = Some(Country("USA", "US", false, Some("USA"), Nil)))))))(any())

    }

    "store the cost, replace the working product in purchased products (maintaining order) if it already existed and redirect to the next step when items remaining was 1 and given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(
        purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("other-goods/books"), "iid0", currency = Some("USD"), cost = Some(BigDecimal("0.99")), country = Some(Country("USA", "US", false, Some("USA"), Nil))),
          PurchasedProductInstance(ProductPath("other-goods/books"), "iid1", currency = Some("USD"), cost = Some(BigDecimal("0.99")), country = Some(Country("USA", "US", false, Some("USA"), Nil))),
          PurchasedProductInstance(ProductPath("other-goods/books"), "iid2", currency = Some("USD"), cost = Some(BigDecimal("0.99")), country = Some(Country("USA", "US", false, Some("USA"), Nil)))
        ),
        workingInstance = Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid1", currency = Some("USD"), country = Some(Country("USA", "US", false, Some("USA"), Nil))))
      ))
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid1").withFormUrlEncodedBody("cost" -> "2.99", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/next-step")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("other-goods/books"), "iid0", currency = Some("USD"), cost = Some(BigDecimal("0.99")), country = Some(Country("USA", "US", false, Some("USA"), Nil))),
        PurchasedProductInstance(ProductPath("other-goods/books"), "iid1", currency = Some("USD"), cost = Some(BigDecimal("2.99")), country = Some(Country("USA", "US", false, Some("USA"), Nil))),
        PurchasedProductInstance(ProductPath("other-goods/books"), "iid2", currency = Some("USD"), cost = Some(BigDecimal("0.99")), country = Some(Country("USA", "US", false, Some("USA"), Nil)))
      ))))(any())

    }

    "not store any changes when trying to store the cost without having the currency already in the working product" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("other-goods/books"), iid = "iid0"))))
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/cost/iid0").withFormUrlEncodedBody("cost" -> "2.99", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/next-step")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())

    }
  }
}
