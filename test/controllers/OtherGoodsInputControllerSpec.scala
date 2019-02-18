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
import services.{PurchasedProductService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future


class OtherGoodsInputControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[PurchasedProductService])
  }

  trait LocalSetup {

    def requiredJourneyData: JourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))
    def cachedJourneyData: Option[JourneyData]

    def result: Future[Result]
    def content: String = contentAsString(result)
    def doc: Document = Jsoup.parse(content)

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData())

      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn {
        Future.successful(cachedJourneyData)
      }

      rt(app, req)
    }
  }

  "Calling GET /products/other-goods/.../currency" should {

    "return a 200 and not prepopulate the currency value if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/jewellery/currency/iid0?ir=0")).get

      status(result) shouldBe OK

      doc.getElementsByAttributeValue("selected", "selected").attr("value") shouldBe empty
    }
  }

  "Calling GET /products/other-goods/.../currency/<iid>/update" should {

    "redirect to the currency input page and add the purchased product to the working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances =
        List(PurchasedProductInstance(ProductPath("other-goods/jewellery"), iid = "iid0", currency = Some("JMD")))))

      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData(workingInstance =
        Some(PurchasedProductInstance(ProductPath("other-goods/jewellery"), iid = "iid0", currency = Some("JMD"))))
      )

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/jewellery/currency/iid0/update")).get

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/jewellery/currency/iid0?ir=0")

      verify(injected[PurchasedProductService], times(1)).makeWorkingInstance(any(), any())(any(), any())
    }
  }

  "Calling GET /products/other-goods/.../country" should {

    "return a 200 and not prepopulate the country value if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/jewellery/country/iid0?ir=0")).get

      status(result) shouldBe OK

      doc.getElementsByAttributeValue("selected", "selected").attr("value") shouldBe empty
    }
  }

  "Calling GET /products/other-goods/.../country/<iid>/update" should {

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


  "Calling GET /products/other-goods/.../quantity" should {

    "return a 200 with a quantity input view given a correct product path" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/quantity") ).get

      status(result) shouldBe OK
    }
  }

  "Calling POST /products/other-goods/.../quantity" should {

    "return a 400 with a quantity input view given bad form input" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "NaN") ).get

      status(result) shouldBe BAD_REQUEST
    }

    "return a 303 given valid form input" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "2") ).get

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should fullyMatch regex """^/check-tax-on-goods-you-bring-into-the-uk/products/other-goods/books/country/[a-zA-Z0-9]{6}[?]ir=2$""".r
    }
  }
}
