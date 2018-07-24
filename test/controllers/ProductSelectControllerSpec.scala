
package controllers

import models.JourneyData
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.libs.Json
import services.TravelDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future
import scala.language.postfixOps


class ProductSelectControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[TravelDetailsService])
  }

  val controller = app.injector.instanceOf[ProductSelectController]

  def checkForCorrectResponse(url: String, h1: String, checkbox: Option[String] = None){
    val response = route(app, EnhancedFakeRequest("GET", s"/bc-passengers-frontend/$url")).get
    status(response) shouldBe OK
    val content = contentAsString(response)
    val doc = Jsoup.parse(content)
    doc.getElementsByTag("h1").text() shouldBe (h1)

    checkbox match {
      case Some(checkbox) =>
        Option(doc.getElementById(checkbox)) should not be None
      case None => None;
    }
  }

  "Invoking selectProducts" should {

    "return the select products alcohol page given path /alcohol" in {
      checkForCorrectResponse("products/alcohol", "What type of alcohol are you bringing into the UK?", Some("beer"))
    }

    "return the correct product details page given path /alcohol/beer" in {
      checkForCorrectResponse("products/alcohol/beer", "Beer")
    }

    "return the select products tobacco page given path /tobacco" in {
      checkForCorrectResponse("products/tobacco", "What type of tobacco are you bringing into the UK?", Some("cigarettes"))
    }

    "return the correct product details page given path /tobacco/cigars" in {
      checkForCorrectResponse("products/tobacco/cigars", "Cigars")
    }

    "return the select products other goods page given path /other-goods" in {
      checkForCorrectResponse("products/other-goods", "What items are you bringing into the UK?",
        Some("antiques"))
    }

    "return the correct product details page given path /other-goods/books" in {
      checkForCorrectResponse("products/other-goods/books", "Books and publications")
    }

    "return the select products page given path /other-goods/carpets-cotton-fabric" in {
      checkForCorrectResponse("products/other-goods/carpets-cotton-fabric", "What carpets, cotton and fabrics are you bringing into the UK?", Some("carpets"))
      }

    "return the technical error page given an incorrect path" in {
      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/products/dfhshdf")).get
      status(response) shouldBe NOT_FOUND
      val content = contentAsString(response)
      val doc = Jsoup.parse(content)
      doc.getElementsByTag("h1").text() shouldBe ("Technical problem")
    }

  }

  "Invoking processSelectProducts" should {

    "return bad request when given invalid data" in {
      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol").withFormUrlEncodedBody("value" -> "bad_value")).get
      status(response) shouldBe BAD_REQUEST
    }

    "addSelectedProducts to keystore and return redirect to nextStep given valid checkbox values" in {

      when(app.injector.instanceOf[TravelDetailsService].addSelectedProducts(any())(any())) thenReturn{
        Future.successful(CacheMap("dummy", Map.empty))
      }

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/products/alcohol").withFormUrlEncodedBody("tokens[0]" -> "beer")).get

      status(response) shouldBe SEE_OTHER

      verify(app.injector.instanceOf[TravelDetailsService], times(1)).addSelectedProducts(meq(List(List("alcohol", "beer"))))(any())

    }
  }

  "Invoking nextStep" should {

    "redirect to dashboard page when journeyData.selectedProducts returns an empty list" in {

      val journeyData = Some(JourneyData(selectedProducts = Some(List.empty)))

      when(app.injector.instanceOf[TravelDetailsService].getUserInputData(any())) thenReturn{
        Future.successful(journeyData)
      }

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/next-step")).get

      status(response) shouldBe SEE_OTHER

      verify(app.injector.instanceOf[TravelDetailsService], times(1)).getUserInputData(any())

    }

    "redirect to selectProducts when journeyData.selectedProducts returns a list of lists" in {

      val journeyData = Some(JourneyData(selectedProducts = Some(List(List("alcohol", "beer")))))

      when(app.injector.instanceOf[TravelDetailsService].getUserInputData(any())) thenReturn{
        Future.successful(journeyData)
      }

      when(app.injector.instanceOf[TravelDetailsService].removeSelectedProduct()(any())) thenReturn{
        Future.successful(CacheMap("dummy", Map.empty))
      }

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/next-step")).get

      status(response) shouldBe SEE_OTHER

      verify(app.injector.instanceOf[TravelDetailsService], times(1)).getUserInputData(any())

      verify(app.injector.instanceOf[TravelDetailsService], times(1)).removeSelectedProduct()(any())
    }

    "return a 404 when journeyData.selectedProducts returns None" in {

      val journeyData = Some(JourneyData(selectedProducts = None))

      when(app.injector.instanceOf[TravelDetailsService].getUserInputData(any())) thenReturn{
        Future.successful(journeyData)
      }

      when(app.injector.instanceOf[TravelDetailsService].removeSelectedProduct()(any())) thenReturn{
        Future.successful(CacheMap("dummy", Map.empty))
      }

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/next-step")).get

      status(response) shouldBe NOT_FOUND

      verify(app.injector.instanceOf[TravelDetailsService], times(1)).getUserInputData(any())

      verify(app.injector.instanceOf[TravelDetailsService], times(0)).removeSelectedProduct()(any())
    }


    "return a 404 when getUserInputData returns None" in {

      when(app.injector.instanceOf[TravelDetailsService].getUserInputData(any())) thenReturn{
        Future.successful(None)
      }

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/next-step")).get

      status(response) shouldBe NOT_FOUND

    }

  }




}
