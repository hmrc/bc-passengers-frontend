package controllers

import models.{JourneyData, ProductPath, PurchasedProductInstance}
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
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future


class OtherGoodsInputControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[PurchasedProductService].toInstance(MockitoSugar.mock[PurchasedProductService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  trait LocalSetup {

    def requiredJourneyData: JourneyData = JourneyData(ageOver17 = Some(true), privateCraft = Some(false))
    def cachedJourneyData: Option[JourneyData]

    def route[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Option[Future[Result]] = {

      when(injected[TravelDetailsService].getJourneyData(any())) thenReturn {
        Future.successful(cachedJourneyData)
      }

      rt(app, req)
    }
  }

  "Calling GET /products/other-goods/.../currency" should {

    "return a 200 and not prepopulate the currency value if there is no working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/jewellery/currency/iid0?ir=0")).get
      val content: String = contentAsString(result)

      status(result) shouldBe OK

      content should not include """selected="selected""""
    }

  }

  "Calling GET /products/other-goods/.../currency/<iid>/update" should {

    "add a product to the working instance, return a 200 and prepopulate the currency value" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData)

      when(injected[PurchasedProductService].makeWorkingInstance(any(), any())(any(), any())) thenReturn Future.successful(JourneyData(workingInstance =
        Some(PurchasedProductInstance(ProductPath("tobacco/cigarettes"), iid = "iid0", currency = Some("JMD"))))
      )

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/jewellery/currency/iid0/update")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      content should include ("""<option value="JMD" selected="selected">Jamaica Dollar (JMD)</option>""")

      verify(injected[PurchasedProductService], times(1)).makeWorkingInstance(any(), any())(any(), any())
    }

    "return a 200 with the currency value populated if there is a working instance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(requiredJourneyData.copy(purchasedProductInstances =
        List(PurchasedProductInstance(ProductPath("other-goods/jewellery"), iid = "iid0", currency = Some("JMD")))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/jewellery/currency/iid0/update")).get
      val content: String = contentAsString(result)
      val doc: Document = Jsoup.parse(content)

      status(result) shouldBe OK

      content should include ("""<option value="JMD" selected="selected">Jamaica Dollar (JMD)</option>""")
    }
  }

  "Calling displayQuantityInput" should {

    "return a 200 with a quantity input view given a correct product path" in new LocalSetup {

      override lazy val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/books/quantity") ).get

      status(result) shouldBe OK
    }
  }

  "Calling processQuantityInput" should {

    "return a 400 with a quantity input view given bad form input" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData] = Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "NaN") ).get

      status(result) shouldBe BAD_REQUEST
    }

    "return a 303 given valid form input" in new LocalSetup {

      override val cachedJourneyData: Option[JourneyData]= Some(requiredJourneyData)

      val result: Future[Result]= route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/other-goods/books/quantity").withFormUrlEncodedBody("quantity" -> "2") ).get

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should fullyMatch regex """^/bc-passengers-frontend/products/other-goods/books/currency/[a-zA-Z0-9]{6}[?]ir=2$""".r
    }
  }
}
