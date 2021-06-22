/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import connectors.Cache
import models.{Calculation, CalculatorResponse, JourneyData}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, route, status, _}
import repositories.BCPassengersSessionRepository
import services.{CalculatorService, CalculatorServiceCantBuildCalcReqResponse, CalculatorServiceSuccessResponse}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class PendingPaymentControllerSpec extends BaseSpec {

  val calculatorService: CalculatorService = MockitoSugar.mock[CalculatorService]
  val mockCache: Cache = MockitoSugar.mock[Cache]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[CalculatorService].toInstance(calculatorService))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(calculatorService, mockCache)

  }

  "loadPendingPaymentPage" should {
    "load the pending payment page" in {

      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(deltaCalculation = Some(Calculation("1.00", "7.00", "90000.00", "98000.00")),
        prevDeclaration = Some(true)
      ))))
      when(injected[CalculatorService].calculate(any())(any(), any())) thenReturn Future.successful(CalculatorServiceSuccessResponse(CalculatorResponse(None, None, None, Calculation("0.00", "0.00", "0.00", "0.00"), withinFreeAllowance = true, Map.empty, isAnyItemOverAllowance = false)))
      when(injected[CalculatorService].storeCalculatorResponse(any(), any(), any())(any())) thenReturn Future.successful(JourneyData())
      when(injected[CalculatorService].getPreviousPaidCalculation(any(), any())) thenReturn Calculation("10.00", "10.00", "10.00", "30.00")

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/pending-payment")).get

      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "You have an incomplete payment for your declaration for £98,000.00"
    }
    "load the pending payment page when pending-payment = true in journey data" in {

      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(deltaCalculation = Some(Calculation("1.00", "7.00", "90000.00", "98000.00")),
        prevDeclaration = Some(true),
        pendingPayment = Some(true)
      ))))
      when(injected[CalculatorService].calculate(any())(any(), any())) thenReturn Future.successful(CalculatorServiceSuccessResponse(CalculatorResponse(None, None, None, Calculation("0.00", "0.00", "0.00", "0.00"), withinFreeAllowance = true, Map.empty, isAnyItemOverAllowance = false)))
      when(injected[CalculatorService].storeCalculatorResponse(any(), any(), any())(any())) thenReturn Future.successful(JourneyData())
      when(injected[CalculatorService].getPreviousPaidCalculation(any(), any())) thenReturn Calculation("10.00", "10.00", "10.00", "30.00")

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/pending-payment")).get

      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "You have an incomplete payment for your declaration for £98,000.00"
      doc.select("#pendingPayment-true").hasAttr("checked") shouldBe true
    }

    "return 500 when calculator response missing while load the pending payment page" in {

      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(deltaCalculation = Some(Calculation("1.00", "7.00", "90000.00", "98000.00")),
        prevDeclaration = Some(true)
      ))))
      when(injected[CalculatorService].calculate(any())(any(), any())) thenReturn Future.successful(CalculatorServiceCantBuildCalcReqResponse)
      when(injected[CalculatorService].storeCalculatorResponse(any(), any(), any())(any())) thenReturn Future.successful(JourneyData())
      when(injected[CalculatorService].getPreviousPaidCalculation(any(), any())) thenReturn Calculation("10.00", "10.00", "0.00", "20.00")

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/pending-payment")).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "postPendingPaymentPage" should {

    "redirect to .../no-further-amendments when user says they don't want to pay pending payment" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(calculatorResponse = Some(CalculatorResponse(None, None, None, Calculation("0.00", "0.00", "0.00", "0.00"), withinFreeAllowance = true, Map.empty, isAnyItemOverAllowance = false)),
        prevDeclaration = Some(true)
      )))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.storeJourneyData(any())(any())) thenReturn Future.successful(Some(JourneyData()))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/pending-payment")
        .withFormUrlEncodedBody("pendingPayment" -> "false")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/no-further-amendments")


    }

    "redirect to .../process-amendment when user says they want to pay pending payment" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(calculatorResponse = Some(CalculatorResponse(None, None, None, Calculation("0.00", "0.00", "0.00", "0.00"), withinFreeAllowance = true, Map.empty, isAnyItemOverAllowance = false)),
        prevDeclaration = Some(true)
      )))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.storeJourneyData(any())(any())) thenReturn Future.successful(Some(JourneyData()))

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/pending-payment")
        .withFormUrlEncodedBody("pendingPayment" -> "true")).get

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/process-amendment")


    }

    "return a bad request when user selects an invalid value in pending payment" in  {

      val cachedJourneyData = Future.successful(Some(JourneyData(calculatorResponse = Some(CalculatorResponse(None, None, None, Calculation("0.00", "0.00", "0.00", "0.00"), withinFreeAllowance = true, Map.empty, isAnyItemOverAllowance = false)),
        prevDeclaration = Some(true),
        deltaCalculation = Some(Calculation("0.00", "0.00", "0.00", "0.00"))
      )))

      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockCache.storeJourneyData(any())(any())) thenReturn Future.successful(Some(JourneyData()))
      when(injected[CalculatorService].getPreviousPaidCalculation(any(), any())) thenReturn Calculation("0.00", "0.00", "0.00", "0.00")

      val response = route(app, EnhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/pending-payment")
        .withFormUrlEncodedBody()).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "You have an incomplete payment for your declaration for £0.00"
      doc.select("#error-heading").text() shouldBe "There is a problem"
      doc.getElementById("errors").select("a[href=#pendingPayment]").html() shouldBe "Select yes if you want to pay now"
      doc.getElementById("pendingPayment-error-message").getElementsByClass("error-message").html() shouldBe "<span class=\"visually-hidden\">Error: </span> Select yes if you want to pay now"


    }
  }

  "load noFurtherAmendment" should {
    "load the no further amendment page" in {
     when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(deltaCalculation = Some(Calculation("1.00", "7.00", "90000.00", "98000.00")),
        prevDeclaration = Some(true)
      ))))

      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/no-further-amendments")).get

      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "You can no longer use this service to add goods to your declaration"
    }
  }
}
