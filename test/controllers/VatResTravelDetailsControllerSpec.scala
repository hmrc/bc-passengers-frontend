/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers


import connectors.Cache
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.JourneyData
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.TravelDetailsService
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.language.postfixOps

class VatResTravelDetailsControllerSpec extends BaseSpec {


  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[Cache].toInstance(MockitoSugar.mock[Cache]))
    .overrides(bind[TravelDetailsService].toInstance(MockitoSugar.mock[TravelDetailsService]))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[IdentifierAction].to[FakeIdentifierAction])
    .configure("features.vat-res" -> true)
    .build()


  override def beforeEach: Unit = {
    reset(injected[Cache], injected[TravelDetailsService])
  }

  trait LocalSetup {

    val controller: TravelDetailsController = app.injector.instanceOf[TravelDetailsController]
    def cachedJourneyData: Option[JourneyData] = Some(JourneyData())
    when(injected[Cache].fetch(any())) thenReturn Future.successful(cachedJourneyData)
    when(injected[TravelDetailsService].storeBringingOverAllowance(any())(any())(any())) thenReturn Future.successful(Some(JourneyData()))

  }

  "Invoking POST .../eu-country-check" should {

    "redirect to .../did-you-claim-tax-back when user selects country in EU" in new LocalSetup {

      when(controller.travelDetailsService.storeEuCountryCheck(any())(meq("euOnly"))(any())) thenReturn Future.successful( Some( JourneyData() ) )
      when(controller.cache.fetch(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("euOnly"))))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "euOnly")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("euOnly"))(any())
    }

    "redirect to .../arrivals-from-outside-the-eu when user says they have only arrived from countries outside EU" in new LocalSetup {

      when(controller.travelDetailsService.storeEuCountryCheck(any())(meq("nonEuOnly"))(any())) thenReturn Future.successful( Some( JourneyData() ) )
      when(controller.cache.fetch(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("nonEuOnly"))))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "nonEuOnly")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("nonEuOnly"))(any())
    }


    "redirect to .../did-you-claim-tax-back when user says they have arrived from both EU and ROW countries" in new LocalSetup {

      when(controller.travelDetailsService.storeEuCountryCheck(any())(meq("both"))(any())) thenReturn Future.successful( Some( JourneyData() ) )
      when(controller.cache.fetch(any())) thenReturn Future.successful(Some(JourneyData(euCountryCheck = Some("both"))))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought").withFormUrlEncodedBody("euCountryCheck" -> "both")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/arriving-ni")

      verify(controller.travelDetailsService, times(1)).storeEuCountryCheck(any())(meq("both"))(any())
    }
  }

  "Invoking GET .../did-you-claim-tax-back" should {

    "return the did you claim tax back view unpopulated if there is no vat res answer in keystore" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), arrivingNICheck = Some(true), isVatResClaimed = None))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#claimedVatRes-true").hasAttr("checked") shouldBe false
      doc.select("#claimedVatRes-false").hasAttr("checked") shouldBe false

      content should include ("Did you claim tax back on any goods you bought in the EU?")

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the did you claim tax back view populated if there is a vat res answer already in keystore?" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"),arrivingNICheck = Some(true),  isVatResClaimed = Some(true)))

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

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), arrivingNICheck = Some(true), isVatResClaimed = None))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeVatResCheck(any())(any())(any())

    }

    "return top error summary box when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), arrivingNICheck = Some(true), isVatResClaimed = None))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#claimedVatRes]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#claimedVatRes]").html()).get shouldBe "Select if you claimed tax back on any goods you bought in the EU"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"
    }

    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"),arrivingNICheck = Some(true), Some(false), Some(false),isVatResClaimed = None))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/did-you-claim-tax-back")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=claimedVatRes]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=claimedVatRes]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select if you claimed tax back on any goods you bought in the EU"
    }
  }

  "Invoking GET .../duty-free" should {

    "return the duty free view unpopulated if there is no duty free answer in keystore" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), Some(false), Some(false),Some(false), Some(false), Some(false),Some(false),isBringingDutyFree = None))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#isBringingDutyFree-true").hasAttr("checked") shouldBe false
      doc.select("#isBringingDutyFree-false").hasAttr("checked") shouldBe false

      content should include ("Are you bringing in duty-free alcohol or tobacco bought in UK or EU shops?")

      verify(controller.cache, times(1)).fetch(any())
    }

    "return the duty free view populated if there is a duty free answer already in keystore" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), Some(false),Some(false),Some(false),Some(false),Some(false), Some(false), isBringingDutyFree = Some(true)))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")).get

      status(response) shouldBe OK

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("#isBringingDutyFree-true").hasAttr("checked") shouldBe true
      doc.select("#isBringingDutyFree-false").hasAttr("checked") shouldBe false

      content should include ("Are you bringing in duty-free alcohol or tobacco bought in UK or EU shops?")

      verify(controller.cache, times(1)).fetch(any())
    }
  }

  "Invoking POST .../duty-free" should {

    "redirect to the goods-bought-into-northern-ireland-inside-EU if not bringing duty free and had previously selected eu only countries" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), Some(false),Some(false),Some(false),Some(false),Some(false),Some(false),  isBringingDutyFree = None))

      when(controller.travelDetailsService.storeBringingDutyFree(any())(meq(false))(any())) thenReturn Future.successful( Some( JourneyData() ) )

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free").withFormUrlEncodedBody("isBringingDutyFree" -> "false")).get

      status(response) shouldBe SEE_OTHER

      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/goods-bought-into-northern-ireland-inside-eu")

      verify(controller.travelDetailsService, times(0)).storeVatResCheck(any())(any())(any())
    }

    "return bad request when given invalid data" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), Some(false),Some(false),Some(false),Some(false),Some(false),  Some(false), isBringingDutyFree = None))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free").withFormUrlEncodedBody("value" -> "badValue")).get

      status(response) shouldBe BAD_REQUEST

      verify(controller.travelDetailsService, times(0)).storeVatResCheck(any())(any())(any())
    }

    "return top error summary box when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"),Some(false),Some(false),Some(false),Some(false),Some(false), Some(false), isBringingDutyFree = None))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      Option(doc.getElementById("errors").select("a[href=#isBringingDutyFree]")).isEmpty shouldBe false
      Option(doc.getElementById("errors").select("a[href=#isBringingDutyFree]").html()).get shouldBe "Select if you are bringing in alcohol or tobacco bought in duty-free shops in the UK or EU"
      Option(doc.getElementById("errors").select("h2").hasClass("error-summary-heading")).get shouldBe true
      Option(doc.getElementById("errors").select("h2").html()).get shouldBe "There is a problem"
    }

    "return error notification on the control when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), Some(false),Some(false),Some(false),Some(false),Some(false), Some(false),isBringingDutyFree = None))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=isBringingDutyFree]").parents.find(_.tagName=="fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=isBringingDutyFree]").parents.find(_.tagName=="fieldset").get.select(".error-message").html() shouldBe "Select if you are bringing in alcohol or tobacco bought in duty-free shops in the UK or EU"
    }
  }

  "Invoking GET .../duty-free-eu" should {

    "return the interrupt page for passengers coming from EU that have bought EU" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"),Some(false),Some(false),Some(false),Some(false), Some(false), Some(false), Some(true)))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-eu")).get

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.title() shouldBe "You may need to declare goods brought in from EU countries - Check tax on goods you bring into the UK - GOV.UK"
      status(response) shouldBe OK
    }
  }

  "Invoking POST .../duty-free-eu" should {

    "return error notification on the form when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), Some(false),Some(false),Some(false),Some(false),Some(false), Some(false), Some(true)))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-eu")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=bringingOverAllowance]").parents.find(_.tagName == "fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=bringingOverAllowance]").parents.find(_.tagName == "fieldset").get.select(".error-message").html() shouldBe "Select yes if you are bringing in goods over your allowances, or you are unsure or undecided"
    }

    "redirect to the no need to use service page if they are not bringing in goods over their allowance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"),Some(false), Some(false),Some(false),Some(false),Some(false),Some(false), Some(true)))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-eu").withFormUrlEncodedBody("bringingOverAllowance" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/no-need-to-use-service")
    }

    "redirect to the private travel page if they are bringing in goods over their allowance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("euOnly"), Some(false),Some(false),Some(false),Some(false),Some(false), Some(false), Some(true)))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-eu").withFormUrlEncodedBody("bringingOverAllowance" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/private-travel")
    }
  }

  "Invoking GET .../duty-free-mix" should {

    "return the interrupt page for passengers coming from EU that have bought EU" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("both"), Some(false),Some(false),Some(false),Some(false),Some(false),Some(false), Some(true)))

      val response = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-mix")).get

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.title() shouldBe "You may need to declare all your goods - Check tax on goods you bring into the UK - GOV.UK"
      status(response) shouldBe OK
    }
  }

  "Invoking POST .../duty-free-mix" should {

    "return error notification on the form when trying to submit a blank form" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("both"), Some(false),Some(false),Some(false),Some(false), Some(false),Some(false), Some(true)))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-mix")).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.select("input[name=bringingOverAllowance]").parents.find(_.tagName == "fieldset").get.select(".error-message").isEmpty shouldBe false
      doc.select("input[name=bringingOverAllowance]").parents.find(_.tagName == "fieldset").get.select(".error-message").html() shouldBe "Select yes if you are bringing in goods over your allowances, or you are unsure or undecided"
    }

    "redirect to the no need to use service page if they are not bringing in goods over their allowance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("both"), Some(false),Some(false),Some(false),Some(false), Some(false), Some(false), Some(true)))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-mix").withFormUrlEncodedBody("bringingOverAllowance" -> "false")).get

      status(response) shouldBe SEE_OTHER

      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/no-need-to-use-service")
    }

    "redirect to the private travel page if they are bringing in goods over their allowance" in new LocalSetup {

      override lazy val cachedJourneyData = Some(JourneyData(Some("both"), Some(false),Some(false),Some(false),Some(false),Some(false),Some(false), Some(true)))
      
      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/duty-free-mix").withFormUrlEncodedBody("bringingOverAllowance" -> "true")).get

      status(response) shouldBe SEE_OTHER

      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/private-travel")
    }
  }
}
