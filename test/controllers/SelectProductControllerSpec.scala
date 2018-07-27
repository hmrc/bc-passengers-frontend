
package controllers

import models.{JourneyData, ProductPath}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.Inspectors._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{SelectProductService, TravelDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future
import scala.language.postfixOps


class SelectProductControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[SelectProductService].toInstance(MockitoSugar.mock[SelectProductService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(injected[TravelDetailsService], injected[SelectProductService])
  }

  val controller: SelectProductController = injected[SelectProductController]

  trait LocalSetup {
    def result: Future[Result]
    def content: String = contentAsString(result)
    def doc: Document = Jsoup.parse(content)
    def h1: String = doc.getElementsByTag("h1").text
    def title: String = doc.title
  }

  "Invoking askProductSelection for branch items" should {

    "return the select products alcohol page given path /alcohol (branch)" in new LocalSetup {
      override val result: Future[Result] = controller.askProductSelection(ProductPath("alcohol"))(EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol"))

      status(result) shouldBe OK
      h1 shouldBe "What type of alcohol are you bringing into the UK?"
      title shouldBe "What type of alcohol are you bringing into the UK? - GOV.UK"
      forAll(List("beer", "wine", "cider", "spirits", "wine", "sparkling")) { cb =>
        Option(doc.getElementById(cb)) should not be None
      }
    }


    "return the select products tobacco page given path /tobacco (branch)" in new LocalSetup {
      override val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/tobacco")).get

      status(result) shouldBe OK
      h1 shouldBe "What type of tobacco are you bringing into the UK?"

      forAll(List("cigars", "cigarettes", "cigarillos", "rolling", "chewing")) { cb =>
        Option(doc.getElementById(cb)) should not be None
      }
    }


    "return the select products other goods page given path /other-goods (branch" in new LocalSetup {
      override val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods")).get

      status(result) shouldBe OK
      h1 shouldBe "What items are you bringing into the UK?"
      Option(doc.getElementById("antiques")) should not be None
    }

    "return the select products page given path /other-goods/carpets-cotton-fabric" in new LocalSetup {
      override val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/other-goods/carpets-cotton-fabric")).get

      status(result) shouldBe OK
      h1 shouldBe "What carpets, cotton and fabrics are you bringing into the UK?"
      forAll(List("carpets", "cotton", "fabrics")) { cb =>
        Option(doc.getElementById(cb)) should not be None
      }
    }

    "return the technical error page given an incorrect path" in new LocalSetup {
      override val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/invalid/path")).get

      status(result) shouldBe NOT_FOUND
      h1 shouldBe "Technical problem"
    }

    "return the technical error page given a leaf path" in new LocalSetup {
      override val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/alcohol/beer")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
      h1 shouldBe "Technical problem"
    }
  }

  "Invoking processSelectedProducts" should {

    "return bad request when given invalid data" in {
      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol").withFormUrlEncodedBody("value" -> "bad_value")).get
      status(response) shouldBe BAD_REQUEST
    }

    "addSelectedProducts to keystore and return redirect to nextStep given valid checkbox values" in {

      when(injected[SelectProductService].addSelectedProducts(any())(any(),any())) thenReturn {
        Future.successful(CacheMap("fakeid", Map.empty))
      }

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol").withFormUrlEncodedBody("tokens[0]" -> "beer")).get

      status(response) shouldBe SEE_OTHER

      verify(injected[SelectProductService], times(1)).addSelectedProducts(meq(List(ProductPath("alcohol/beer"))))(any(),any())

    }
  }

  "Invoking nextStep" should {


    trait NextStepSetup {

      def journeyData: Option[JourneyData]

      lazy val response = {

        when(injected[TravelDetailsService].getJourneyData(any())) thenReturn{
          Future.successful(journeyData)
        }
        when(injected[SelectProductService].removeSelectedProduct()(any(),any())) thenReturn{
          Future.successful(CacheMap("dummy", Map.empty))
        }

        route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/next-step")).get
      }


    }

    "redirect to dashboard page when journeyData.selectedProducts returns an empty list" in new NextStepSetup {

      override val journeyData = Some(JourneyData(selectedProducts = Some(List.empty)))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/dashboard")
      verify(injected[TravelDetailsService], times(1)).getJourneyData(any())
      verify(injected[SelectProductService], times(0)).removeSelectedProduct()(any(),any())
    }

    "inform the user the item is not found when journeyData.selectedProducts contains an invalid path" in new NextStepSetup {

      override val journeyData = Some(JourneyData(selectedProducts = Some(List(List("other-goods", "beer")))))

      status(response) shouldBe NOT_FOUND
      verify(injected[TravelDetailsService], times(1)).getJourneyData(any())
      verify(injected[SelectProductService], times(1)).removeSelectedProduct()(any(),any())
    }

    "go to purchase input form when journeyData.selectedProducts contains a leaf" in new NextStepSetup {

      override val journeyData = Some(JourneyData(selectedProducts = Some(List(List("other-goods", "books")))))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/products/other-goods/books/quantity")
      verify(injected[TravelDetailsService], times(1)).getJourneyData(any())
      verify(injected[SelectProductService], times(1)).removeSelectedProduct()(any(),any())
    }

    "redirect to selectProducts when journeyData.selectedProducts contains a branch" in new NextStepSetup {

      override val journeyData = Some(JourneyData(selectedProducts = Some(List(List("alcohol")))))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/products/alcohol")
      verify(injected[TravelDetailsService], times(1)).getJourneyData(any())
      verify(injected[SelectProductService], times(1)).removeSelectedProduct()(any(),any())
    }

    "return a INTERNAL_SERVER_ERROR when journeyData.selectedProducts returns None" in new NextStepSetup {

      override val journeyData = Some(JourneyData(selectedProducts = None))

      status(response) shouldBe INTERNAL_SERVER_ERROR
      verify(injected[TravelDetailsService], times(1)).getJourneyData(any())
      verify(injected[SelectProductService], times(0)).removeSelectedProduct()(any(),any())
    }


    "return a INTERNAL_SERVER_ERROR when getUserInputData returns None" in new NextStepSetup {

      override val journeyData = None


      status(response) shouldBe INTERNAL_SERVER_ERROR
      verify(injected[TravelDetailsService], times(1)).getJourneyData(any())
      verify(injected[SelectProductService], times(0)).removeSelectedProduct()(any(),any())
    }

  }
}
