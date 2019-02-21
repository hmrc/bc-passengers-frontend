
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
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.language.postfixOps


class TravelDetailsControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[TravelDetailsService])
  }

  val controller: TravelDetailsController = app.injector.instanceOf[TravelDetailsController]

  "Invoking checkDeclareGoodsStartPage" should {

    "return the check declare goods start page" in {

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/check-declare-goods-start-page")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Check tax on goods you bring into the UK"
    }
  }


  "calling GET .../where-goods-bought" should {

    "return the select eu country check page with the previous choice populated if there is one in the keystore" in {


      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( Some(JourneyData(euCountryCheck = Some("nonEuOnly"))) )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#euCountryCheck-noneuonly").hasAttr("checked") shouldBe true

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }

    "return the select eu country check page with the previous choice not populated if there is not one in keystore" in {


      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get
      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#euCountryCheck-euonly").hasAttr("checked") shouldBe false
      doc.select("#euCountryCheck-noneuonly").hasAttr("checked") shouldBe false
      doc.select("#euCountryCheck-both").hasAttr("checked") shouldBe false


      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }
  }

  "calling POST .../where-goods-bought" should {

    "redirect to .../goods-bought-inside-eu when user selects country in EU" in {

      when(controller.travelDetailsService.storeEuCountryCheck(meq("euOnly"))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"))))


      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "euOnly")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-inside-eu")


      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(meq("euOnly"))(any())
    }

    "redirect to .../arrivals-from-outside-the-eu when user says they have only arrived from countries outside EU" in {

      when(controller.travelDetailsService.storeEuCountryCheck(meq("nonEuOnly"))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"))))


      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "nonEuOnly")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arrivals-from-outside-the-eu")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(meq("nonEuOnly"))(any())
    }


    "redirect to .../arrivals-from-both when user says they have arrived from both EU and ROW countries" in {

      when(controller.travelDetailsService.storeEuCountryCheck(meq("both"))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("both"))))


      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "both")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arrivals-from-both")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(meq("both"))(any())
    }

    "return bad request when given invalid data" in {
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeEuCountryCheck(any())(any())

    }
  }

  "calling GET ../arrivals-from-outside-the-eu" should {
    "return the interrupt page without the added rest of world guidance" in {

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arrivals-from-outside-the-eu")).get
      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      content should not include "You do not need to tell us about goods bought from countries inside the EU."
    }
  }


  "calling GET ../arrivals-from-both" should {
    "return the interrupt page without the added rest of world guidance" in {

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arrivals-from-both")).get
      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      content should include ("You do not need to tell us about goods bought from countries inside the EU.")
    }
  }

  "calling GET ../eu-done" should {
    "return the interrupt page without the added rest of world guidance" in {

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/arrivals-from-both")).get
      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      content should include ("You do not need to tell us about goods bought from countries inside the EU.")
    }
  }

  "Invoking confirmAge" should {

    "return the confirm age page unpopulated if there is no age answer in keystore" in {

      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe false
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe false

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }

    "return the confirm age page pre-populated yes if there is age answer true in keystore" in {

      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( Some(JourneyData(privateCraft = Some(true), ageOver17 = Some(true))) )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe true
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe false

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }

    "return the confirm age page pre-populated no if there is age answer false in keystore" in {

      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( Some(JourneyData(privateCraft = Some(false), ageOver17 = Some(false))) )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe false
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe true

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }

  }

  "Invoking confirmAgePost" should {

    "redirect to /check-tax-on-goods-you-bring-into-the-uk/dashboard when subsequent journey data is present" in {

      when(controller.travelDetailsService.storeAgeOver17(meq(true))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( Some(JourneyData(ageOver17 = Some(false), privateCraft = Some(false))) )

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age").withFormUrlEncodedBody("ageOver17" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(controller.travelDetailsService, times(1)).storeAgeOver17(meq(true))(any())
    }

    "return bad request when given invalid data" in {
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeAgeOver17(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#ageOver17]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#ageOver17]").html()).get shouldBe "Select yes if you are 17 or over"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"

    }

    "return error notification on the control when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=ageOver17]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=ageOver17]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select yes if you are 17 or over"
    }

  }

  "Invoking privateCraft" should {

    "return the private craft page unpopulated if there is no age answer in keystore" in {

      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe false
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe false

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }

    "return the private craft page pre populated no if there is age answer false in keystore" in {

      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( Some(JourneyData(privateCraft = Some(false))) )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe false
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe true

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }

    "return the private craft page pre populated yes if there is age answer true in keystore" in {

      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful( Some(JourneyData(privateCraft = Some(true))) )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe true
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe false

      verify(controller.travelDetailsService, times(1)).getJourneyData(any())
    }
  }

  "Invoking privateCraftPost" should {

    "redirect to /passengers/dashboard" in {

      when(controller.travelDetailsService.storePrivateCraft(meq(true))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"), privateCraft = Some(true), ageOver17 = Some(true))))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel").withFormUrlEncodedBody("privateCraft" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/dashboard")

      verify(controller.travelDetailsService, times(1)).storePrivateCraft(meq(true))(any())
    }

    "redirect to /check-tax-on-goods-you-bring-into-the-uk/confirm-age when only private craft journey data is present" in {

      when(controller.travelDetailsService.storePrivateCraft(meq(true))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.travelDetailsService.getJourneyData(any())) thenReturn Future.successful(Some(JourneyData(privateCraft = Some(true))))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel").withFormUrlEncodedBody("privateCraft" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/confirm-age")

      verify(controller.travelDetailsService, times(1)).storePrivateCraft(meq(true))(any())
    }


    "return bad request when given invalid data" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel").withFormUrlEncodedBody("value" -> "bad_value")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storePrivateCraft(any())(any())

    }

    "return error summary box on the page head when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#privateCraft]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#privateCraft]").html()).get shouldBe "Select yes if you arrived in the UK by private aircraft or private boat"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"
    }


    "return error notification on the control when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=privateCraft]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=privateCraft]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select yes if you arrived in the UK by private aircraft or private boat"

    }

  }

  "Invoking newSession" should {

    "redirect to select country, changing session id, keep any session data for bcpaccess when redirecting" in {

      val fakeRequest = EnhancedFakeRequest("GET","/check-tax-on-goods-you-bring-into-the-uk/new-session").withSession("bcpaccess" -> "true")
      val sessionId = fakeRequest.session.get("sessionId")
      val response = route(app, fakeRequest).get

      status(response) shouldBe  SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
      session(response).data.get("bcpaccess") shouldBe Some("true")
      session(response).data.get("sessionId") should not be sessionId

    }

  }

}
