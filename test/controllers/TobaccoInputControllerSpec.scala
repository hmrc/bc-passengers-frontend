package controllers

import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{route => rt, _}
import services.{CalculatorService, LimitUsageSuccessResponse, PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future
import scala.language.postfixOps

class TobaccoInputControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[CalculatorService].toInstance(MockitoSugar.mock[CalculatorService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService])
  }

  val controller: TobaccoInputController = app.injector.instanceOf[TobaccoInputController]

  trait LocalSetup {

    val requiredJourneyData = JourneyData(euCountryCheck = Some("nonEuOnly"), ageOver17 = Some(true), privateCraft = Some(false))
    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData())
      when(injected[PurchasedProductService].cacheJourneyData(any())(any())) thenReturn Future.successful(CacheMap("fakeId", Map.empty))
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[CalculatorService].limitUsage(any())(any())) thenReturn Future.successful(LimitUsageSuccessResponse(Map.empty))

      rt(app, req)
    }
  }

  "Calling GET /products/tobacco/.../no-of-sticks" should {

    "return a 200 and not prepopulate the noOfSticks value if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=noOfSticks]").attr("value") shouldBe empty
    }

    "return a 200 with the noOfSticks value populated if there is a working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", noOfSticks = Some(5)))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=noOfSticks]").attr("value") shouldBe "5"
    }
  }

  "Calling GET /products/tobacco/.../no-of-sticks-weight" should {

    "return a 200 and not prepopulate the noOfSticks and weight values if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/no-of-sticks-weight/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=noOfSticks]").attr("value") shouldBe empty
    }

    "return a 200 with the noOfSticks and weight values populated if there is a working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)), noOfSticks = Some(5)))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/no-of-sticks-weight/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=noOfSticks]").attr("value") shouldBe "5"
      doc.select("input[name=weight]").attr("value") should not be empty
    }
  }

  "Calling GET /products/tobacco/.../weight" should {

    "return a 200 and not prepopulate the weight value if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=weight]").attr("value") shouldBe empty
    }

    "return a 200 with the weight value populated if there is a working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5))))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=weight]").attr("value") should not be empty
    }
  }

  "Calling GET /products/tobacco/.../country" should {

    "return a 200 and not prepopulate the country value if there is no working instance country" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal(10.00))))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/country/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.getElementsByAttributeValue("selected", "selected").attr("value") shouldBe empty
    }
  }

  "Calling GET /products/tobacco/.../country/<iid>/update" should {

    "redirect to the country input page and add the purchased product to the working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances =
        List(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0",  weightOrVolume = Some(BigDecimal("20")), country = Some(Country("Jamaica", "JM", isEu = false, Nil))))))

      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigars"), iid = "iid0", weightOrVolume = Some(BigDecimal("20")), country = Some(Country("Jamaica", "JM", isEu = false, Nil))))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/country/iid0/update")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigars/country/iid0")

      verify(injected[PurchasedProductService], times(1)).makeWorkingInstance(any(), any())(any(), any())
    }
  }


  "Calling GET /products/tobacco/.../currency" should {

    "redirect to the dashboard if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/currency/iid0")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")
    }

    "return a 200 with the currency value populated if there is a working instance currency" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", currency = Some("JMD")))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/currency/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.getElementById("currency-Jamaican dollars (JMD)").outerHtml should include ("""<option id="currency-Jamaican dollars (JMD)" value="Jamaican dollars (JMD)" selected>Jamaican dollars (JMD)</option>""")
    }
  }

  "Calling GET /products/tobacco/.../currency/<iid>/update" should {
    "redirect to the input page and add the purchased product to the working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", currency = Some("JMD")))
      ))

      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", currency = Some("JMD"))))
      )

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/currency/iid0/update")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/currency/iid0")

      verify(injected[PurchasedProductService], times(1)).makeWorkingInstance(any(), any())(any(), any())
    }
  }

  "Calling GET /products/tobacco/.../cost" should {

    "redirect to the dashboard if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/currency/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)


      Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")
    }

    "return a 200 with the cost value populated if there is a working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)), currency = Some("JMD"), cost = Some(BigDecimal(200.80)))
        )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/cost/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=cost]").attr("value") shouldBe "200.8"
    }
  }

  "Calling GET /products/tobacco/.../update/<iid>" should {

    "redirect to the noOfSticks input page when changing cigarettes" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)), currency = Some("JMD"), cost = Some(BigDecimal(200.80)))
        )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/iid0/update")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid0")
    }
    "redirect to the noOfSticksWeight input page when changing cigarillos or cigars" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)), currency = Some("JMD"), cost = Some(BigDecimal(200.80)))
        )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarillos/iid0/update")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarillos/no-of-sticks-weight/iid0")
    }
    "redirect to the weight input page when changing rolling or pipe/rolling tobacco" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)), currency = Some("JMD"), cost = Some(BigDecimal(200.80)))
        )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/iid0/update")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid0")
    }
  }

  "Calling POST /products/tobacco/cigarettes/no-of-sticks/<iid>" should {

    "redirect to the dashboard when the iid already exists in the working instance" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid1", None, Some(100), Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid1")
      .withFormUrlEncodedBody("noOfSticks" -> "200")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")
    }

    "redirect to the country input when the iid does not match the working instance" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("tobacco/cigarettes"), "iid2", Some(BigDecimal("4.0")), None)
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/no-of-sticks/iid1")
        .withFormUrlEncodedBody("noOfSticks" -> "200")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarettes/country/iid1")
    }
  }

  "Calling POST /products/tobacco/rolling/weight/<iid>" should {

    "redirect to the dashboard when the iid already exists in the working instance" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("tobacco/rolling"), "iid1", Some(BigDecimal("0.50")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid1")
        .withFormUrlEncodedBody("weight" -> "1000")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

    }

    "redirect to the country input when the iid is not in the cached journey data" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("other-goods/jewellery"), "iid0", None, None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99"))),
        PurchasedProductInstance(ProductPath("tobacco/rolling"), "iid1", Some(BigDecimal("0.25")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/weight/iid2")
        .withFormUrlEncodedBody("weight" -> "600")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/rolling/country/iid2")
    }
  }

  "Calling POST /products/tobacco/cigarillos/no-of-sticks-weight/<iid>" should {

    "redirect to the dashboard when the iid already exists in the working instance" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("tobacco/cigarillos"), "iid1", Some(BigDecimal("0.50")), Some(1000), Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarillos/no-of-sticks-weight/iid1")
        .withFormUrlEncodedBody("noOfSticks" -> "500", "weight" -> "125")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

    }

    "redirect to the country input when the iid is not in the cached journey data" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("other-goods/jewellery"), "iid0", None, None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99"))),
        PurchasedProductInstance(ProductPath("tobacco/cigarillos"), "iid1", Some(BigDecimal("0.25")), Some(1000), Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarillos/no-of-sticks-weight/iid2")
        .withFormUrlEncodedBody("noOfSticks" -> "500", "weight" -> "125")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/tobacco/cigarillos/country/iid2")
    }
  }
}
