package controllers


import connectors.Cache
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

class VatResTravelDetailsControllerSpec extends BaseSpec {


  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .configure("features.vat-res" -> true)
    .build()


  override def beforeEach: Unit = {
    reset(injected[Cache], injected[TravelDetailsService])
  }

  val controller: TravelDetailsController = app.injector.instanceOf[TravelDetailsController]

  "Invoking POST .../eu-country-check" should {

    "redirect to .../did-you-claim-tax-back when user selects country in EU" in {

      when(controller.travelDetailsService.storeEuCountryCheck(meq("euOnly"))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.cache.fetch(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"))))


      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "euOnly")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")


      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(meq("euOnly"))(any())
    }

    "redirect to .../arrivals-from-outside-the-eu when user says they have only arrived from countries outside EU" in {

      when(controller.travelDetailsService.storeEuCountryCheck(meq("nonEuOnly"))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.cache.fetch(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"))))


      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "nonEuOnly")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-outside-eu")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(meq("nonEuOnly"))(any())
    }


    "redirect to .../did-you-claim-tax-back when user says they have arrived from both EU and ROW countries" in {

      when(controller.travelDetailsService.storeEuCountryCheck(meq("both"))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.cache.fetch(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("both"))))


      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "both")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(meq("both"))(any())
    }
  }

  "Invoking GET .../did-you-claim-tax-back" should {
    "return the did you claim tax back view unpopulated if there is no vat res answer in keystore?" in {

      when(controller.cache.fetch(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#claimedVatRes-true").hasAttr("checked") shouldBe false
      doc.select("#claimedVatRes-false").hasAttr("checked") shouldBe false

      content should include ("Did you claim tax back on any goods you bought in the EU?")

      verify(controller.cache, times(1)).fetch(any())

    }

    "return the did you claim tax back view populated if there is a vat res answer already in keystore?" in {

      when(controller.cache.fetch(any())) thenReturn Future.successful( Some(JourneyData(isVatResClaimed = Some(true))) )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#claimedVatRes-true").hasAttr("checked") shouldBe true
      doc.select("#claimedVatRes-false").hasAttr("checked") shouldBe false

      content should include ("Did you claim tax back on any goods you bought in the EU?")

      verify(controller.cache, times(1)).fetch(any())

    }
  }

  "Invoking POST .../did-you-claim-tax-back" should {

    "return bad request when given invalid data" in {
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeVatResCheck(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#claimedVatRes]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#claimedVatRes]").html()).get shouldBe "Select if you claimed tax back on any goods you bought in the EU"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"

    }

    "return error notification on the control when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=claimedVatRes]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=claimedVatRes]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select if you claimed tax back on any goods you bought in the EU"
    }
  }

  "Invoking GET .../duty-free" should {
    "return the duty free view unpopulated if there is no duty free answer in keystore?" in {

      when(controller.cache.fetch(any())) thenReturn Future.successful( None )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#bringingDutyFree-true").hasAttr("checked") shouldBe false
      doc.select("#bringingDutyFree-false").hasAttr("checked") shouldBe false

      content should include ("Are you bringing in duty-free alcohol or tobacco bought in UK or EU shops?")

      verify(controller.cache, times(1)).fetch(any())

    }

    "return the duty free view populated if there is a duty free answer already in keystore?" in {

      when(controller.cache.fetch(any())) thenReturn Future.successful( Some(JourneyData(bringingDutyFree = Some(true))) )

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#bringingDutyFree-true").hasAttr("checked") shouldBe true
      doc.select("#bringingDutyFree-false").hasAttr("checked") shouldBe false

      content should include ("Are you bringing in duty-free alcohol or tobacco bought in UK or EU shops?")

      verify(controller.cache, times(1)).fetch(any())

    }
  }

  "Invoking POST .../duty-free" should {

    "redirect to the goods-bought-inside-eu if not bringing duty free and had previously selected eu only countries" in {
      when(controller.travelDetailsService.storeDutyFreeCheck(meq(false))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.cache.fetch(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"), isVatResClaimed = Some(false))))


      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free").withFormUrlEncodedBody("bringingDutyFree" -> "false")).get

      status(response) shouldBe SEE_OTHER

      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-inside-eu")

      verify(controller.travelDetailsService, times(0)).storeVatResCheck(any())(any())

    }

    "redirect to the goods-bought-inside-and-outside-eu if not bringing duty free and had previously selected both eu and non eu countries" in {
      when(controller.travelDetailsService.storeDutyFreeCheck(meq(false))(any())) thenReturn Future.successful(CacheMap("", Map.empty))
      when(controller.cache.fetch(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("both"), isVatResClaimed = Some(false))))


      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free").withFormUrlEncodedBody("bringingDutyFree" -> "false")).get

      status(response) shouldBe SEE_OTHER

      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-inside-and-outside-eu")

      verify(controller.travelDetailsService, times(0)).storeVatResCheck(any())(any())

    }


    "return bad request when given invalid data" in {
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeVatResCheck(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#bringingDutyFree]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#bringingDutyFree]").html()).get shouldBe "Select if you are bringing in alcohol or tobacco bought in duty-free shops in the UK or EU"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"

    }

    "return error notification on the control when trying to submit a blank form" in {

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=bringingDutyFree]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=bringingDutyFree]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select if you are bringing in alcohol or tobacco bought in duty-free shops in the UK or EU"
    }
  }
}
