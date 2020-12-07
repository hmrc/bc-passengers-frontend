/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import models._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{LocalDate, LocalTime}
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
import services.{DeclarationService, DeclarationServiceFailureResponse, DeclarationServiceSuccessResponse}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter}

import scala.concurrent.Future

class ZeroDeclarationControllerSpec extends BaseSpec {

  val mockCache: Cache = MockitoSugar.mock[Cache]
  val mockAppConfig: AppConfig = MockitoSugar.mock[AppConfig]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[DeclarationService].toInstance(MockitoSugar.mock[DeclarationService]))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance(mockAppConfig))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCache, mockAppConfig, injected[DeclarationService])
  }

  lazy val crZeroTax: CalculatorResponse = CalculatorResponse(
    Some(Alcohol(List(
      Band("A", List(
        Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))
    ), Calculation("0.00", "0.00", "0.00", "0.00"))),
    Some(Tobacco(List(
      Band("A", List(
        Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))
    ), Calculation("0.00", "0.00", "0.00", "0.00"))),
    Some(OtherGoods(List(
      Band("A", List(
        Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))
    ), Calculation("0.00", "0.00", "0.00", "0.00"))),
    Calculation("0.00", "0.00", "0.00", "0.00"),
    withinFreeAllowance = true,
    limits = Map.empty,
    isAnyItemOverAllowance = false
  )

  lazy val ui: UserInformation = UserInformation("Harry", "Potter", "passport", "SX12345", "abc@gmail.com", "LHR", "", LocalDate.parse("2018-11-12"), LocalTime.parse("12:20 pm", DateTimeFormat.forPattern("hh:mm aa")))

  "loadDeclarationPage" should {

    "load the page" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())) thenReturn Future.successful(DeclarationServiceSuccessResponse)
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false), Some("greatBritain"), arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crZeroTax), chargeReference= Some("XJPR5768524625"), userInformation = Some(ui)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Declaration complete"
    }

    "loading the page and populate data from keyStore when place of arrival is selected" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())) thenReturn Future.successful(DeclarationServiceSuccessResponse)
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false), Some("greatBritain"), arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crZeroTax), chargeReference= Some("XJPR5768524625"), userInformation = Some(ui)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Declaration complete"
      doc.getElementsByClass("govuk-panel__body").text() shouldBe "Your reference number XJPR5768524625"
    }

    "loading the page and populate data from keyStore when place of arrival is entered" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())) thenReturn Future.successful(DeclarationServiceSuccessResponse)
      val userInformationMock = ui.copy(selectPlaceOfArrival = "", enterPlaceOfArrival = "Belfast Seaport")
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false), Some("greatBritain"), arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crZeroTax), chargeReference= Some("XJPR5768524625"), userInformation = Some(userInformationMock)))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Declaration complete"
      doc.getElementsByClass("govuk-panel__body").text() shouldBe "Your reference number XJPR5768524625"
    }

    "redirect to the start page when there is no journey data" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }

    "return a INTERNAL_SERVER_ERROR for update if the declaration returns 500" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())) thenReturn Future.successful(DeclarationServiceFailureResponse)

      val userInformationMock = ui.copy(selectPlaceOfArrival = "", enterPlaceOfArrival = "Belfast Seaport")
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false), Some("greatBritain"), arrivingNICheck = Some(true), isVatResClaimed = None, isBringingDutyFree = None, bringingOverAllowance = Some(true), ageOver17 = Some(true), privateCraft = Some(false), calculatorResponse = Some(crZeroTax), chargeReference = Some("XJPR5768524625"), userInformation = Some(userInformationMock)))))
      val response: Future[Result] = route(app, EnhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get

      status(response) shouldBe INTERNAL_SERVER_ERROR

    }
  }

}

