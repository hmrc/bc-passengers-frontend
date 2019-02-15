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

class AlcoholInputControllerSpec extends BaseSpec {

  val requiredJourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[CalculatorService].toInstance(MockitoSugar.mock[CalculatorService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService])
  }

  trait LocalSetup {

    def cachedJourneyData: Option[JourneyData]

    val requiredJourneyData: JourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {
      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData())

      when(injected[PurchasedProductService].cacheJourneyData(any())(any())) thenReturn Future.successful(CacheMap("fakeId", Map.empty))
      when(injected[PurchasedProductService].storeCountry(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(JourneyData())
      when(injected[PurchasedProductService].updateCountry(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(JourneyData())
      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn Future.successful(cachedJourneyData)
      when(injected[CalculatorService].limitUsage(any())(any())) thenReturn Future.successful(LimitUsageSuccessResponse(Map.empty))

      rt(app, req)
    }
  }

  val controller: AlcoholInputController = app.injector.instanceOf[AlcoholInputController]

  "Calling GET /products/alcohol/.../volume" should {

    "return a 200 with and not prepopulate the volume value if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=volume]").attr("value") shouldBe empty
    }

    "return a 200 with the volume value populated if there is a working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=volume]").attr("value") should not be empty
    }
  }

  "Calling GET /products/alcohol/.../volume/<iid>/update" should {
    "redirect to the input page and add the purchased product to the working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("20"))))
      ))

      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("20"))))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0/update")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid0")

      verify(injected[PurchasedProductService], times(1)).makeWorkingInstance(any(), any())(any(), any())
    }
  }

  "Calling GET /products/alcohol/.../country" should {

    "return a 200 and not prepopulate the country value if there is no working instance country" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/wine"), iid = "iid0",  weightOrVolume = Some(BigDecimal(20.5))))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/wine/country/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.getElementsByAttributeValue("selected", "selected").attr("value") shouldBe empty
    }

    "return a 200 and prepopulate the country value if there is a working instance country" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/wine"), iid = "iid0",  weightOrVolume = Some(BigDecimal(20.5)), country = Some(Country("Jamaica", "JM", isEu = false, Nil))))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/wine/country/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.getElementById("country-Jamaica").outerHtml should include("""<option id="country-Jamaica" value="Jamaica" selected>Jamaica</option>""")
    }

  }

  "Calling GET /products/alcohol/.../country/<iid>/update" should {

    "redirect to the country input page and add the purchased product to the working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances =
        List(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0",  weightOrVolume = Some(BigDecimal("20")), country = Some(Country("Jamaica", "JM", isEu = false, Nil))))))

      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData(workingInstance =
        Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal("20")), country = Some(Country("Jamaica", "JM", isEu = false, Nil))))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/country/iid0/update")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/country/iid0")

      verify(injected[PurchasedProductService], times(1)).makeWorkingInstance(any(), any())(any(), any())
    }
  }

  "Calling GET /products/alcohol/.../currency" should {

    "return a 200 with and not prepopulate if there is no currency in the working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/currency/iid1")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.getElementsByAttributeValue("selected", "selected").attr("value") shouldBe empty
    }

    "return a 200 with the currency value populated if there is a working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/cider"), iid = "iid1", currency = Some("USD"), weightOrVolume = Some(BigDecimal(20.4))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", currency = Some("JMD"), weightOrVolume = Some(BigDecimal(20.5)))),
        workingInstance = Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", currency = Some("JMD"), weightOrVolume = Some(BigDecimal(20.5))))
      ))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/currency/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.getElementById("currency-Jamaican dollars (JMD)").outerHtml should include("""<option id="currency-Jamaican dollars (JMD)" value="Jamaican dollars (JMD)" selected>Jamaican dollars (JMD)</option>""")
    }
  }

  "Calling GET /products/alcohol/.../cost" should {

    "return a 200 with and not prepopulate the cost value there is no cost in the working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)))
      )))


      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/currency/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.getElementsByAttributeValue("selected", "selected").attr("value") shouldBe empty
    }

    "return a 200 with the cost value populated if there is a working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/cider"), iid = "iid1", weightOrVolume = Some(BigDecimal(20.4)), currency = Some("USD"),  cost = Some(BigDecimal(200.40))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)), currency = Some("JMD"), cost = Some(BigDecimal(200.80)))),
        workingInstance = Some(PurchasedProductInstance(ProductPath("alcohol/beer"), iid = "iid0", weightOrVolume = Some(BigDecimal(20.5)), currency = Some("JMD"), cost = Some(BigDecimal(200.80)))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/cost/iid0")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      doc.select("input[name=cost]").attr("value") shouldBe "200.8"
    }
  }

  "Calling GET /products/alcohol/.../start" should {

    "redirect to the volume input page" in new LocalSetup {
      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/cider/start")).get


      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/cider/volume/[a-zA-Z0-9]{6}$""".r
    }
  }

  "Calling POST /products/alcohol/beer/volume/<iid>" should {

    "redirect to the dashboard when the iid already exists" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("500")), None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid1")
        .withFormUrlEncodedBody("volume" -> "20")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

    }

    "redirect to the country input when the iid is not in the cached journey data" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/cider"), "iid0", None, None, Some(Country("Egypt", "EG", isEu = false, Nil)), Some("USD"), Some(BigDecimal("12.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/volume/iid1")
        .withFormUrlEncodedBody("volume" -> "20")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/country/iid1")
    }
  }

  "Calling POST /products/alcohol/beer/country/<iid>" should {

    "redirect to the dashboard when the iid already exists" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(workingInstance = Some(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("500")), None, Some(Country("Jamaica", "JM", isEu = false, Nil)), Some("USD"), Some(BigDecimal("4.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/country/iid1")
        .withFormUrlEncodedBody("country" -> "Afghanistan")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

    }

    "redirect to the currency input when the iid is not in the cached journey data" in new LocalSetup {
      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances = List(
        PurchasedProductInstance(ProductPath("alcohol/cider"), "iid0", None, None, None, None, Some(BigDecimal("12.99")))
      )))

      val result: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/country/iid1")
        .withFormUrlEncodedBody("country" -> "Canada")).get

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/alcohol/beer/currency/iid1")
    }
  }
}
