/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import models.JourneyData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands.UpdateWriteResult
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.language.postfixOps


class TravelDetailsControllerSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[CalculatorService].toInstance(MockitoSugar.mock[CalculatorService]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[AppConfig].toInstance(MockitoSugar.mock[AppConfig]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach: Unit = {
    reset(app.injector.instanceOf[TravelDetailsService], app.injector.instanceOf[Cache])
    when(injected[AppConfig].declareGoodsUrl) thenReturn "https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods"
  }

  trait LocalSetup {

    val controller: TravelDetailsController = app.injector.instanceOf[TravelDetailsController]

    def cachedJourneyData: Future[Option[JourneyData]] = Future.successful(Some(JourneyData()))

    when(injected[Cache].fetch(any())) thenReturn cachedJourneyData
    when(injected[TravelDetailsService].storeAgeOver17(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))
    when(injected[TravelDetailsService].storeBringingDutyFree(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))
    when(injected[TravelDetailsService].storeBringingOverAllowance(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))
    when(injected[TravelDetailsService].storeEuCountryCheck(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))
    when(injected[TravelDetailsService].storeIrishBorder(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))
    when(injected[TravelDetailsService].storePrivateCraft(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))
    when(injected[TravelDetailsService].storeVatResCheck(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))
  }


  "calling GET .../where-goods-bought" should {

    "return the select eu country check page with the previous choice populated if there is one in the keystore" in new LocalSetup {


      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful( Some(JourneyData(euCountryCheck = Some("nonEuOnly"))) )

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Where are you bringing in goods from?"

      doc.getElementsByAttributeValueMatching("name", "euCountryCheck").length shouldBe 3

      doc.getElementById("hint-greatBritain").text() shouldBe "If you are bringing goods from Great Britain or the Isle of Man to Northern Ireland only, select this option."
      doc.select("#euCountryCheck-noneuonly").hasAttr("checked") shouldBe true
      doc.text() should include("If you are bringing in goods that originate in the EU, select EU countries below. If you are bringing in goods that originate outside the EU, select non-EU countries below.")

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the select eu country check page with the previous choice not populated if there is not one in keystore" in new LocalSetup {


      override lazy val cachedJourneyData: Future[Option[JourneyData]] = Future.successful( None )

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get
      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Where are you bringing in goods from?"
      doc.select("#euCountryCheck-euonly").hasAttr("checked") shouldBe false
      doc.select("#euCountryCheck-noneuonly").hasAttr("checked") shouldBe false
      doc.select("#euCountryCheck-greatBritain").hasAttr("checked") shouldBe false


      verify(controller.cache, times(1)).fetch(any())
    }

    "show the back link to previous declaration page when the amendments feature is on" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled) thenReturn true

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)
      doc.getElementById("back").attr("href") shouldBe "/check-tax-on-goods-you-bring-into-the-uk/previous-declaration"

      verify(controller.cache, times(1)).fetch(any())
    }

    "show the back link to gov uk declare goods start page page when the amendments feature is off" in new LocalSetup {


      when(injected[AppConfig].isAmendmentsEnabled) thenReturn false

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)
      doc.getElementById("back").attr("href") shouldBe "https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods"

      verify(controller.cache, times(1)).fetch(any())
    }
  }

  "calling POST .../where-goods-bought" should {

    "redirect to .../goods-bought-into-northern-ireland-inside-eu when user selects country in EU" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "euOnly")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")


      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("euOnly"))(any())
    }

    "redirect to .../goods-bought-outside-eu when user says they have only arrived from countries outside EU" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "nonEuOnly")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("nonEuOnly"))(any())
    }


    "redirect to .../goods-bought-inside-and-outside-eu when user says they have arrived from both EU and ROW countries" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(euCountryCheck = Some("both"))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "both")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("both"))(any())
    }

    "return bad request when given invalid data" in new LocalSetup {
      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeEuCountryCheck(any())(any())(any())

    }

    "return error summary box on the page head when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#euCountryCheck]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#euCountryCheck]").html()).get shouldBe "Select where you are bringing in goods from"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"
    }


    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.select("input[name=euCountryCheck]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=euCountryCheck]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select where you are bringing in goods from"

    }
  }

  "calling GET .../goods-brought-into-northern-ireland" should {
    "return the interrupt page without the added rest of world guidance" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("nonEuOnly"),Some(true), None, None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland")).get
      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.text() should not include "You do not need to tell us about goods bought from countries inside the EU."
    }
  }

  "calling POST .../goods-bought-outside-eu" should {

    "redirect to .../private-travel when bringing in goods over the indicated allowances" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("nonEuOnly"), Some(true), None, None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland").withFormUrlEncodedBody("bringingOverAllowance" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/private-travel")
    }

    "redirect to .../no-need-to-use-service when bringing in goods under the indicated allowances" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("nonEuOnly"), Some(true), None, None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-northern-ireland").withFormUrlEncodedBody("bringingOverAllowance" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/no-need-to-use-service")
    }

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("nonEuOnly"), Some(true), None, None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST
    }
  }


  "calling GET .../goods-brought-into-great-britain-iom" should {

    "return the interrupt page without the added rest of world guidance" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("nonEuOnly"),Some(false), None, None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom")).get
      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.text() should include ("Goods brought into Great Britain")
    }
  }

  "calling POST .../goods-brought-into-great-britain-iom" should {

    "redirect to .../private-travel when bringing in goods over the indicated allowances" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("euOnly"), Some(false), None, None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/goods-brought-into-great-britain-iom").withFormUrlEncodedBody("bringingOverAllowance" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/private-travel")
    }

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("both"), Some(true), None, None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST
    }
  }

  "calling GET .../goods-bought-into-northern-ireland-inside-eu" should {

    "return the goods bought inside the eu interrupt page" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("euOnly"), Some(true), None, None)))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/goods-bought-into-northern-ireland-inside-eu")).get
      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.text() should include ("You do not need to tell us about your goods")
    }
  }

  "calling GET .../no-need-to-use-service" should {

    "return no need to use this service page" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("nonEuOnly"), Some(true),None,None,None,None,None, None, Some(false))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/no-need-to-use-service")).get
      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.text() should include ("You do not need to use this service")
    }
  }

  "Calling GET .../private-travel" should {

    "return the private craft page unpopulated if there is no age answer in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"), Some(true),None,None,None,None, None,None, Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc: Document = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe false
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe false

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the private craft page pre populated no if there is answer false in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"), Some(true), None,None,None,None,None,None, Some(true), privateCraft = Some(false))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc: Document = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe false
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe true

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the private craft page pre populated yes if there is age answer true in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"),Some(true), None,None,None,None,None, None, Some(true), privateCraft = Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe OK
      val doc: Document = Jsoup.parse(contentAsString(response))

      doc.select("#privateCraft-true").hasAttr("checked") shouldBe true
      doc.select("#privateCraft-false").hasAttr("checked") shouldBe false

      verify(controller.cache, times(1)).fetch(any())
    }
  }

  "Calling POST .../private-travel" should {

    "redirect to /check-tax-on-goods-you-bring-into-the-uk/confirm-age" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"), Some(true),None,None,None,None,None, None, Some(true), privateCraft = Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel").withFormUrlEncodedBody("privateCraft" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/confirm-age")

      verify(controller.travelDetailsService, times(1)).storePrivateCraft(any())(meq(true))(any())
    }


    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"), Some(true),None,None,None, None,None, None, Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel").withFormUrlEncodedBody("value" -> "bad_value")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storePrivateCraft(any())(any())(any())

    }

    "return error summary box on the page head when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"),Some(true), None,None,None,None, None,None, Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#privateCraft]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#privateCraft]").html()).get shouldBe "Select yes if you are arriving in the UK by private transport"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"
    }


    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"),Some(true), None,None,None,None,None, None, Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/private-travel")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.select("input[name=privateCraft]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=privateCraft]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select yes if you are arriving in the UK by private transport"

    }

  }

  "calling GET .../confirm-age" should {

    "return the confirm age page unpopulated if there is no age answer in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"),Some(true),None,None,None,None, None,None, Some(true), Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe false
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe false

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the confirm age page pre-populated yes if there is age answer true in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"), Some(true),None,None,None,None,None, None, Some(true), Some(true), ageOver17 = Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe true
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe false

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the confirm age page pre-populated no if there is age answer false in keystore" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"),Some(true),None,None,None,None, None,None, Some(true), Some(true), ageOver17 = Some(false))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.select("#ageOver17-true").hasAttr("checked") shouldBe false
      doc.select("#ageOver17-false").hasAttr("checked") shouldBe true

      verify(controller.cache, times(1)).fetch(any())
    }

  }

  "Calling POST .../confirm-age" should {

    "redirect to /check-tax-on-goods-you-bring-into-the-uk/tell-us when subsequent journey data is present" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"),Some(true), None,None, None, None, None,None, Some(true), Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age").withFormUrlEncodedBody("ageOver17" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/tell-us")

      verify(controller.travelDetailsService, times(1)).storeAgeOver17(any())(meq(true))(any())
    }

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"),Some(true),None, None, None,None, None,None, Some(true), Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeAgeOver17(any())(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"), Some(true),None,None,None, None,None, None, Some(true), Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#ageOver17]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#ageOver17]").html()).get shouldBe "Select yes if you are aged 17 or over"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"

    }

    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"), Some(true),None,None,None,None,None, None, Some(true), Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age")).get

      status(response) shouldBe BAD_REQUEST

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.select("input[name=ageOver17]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=ageOver17]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select yes if you are aged 17 or over"
    }

  }



  "Calling GET .../check-tax-on-goods-you-bring-into-the-uk" should {

    "redirect to where-good-bought, changing session id, keep any session data for bcpaccess when amendments feature is off" in new LocalSetup {


      when(injected[AppConfig].isAmendmentsEnabled) thenReturn false

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = EnhancedFakeRequest("GET","/check-tax-on-goods-you-bring-into-the-uk").withSession("bcpaccess" -> "true")
      val sessionId: Option[String] = fakeRequest.session.get("sessionId")
      val response: Future[Result] = route(app, fakeRequest).get

      status(response) shouldBe  SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
      session(response).data.get("bcpaccess") shouldBe Some("true")
      session(response).data.get("sessionId") should not be sessionId

    }

    "redirect to previous-declaration, changing session id, keep any session data for bcpaccess when amendments feature is on" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled) thenReturn true

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = EnhancedFakeRequest("GET","/check-tax-on-goods-you-bring-into-the-uk").withSession("bcpaccess" -> "true")
      val sessionId: Option[String] = fakeRequest.session.get("sessionId")

      val response: Future[Result] = route(app, fakeRequest).get

      status(response) shouldBe  SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-declaration")
      session(response).data.get("bcpaccess") shouldBe Some("true")
      session(response).data.get("sessionId") should not be sessionId

    }

  }

  "calling GET .../keepAlive" should {
    "return a response OK" in new LocalSetup {
      when(injected[Cache].updateUpdatedAtTimestamp(any())) thenReturn Future
        .successful(UpdateWriteResult(ok = true,1,1,Seq(),Seq(),None,None,None))
      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/keep-alive")).get
      status(response) shouldBe OK

    }

    "return a response INTERNAL_SERVER_ERROR" in new LocalSetup {
      when(injected[Cache].updateUpdatedAtTimestamp(any())) thenReturn Future
        .failed(new Exception("failed updating timestamp"))
      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/keep-alive")).get
      status(response) shouldBe INTERNAL_SERVER_ERROR

    }
  }

  "Invoking GET .../gb-ni-no-need-to-use-service" should {

    "load no need to use service gbni page if they have paid VAT and excise and are a UK resident" in new LocalSetup {

      override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(Some(false), Some("greatBritain"), Some(true), Some(true), Some(true), Some(true))))

      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/gb-ni-no-need-to-use-service")).get

      status(response) shouldBe OK

      val content: String = contentAsString(response)
      val doc: Document = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "You do not need to use this service"

      verify(controller.cache, times(1)).fetch(any())
    }
  }

  "return bad request when given invalid data" in new LocalSetup {

    override lazy val cachedJourneyData: Future[Some[JourneyData]] = Future.successful(Some(JourneyData(None, Some("nonEuOnly"),Some(true), None, None,None, None,None,None, Some(true), Some(true))))

    val response: Future[Result] = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/confirm-age").withFormUrlEncodedBody("value" -> "badValue")).get

    status(response) shouldBe BAD_REQUEST

    verify(controller.travelDetailsService, times(0)).storeAgeOver17(any())(any())(any())

  }
}
