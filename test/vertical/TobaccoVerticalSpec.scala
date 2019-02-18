package vertical

import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{times, verify}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{LimitUsageSuccessResponse, LocalSessionCache}

import scala.concurrent.Future

class TobaccoVerticalSpec extends VerticalBaseSpec {

  val requiredJourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))

  "Calling GET /products/tobacco/.../start" should {

    "redirect the user to the relevant start form" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/[a-zA-Z0-9]{6}$""".r

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/no-of-sticks-weight/[a-zA-Z0-9]{6}$""".r

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarillos/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarillos/no-of-sticks-weight/[a-zA-Z0-9]{6}$""".r

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/[a-zA-Z0-9]{6}$""".r

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/chewing/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/chewing/weight/[a-zA-Z0-9]{6}$""".r

      verify(injected[LocalSessionCache], times(5)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling GET /products/tobacco/.../no-of-sticks-weight" should {

    "return a 200" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/no-of-sticks-weight/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling GET /products/tobacco/.../no-of-sticks" should {

    "return a 200" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling GET /products/tobacco/.../weight" should {

    "return a 200" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }


  "Calling POST /products/tobacco/.../no-of-sticks" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return 400 when the calculator limit is exceeded" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-CIGRT" -> "1.01"))
      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "801")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect the user to the country input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "5")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/country/iid0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", noOfSticks = Some(5)))
      )))(any())
    }
  }

  "Calling POST /products/tobacco/.../no-of-sticks-weight" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)
      
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/no-of-sticks-weight/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "NaN", "weight" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return 400 when the calculator limit is exceeded" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-CIGAR" -> "1.02"))
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/no-of-sticks-weight/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "51", "weight" -> "30.2")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect the user to the country input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/no-of-sticks-weight/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "5", "weight" -> "30.2")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/country/iid0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", noOfSticks = Some(5), weightOrVolume = Some(BigDecimal("0.0302"))))
      )))(any())
    }
  }

  "Calling POST /products/tobacco/.../weight" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)
      
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid0")
        .withFormUrlEncodedBody("weight" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return 400 when the calculator limit is exceeded" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-LOOSE" -> "1.001"))
      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid0")
        .withFormUrlEncodedBody("weight" -> "1001")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect the user to the country input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData)
      
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid0")
        .withFormUrlEncodedBody("weight" -> "30.2")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/country/iid0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/rolling"), iid = "iid0", weightOrVolume = Some(BigDecimal("0.0302"))))
      )))(any())
    }
  }

  "Calling GET /products/tobacco/.../country/iid0" should {

    "return a 404 when the product path is invalid" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/not/a/real/product/country/iid0")).get

      status(result) shouldBe NOT_FOUND

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when the product path is valid" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal(10.00))))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/country/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/tobacco/.../country/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal(10.00))))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/country/iid0").withFormUrlEncodedBody("someWrongKey" -> "someWrongValue", "itemsRemaining" -> "1")).get

      status(result) shouldBe BAD_REQUEST
      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "store the country in the working product, and redirect to the cost input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/country/iid0").withFormUrlEncodedBody("country" -> "Egypt", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/currency/iid0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Nil)))))))(any())
    }
  }

  "Calling GET /products/tobacco/.../currency/iid0" should {

    "return a 404 when the product path is not found" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/not/a/real/product/currency/iid0")).get

      status(result) shouldBe NOT_FOUND

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when cached JourneyData exists" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/currency/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/tobacco/.../currency/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/currency/iid0")
        .withFormUrlEncodedBody("currency" -> "Not a currency code")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/currency/iid0")
        .withFormUrlEncodedBody("currency" -> "USA dollars (USD)", "itemsRemaining" -> "5")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/cost/iid0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD")))
      )))(any())
    }
  }

  "Calling GET /products/tobacco/.../cost/iid0" should {

    "start new sesison when there is no cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = None
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect to dashboard when there is no currency in the cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when there is a currency in the cached journey data for this product instance" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD")))))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/cost/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/tobacco/.../cost/iid0" should {

    "start new session when given a bad form input and there is no cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = None

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect to dashboard when given bad form input and there is no currency in the cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get


      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 400 when given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD")))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 303 when given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD"), country = Some(Country("USA", "US", false, Nil))))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "9.99")).get

      status(result) shouldBe SEE_OTHER

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD"), country = Some(Country("USA", "US", false, Nil)), cost = Some(BigDecimal("9.99")))))))(any())
    }
  }
}
