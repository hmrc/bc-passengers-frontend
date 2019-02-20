package vertical

import models._
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{LimitUsageResponse, LimitUsageSuccessResponse, LocalSessionCache}

import scala.concurrent.Future

class AlcoholVerticalSpec extends VerticalBaseSpec {

  val requiredJourneyData = JourneyData(euCountryCheck = Some("nonEuOnly"), ageOver17 = Some(true), isVatResClaimed = None, bringingDutyFree = None,  privateCraft = Some(false))

  "Calling GET /products/alcohol/.../start" should {

    "clear the working instance and redirect to the volume inout" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/start")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/[a-zA-Z0-9]{6}$""".r

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling GET /products/alcohol/.../volume" should {

    "return a 200" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/alcohol/.../volume" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")
        .withFormUrlEncodedBody("volume" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())

    }

    "redirect the user to the country input, storing weightOrVolume given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")
        .withFormUrlEncodedBody("volume" -> "2.5")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/country/iid0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(
        meq(requiredJourneyData.copy(workingInstance = Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5")))))))(any())
    }

    "return a 303 when volume value is just under the calculator limit" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-BEER" -> "0.99"))
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")
        .withFormUrlEncodedBody("volume" -> "109")).get

      status(result) shouldBe SEE_OTHER

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(
        meq(requiredJourneyData.copy(workingInstance = Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("109")))))))(any())
    }

    "return a 303 when volume value is exactly the calculator limit" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-BEER" -> "1.00"))
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")
        .withFormUrlEncodedBody("volume" -> "110")).get

      status(result) shouldBe SEE_OTHER

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(
        meq(requiredJourneyData.copy(workingInstance = Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("110")))))))(any())
    }

    "return a 400 when volume value exceeds calculator limit" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-BEER" -> "1.01"))
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")
        .withFormUrlEncodedBody("volume" -> "111")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())

    }
  }

  "Calling GET /products/alcohol/.../country/iid0" should {

    "return a 404 when the product path is invalid" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/not/a/real/product/country/iid0")).get

      status(result) shouldBe NOT_FOUND

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when the product path is valid and there is a working instance with a volume" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/wine"), iid = "iid0", weightOrVolume = Some(BigDecimal(2.00))))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/wine/country/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/alcohol/.../country/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/wine"), iid = "iid0", weightOrVolume = Some(BigDecimal(2.00))))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/wine/country/iid0").withFormUrlEncodedBody("someWrongKey" -> "someWrongValue", "itemsRemaining" -> "1")).get

      status(result) shouldBe BAD_REQUEST
      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "store the country in the working product, and redirect to the cost input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/wine/country/iid0").withFormUrlEncodedBody("country" -> "Egypt", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/wine/currency/iid0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/wine"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Nil)))))))(any())
    }
  }

  "Calling GET /products/alcohol/.../currency/iid0" should {

    "redirect to dashboard when the product path is not found" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/not/a/real/product/currency/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when cached JourneyData exists" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5"))))))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/currency/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/alcohol/.../currency/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5"))))))

      val result  = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/currency/iid0")
        .withFormUrlEncodedBody("currency" -> "Not a currency code")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }


    "redirect the user to the cost input, storing the currency in the working instance, given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5"))))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/currency/iid0")
        .withFormUrlEncodedBody("currency" -> "USA dollars (USD)")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal(2.5)), currency = Some("USD")))
      )))(any())
    }
  }

  "Calling GET /products/alcohol/.../cost/iid0" should {

    "start a new session when there is no cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = None

      val result = route(app, EnhancedFakeRequest("GET",
        "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "redirect to dashboard when there is no currency in the cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5"))))
      ))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 200 when there is a currency in the cached journey data for this product instance" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5")), currency = Some("USD")))
      ))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")).get

      status(result) shouldBe OK

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }
  }

  "Calling POST /products/alcohol/.../cost/iid0" should {

    "start a new session when given a bad form input and there is no cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = None

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")
          .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return user to dashboard when given bad form input and there is no currency in the cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5"))))
      ))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())
    }

    "return a 400 when given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5")), currency = Some("USD")))
      ))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(0)).cacheJourneyData(any())(any())

    }

    "store the cost and move the working instance to purchased products, then redirect the user to the next step when given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5")), currency = Some("USD"), country = Some(Country("USA", "US", false, Nil))))
      ))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "5.99")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/next-step")

      verify(injected[LocalSessionCache], times(1)).fetchAndGetJourneyData(any())
      verify(injected[LocalSessionCache], times(1)).cacheJourneyData(meq(requiredJourneyData.copy(
        purchasedProductInstances = List(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("2.5")), currency = Some("USD"), country = Some(Country("USA", "US", false, Nil)), cost = Some(BigDecimal("5.99"))))))
      )(any())
    }
  }
}
