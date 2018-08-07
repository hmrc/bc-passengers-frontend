
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
import services.TravelDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CookieCryptoFilter
import util.{BaseSpec, FakeCookieCryptoFilter}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.collection.JavaConversions._


class TravelDetailsControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[CookieCryptoFilter].to[FakeCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[TravelDetailsService])
  }

  val controller = app.injector.instanceOf[TravelDetailsController]

  "Invoking checkDeclareGoodsStartPage" should {

    "return the check declare goods start page" in {

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/check-declare-goods-start-page")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe("Check allowances and estimate duties to bring goods into the UK")
    }
  }


  "Invoking selectCountry" should {

    "return the select country page with country populated if there is one in the keystore" in {


      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( Some(JourneyData(country = Some("Egypt"))) )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/country-of-purchase")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      content should include ("""<option value="Egypt" selected="selected">Egypt</option>""")

      verify(controller.travelDetailsService, times(1)).getUserInputData(any())
    }

    "return the select country page without country populated if there is not one in the keystore" in {


      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/country-of-purchase")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      content should include ("""<option value="Egypt">Egypt</option>""")

      verify(controller.travelDetailsService, times(1)).getUserInputData(any())
    }
  }

  "Invoking selectCountryPost" should {

    "redirect to /passengers/eu_done when user selects country in EU" in {

      when(controller.travelDetailsService.storeCountry(meq("Spain"))(any())) thenReturn Future.successful(CacheMap("", Map.empty))

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/country-of-purchase").withFormUrlEncodedBody("country" -> "Spain")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/eu-done")


      verify(controller.travelDetailsService, times(1)).storeCountry(meq("Spain"))(any())
    }

    "redirect to /passengers/confirm_age when user selects country outside EU" in {

      when(controller.travelDetailsService.storeCountry(meq("Afghanistan"))(any())) thenReturn Future.successful(CacheMap("", Map.empty))

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/country-of-purchase").withFormUrlEncodedBody("country" -> "Afghanistan")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/confirm-age")

      verify(controller.travelDetailsService, times(1)).storeCountry(meq("Afghanistan"))(any())
    }

    "return bad request when given invalid data" in {
      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/country-of-purchase").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeCountry(any())(any())

    }
  }

  "Invoking confirmAge" should {

    "return the confirm age page unpopulated if there is no age answer in keystore" in {

      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/confirm-age")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe false
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe false

      verify(controller.travelDetailsService, times(1)).getUserInputData(any())
    }

    "return the confirm age page pre-populated yes if there is age answer true in keystore" in {

      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( Some(JourneyData(ageOver17 = Some(true))) )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/confirm-age")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe true
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe false

      verify(controller.travelDetailsService, times(1)).getUserInputData(any())
    }

    "return the confirm age page pre-populated no if there is age answer false in keystore" in {

      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( Some(JourneyData(ageOver17 = Some(false))) )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/confirm-age")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe false
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe true

      verify(controller.travelDetailsService, times(1)).getUserInputData(any())
    }

  }

  "Invoking confirmAgePost" should {

    "redirect to /passengers/private_travel" in {

      when(controller.travelDetailsService.storeAgeOver17(meq(true))(any())) thenReturn Future.successful(CacheMap("", Map.empty))

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/confirm-age").withFormUrlEncodedBody("ageOver17" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/private-travel")

      verify(controller.travelDetailsService, times(1)).storeAgeOver17(meq(true))(any())
    }

    "return bad request when given invalid data" in {
      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/confirm-age").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeAgeOver17(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/confirm-age")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#ageOver17]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#ageOver17]").html()).get shouldBe "Select yes if you are 17 or over"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"

    }

    "return error notification on the control when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/confirm-age")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=ageOver17]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=ageOver17]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select yes if you are 17 or over"
    }

  }

  "Invoking privateCraft" should {

    "return the private craft page unpopulated if there is no age answer in keystore" in {

      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/private-travel")).get

      status(response) shouldBe OK
      val doc = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe false
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe false

      verify(controller.travelDetailsService, times(1)).getUserInputData(any())
    }

    "return the private craft page pre populated no if there is age answer false in keystore" in {

      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( Some(JourneyData(privateCraft = Some(false))) )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/private-travel")).get

      status(response) shouldBe OK
      val doc = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe false
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe true

      verify(controller.travelDetailsService, times(1)).getUserInputData(any())
    }

    "return the private craft page pre populated yes if there is age answer true in keystore" in {

      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( Some(JourneyData(privateCraft = Some(true))) )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/private-travel")).get

      status(response) shouldBe OK
      val doc = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe true
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe false

      verify(controller.travelDetailsService, times(1)).getUserInputData(any())
    }
  }

  "Invoking privateCraftPost" should {

    "redirect to /passengers/dashboard" in {

      when(controller.travelDetailsService.storePrivateCraft(meq(true))(any())) thenReturn Future.successful(CacheMap("", Map.empty))


      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/private-travel").withFormUrlEncodedBody("privateCraft" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/bc-passengers-frontend/dashboard")

      verify(controller.travelDetailsService, times(1)).storePrivateCraft(meq(true))(any())
    }

    "return bad request when given invalid data" in {

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/private-travel").withFormUrlEncodedBody("value" -> "bad_value")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storePrivateCraft(any())(any())

    }

    "return error summary box on the page head when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/private-travel")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#privateCraft]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#privateCraft]").html()).get shouldBe "Select yes if you arrived in the UK by private aircraft or private boat"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"
    }


    "return error notification on the control when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/bc-passengers-frontend/private-travel")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=privateCraft]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=privateCraft]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select yes if you arrived in the UK by private aircraft or private boat"

    }

  }

  "Invoking productDashboard" should {

    "display dashboard page" in {

      when(controller.travelDetailsService.getUserInputData(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/bc-passengers-frontend/dashboard")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe ("Tell us about your purchases")


    }
  }


}
