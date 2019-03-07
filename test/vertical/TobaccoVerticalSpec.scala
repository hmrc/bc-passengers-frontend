package vertical

import connectors.Cache
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{times, verify}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.LimitUsageSuccessResponse

import scala.concurrent.Future

class TobaccoVerticalSpec extends VerticalBaseSpec {

  val requiredJourneyData = JourneyData(Some("nonEuOnly"), ageOver17 = Some(true), isVatResClaimed = None, bringingDutyFree = None, privateCraft = Some(false))

  "Calling GET /select-goods/tobacco/.../start" should {

    "redirect the user to the relevant start form" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/no-of-sticks/[a-zA-Z0-9]{6}$""".r

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/no-of-sticks-weight/[a-zA-Z0-9]{6}$""".r

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarillos/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarillos/no-of-sticks-weight/[a-zA-Z0-9]{6}$""".r

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/rolling-tobacco/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/rolling-tobacco/weight/[a-zA-Z0-9]{6}$""".r

      redirectLocation(route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/chewing-tobacco/start")).get).get should
        fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/chewing-tobacco/weight/[a-zA-Z0-9]{6}$""".r

      verify(injected[Cache], times(5)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }
  }

  "Calling GET /select-goods/tobacco/.../no-of-sticks-weight" should {

    "return a 200" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/no-of-sticks-weight/iid0")).get

      status(result) shouldBe OK

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }
  }

  "Calling GET /select-goods/tobacco/.../no-of-sticks" should {

    "return a 200" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/no-of-sticks/iid0")).get

      status(result) shouldBe OK

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }
  }

  "Calling GET /select-goods/tobacco/.../weight" should {

    "return a 200" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/rolling-tobacco/weight/iid0")).get

      status(result) shouldBe OK

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }
  }


  "Calling POST /select-goods/tobacco/.../no-of-sticks" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/no-of-sticks/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return 400 when the calculator limit is exceeded" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-CIGRT" -> "1.01"))
      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/no-of-sticks/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "801")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "redirect the user to the country input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/no-of-sticks/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "5")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/country/iid0")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(1)).store(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", noOfSticks = Some(5)))
      )))(any())
    }
  }

  "Calling POST /select-goods/tobacco/.../no-of-sticks-weight" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)
      
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/no-of-sticks-weight/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "NaN", "weight" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return 400 when the calculator limit is exceeded" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-CIGAR" -> "1.02"))
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/no-of-sticks-weight/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "51", "weight" -> "30.2")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "redirect the user to the country input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/no-of-sticks-weight/iid0")
        .withFormUrlEncodedBody("noOfSticks" -> "5", "weight" -> "30.2")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/country/iid0")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(1)).store(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", noOfSticks = Some(5), weightOrVolume = Some(BigDecimal("0.0302"))))
      )))(any())
    }
  }

  "Calling POST /select-goods/tobacco/.../weight" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = Some(requiredJourneyData)
      
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/rolling-tobacco/weight/iid0")
        .withFormUrlEncodedBody("weight" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return 400 when the calculator limit is exceeded" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map("L-LOOSE" -> "1.001"))
      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/rolling-tobacco/weight/iid0")
        .withFormUrlEncodedBody("weight" -> "1001")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "redirect the user to the country input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData)
      
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/rolling-tobacco/weight/iid0")
        .withFormUrlEncodedBody("weight" -> "30.2")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/rolling-tobacco/country/iid0")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(1)).store(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/rolling-tobacco"), iid = "iid0", weightOrVolume = Some(BigDecimal("0.0302"))))
      )))(any())
    }
  }

  "Calling GET /select-goods/tobacco/.../country/iid0" should {

    "return a 404 when the product path is invalid" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/not/a/real/product/country/iid0")).get

      status(result) shouldBe NOT_FOUND

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return a 200 when the product path is valid" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal(10.00))))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/rolling-tobacco/country/iid0")).get

      status(result) shouldBe OK

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }
  }

  "Calling POST /select-goods/tobacco/.../country/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal(10.00))))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/country/iid0").withFormUrlEncodedBody("someWrongKey" -> "someWrongValue", "itemsRemaining" -> "1")).get

      status(result) shouldBe BAD_REQUEST
      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "store the country in the working product, and redirect to the cost input page given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)
      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/country/iid0").withFormUrlEncodedBody("country" -> "Egypt", "itemsRemaining" -> "1")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigarettes/currency/iid0")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(1)).store(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Nil)))))))(any())
    }
  }

  "Calling GET /select-goods/tobacco/.../currency/iid0" should {

    "return a 404 when the product path is not found" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/not/a/real/product/currency/iid0")).get

      status(result) shouldBe NOT_FOUND

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return a 200 when cached JourneyData exists" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/currency/iid0")).get

      status(result) shouldBe OK

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }
  }

  "Calling POST /select-goods/tobacco/.../currency/iid0" should {

    "return a 400 given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/currency/iid0")
        .withFormUrlEncodedBody("currency" -> "Not a currency code")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return a 303 given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/currency/iid0")
        .withFormUrlEncodedBody("currency" -> "USA dollars (USD)", "itemsRemaining" -> "5")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/cost/iid0")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(1)).store(meq(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD")))
      )))(any())
    }
  }

  "Calling GET /select-goods/tobacco/.../cost/iid0" should {

    "start new sesison when there is no cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = None
      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "redirect to dashboard when there is no currency in the cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/cost/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return a 200 when there is a currency in the cached journey data for this product instance" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD")))))

      val result = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/cost/iid0")).get

      status(result) shouldBe OK

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }
  }

  "Calling POST /select-goods/tobacco/.../cost/iid0" should {

    "start new session when given a bad form input and there is no cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData  = None

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/new-session")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "redirect to dashboard when given bad form input and there is no currency in the cached journey data" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5)))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get


      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return a 400 when given bad form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD")))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "NaN")).get

      status(result) shouldBe BAD_REQUEST

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(0)).store(any())(any())
    }

    "return a 303 when given valid form input" in new LocalSetup {

      override lazy val limitUsageResponse = LimitUsageSuccessResponse(Map.empty)
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD"), country = Some(Country("USA", "US", false, Nil))))))

      val result = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/select-goods/tobacco/cigars/cost/iid0")
        .withFormUrlEncodedBody("cost" -> "9.99")).get

      status(result) shouldBe SEE_OTHER

      verify(injected[Cache], times(1)).fetch(any())
      verify(injected[Cache], times(1)).store(meq(requiredJourneyData.copy(purchasedProductInstances = List(
          PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("30.2")), noOfSticks = Some(5), currency = Some("USD"), country = Some(Country("USA", "US", false, Nil)), cost = Some(BigDecimal("9.99")))))))(any())
    }
  }
}
