/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.AppConfig
import connectors.Cache
import models.UserInformation.getPreUser
import models._

import java.time.{LocalDate, LocalTime}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import repositories.BCPassengersSessionRepository
import services.PreviousDeclarationService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter, parseLocalDate, parseLocalTime}

import scala.concurrent.Future

class DeclarationRetrievalControllerSpec extends BaseSpec {
  val mockPreviousDeclarationService: PreviousDeclarationService = mock(classOf[PreviousDeclarationService])
  val mockCache: Cache                                           = mock(classOf[Cache])
  val mockAppConfig: AppConfig                                   = mock(classOf[AppConfig])

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[PreviousDeclarationService].toInstance(mockPreviousDeclarationService))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance(mockAppConfig))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPreviousDeclarationService)
    reset(mockCache)
    reset(mockAppConfig)
    when(
      injected[AppConfig].declareGoodsUrl
    ) thenReturn "https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods"
    when(injected[AppConfig].isAmendmentsEnabled) thenReturn true
  }

  "loadDeclarationRetrievalPage" should {

    "load the page when amendments feature is on" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(true)))))
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)
      doc.getElementsByTag("h1").text() shouldBe "Add goods to your previous declaration"
    }

    "redirect to start page when the amendments feature is off" in {
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn false
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(false)))))
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought")
    }

    "redirect to .../check-tax-on-goods-you-bring-into-the-uk when page is accessed after user says they have not made any previous declaration" in {
      val journeyData            = JourneyData(prevDeclaration = Some(false), previousDeclarationRequest = None)
      when(injected[AppConfig].isAmendmentsEnabled) thenReturn true
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData)))
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }

    "load the page with values from keyStore" in {
      val previousDeclarationDetails = PreviousDeclarationRequest("Smith", "XAPR1234567890")
      val journeyData                =
        JourneyData(prevDeclaration = Some(true), previousDeclarationRequest = Some(previousDeclarationDetails))
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData)))
      val result: Future[Result]     =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)
      doc.getElementsByTag("h1").text()             shouldBe "Add goods to your previous declaration"
      doc.getElementById("lastName").`val`()        shouldBe "Smith"
      doc.getElementById("referenceNumber").`val`() shouldBe "XAPR1234567890"
    }

    "load the page with empty form when no data in keyStore" in {
      val journeyData            = JourneyData(prevDeclaration = Some(true), previousDeclarationRequest = None)
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(journeyData)))
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)
      doc.getElementsByTag("h1").text()             shouldBe "Add goods to your previous declaration"
      doc.getElementById("lastName").`val`()        shouldBe ""
      doc.getElementById("referenceNumber").`val`() shouldBe ""
    }
  }

  "postDeclarationRetrievalPage" should {
    "redirect to .../where-goods-bought when user says they have not made any previous declaration" in {
      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(false))))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockPreviousDeclarationService.storePrevDeclarationDetails(any())(any())(any())) thenReturn cachedJourneyData
      val response          =
        route(app, enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")).get
      status(response)           shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")

    }

    "return a bad request when user does not fill in required fields" in {
      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(true))))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true

      val response = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
          .withFormUrlEncodedBody("lastName" -> "", "identificationNumber" -> "", "referenceNumber" -> "")
      ).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                shouldBe "Add goods to your previous declaration"
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      doc
        .getElementsByClass("govuk-error-summary__body")
        .select("a[href=#lastName]")
        .html()                                        shouldBe "Enter your last name"
      doc
        .getElementsByClass("govuk-error-summary__body")
        .select("a[href=#referenceNumber]")
        .html()                                        shouldBe "Enter your reference number"
      doc
        .getElementById("lastName-error")
        .parent()
        .getElementsByClass("govuk-error-message")
        .html()                                        shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Enter your last name"
      doc
        .getElementById("referenceNumber-error")
        .parent()
        .getElementsByClass("govuk-error-message")
        .html()                                        shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Enter your reference number"

    }

    "return a bad request when user enters invalid fields" in {
      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(true))))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true

      val response = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
          .withFormUrlEncodedBody(
            "" +
              "lastName"      -> "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
            "referenceNumber" -> "XXXX0123456789"
          )
      ).get

      status(response) shouldBe BAD_REQUEST

      val content = contentAsString(response)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                shouldBe "Add goods to your previous declaration"
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      doc
        .getElementsByClass("govuk-error-summary__body")
        .select("a[href=#lastName]")
        .html()                                        shouldBe "Last name must be 35 characters or less"
      doc
        .getElementsByClass("govuk-error-summary__body")
        .select("a[href=#referenceNumber]")
        .html()                                        shouldBe "Enter your reference number in the correct format"
      doc
        .getElementById("lastName-error")
        .parent()
        .getElementsByClass("govuk-error-message")
        .html()                                        shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Last name must be 35 characters or less"
      doc
        .getElementById("referenceNumber-error")
        .parent()
        .getElementsByClass("govuk-error-message")
        .html()                                        shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> Enter your reference number in the correct format"
      verify(mockPreviousDeclarationService, times(0)).storePrevDeclaration(any())(any())(any())

    }

    "redirect to previous goods page following a successful retrieval" in {

      val previousDeclarationRequest        = PreviousDeclarationRequest("Potter", "someReference")
      val calculation                       = Calculation("160.45", "25012.50", "15134.59", "40307.54")
      val productPath                       = ProductPath("other-goods/adult/adult-footwear")
      val country                           = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())
      val liabilityDetails                  = LiabilityDetails("32.0", "0.0", "126.4", "158.40")
      val userInformation                   = UserInformation(
        "Harry",
        "Smith",
        "passport",
        "SX12345",
        "abc@gmail.com",
        "Newcastle Airport",
        "",
        LocalDate.now(),
        LocalTime.now().minusHours(23)
      )
      val purchasedProductInstances         = List(
        PurchasedProductInstance(
          productPath,
          "UnOGll",
          None,
          None,
          Some(country),
          None,
          Some("GBP"),
          Some(500),
          Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))),
          Some(false),
          Some(false),
          None,
          Some(false),
          None,
          Some(false)
        )
      )
      val declarationResponse               = DeclarationResponse(calculation, liabilityDetails, purchasedProductInstances)
      val retrievedJourneyData: JourneyData = JourneyData(
        prevDeclaration = Some(true),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        ageOver17 = Some(true),
        isUKResident = Some(false),
        privateCraft = Some(true),
        previousDeclarationRequest = Some(previousDeclarationRequest),
        declarationResponse = Some(declarationResponse),
        preUserInformation = Some(getPreUser(userInformation))
      )
      when(mockCache.fetch(any())) thenReturn Future.successful(Some(retrievedJourneyData))
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
      when(mockPreviousDeclarationService.storePrevDeclarationDetails(any())(any())(any())) thenReturn Future
        .successful(Some(retrievedJourneyData))
      val response                          = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
          .withFormUrlEncodedBody(
            "" +
              "lastName"           -> "Smith",
            "identificationNumber" -> "12345",
            "referenceNumber"      -> "XXPR0123456789"
          )
      ).get
      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/previous-goods")
      verify(mockPreviousDeclarationService, times(1)).storePrevDeclarationDetails(any())(any())(any())
    }

    "redirect to declaration not found when a retrospective declaration of more than 24 hours is tried to retrieve" in {
      val previousDeclarationRequest = PreviousDeclarationRequest("Potter", "someReference")
      val calculation                = Calculation("160.45", "25012.50", "15134.59", "40307.54")
      val productPath                = ProductPath("other-goods/adult/adult-footwear")
      val country                    = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())
      val liabilityDetails           = LiabilityDetails("32.0", "0.0", "126.4", "158.40")
      val userInformation            = UserInformation(
        "Harry",
        "Smith",
        "passport",
        "SX12345",
        "abc@gmail.com",
        "Newcastle Airport",
        "",
        parseLocalDate("2021-04-01"),
        parseLocalTime("12:20 pm")
      )
      val purchasedProductInstances  = List(
        PurchasedProductInstance(
          productPath,
          "UnOGll",
          None,
          None,
          Some(country),
          None,
          Some("GBP"),
          Some(500),
          Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))),
          Some(false),
          Some(false),
          None,
          Some(false),
          None,
          Some(false)
        )
      )
      val declarationResponse        = DeclarationResponse(calculation, liabilityDetails, purchasedProductInstances)
      val cachedJourneyData          = Future.successful(
        Some(
          JourneyData(
            prevDeclaration = Some(true),
            euCountryCheck = Some("greatBritain"),
            arrivingNICheck = Some(true),
            ageOver17 = Some(true),
            isUKResident = Some(false),
            privateCraft = Some(true),
            previousDeclarationRequest = Some(previousDeclarationRequest),
            declarationResponse = Some(declarationResponse),
            preUserInformation = Some(getPreUser(userInformation))
          )
        )
      )
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
      when(mockPreviousDeclarationService.storePrevDeclarationDetails(any())(any())(any())) thenReturn cachedJourneyData
      val response                   = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
          .withFormUrlEncodedBody(
            "" +
              "lastName"           -> "Smith",
            "identificationNumber" -> "12345",
            "referenceNumber"      -> "XXPR0123456789"
          )
      ).get
      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/declaration-not-found")
    }

    "redirect to pending payment page following a successful retrieval" in {

      val previousDeclarationRequest        = PreviousDeclarationRequest("Potter", "someReference")
      val calculation                       = Calculation("160.45", "25012.50", "15134.59", "40307.54")
      val productPath                       = ProductPath("other-goods/adult/adult-footwear")
      val country                           = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())
      val liabilityDetails                  = LiabilityDetails("32.0", "0.0", "126.4", "158.40")
      val userInformation                   = UserInformation(
        "Harry",
        "Smith",
        "passport",
        "SX12345",
        "abc@gmail.com",
        "Newcastle Airport",
        "",
        LocalDate.now(),
        LocalTime.now().minusHours(23)
      )
      val purchasedProductInstances         = List(
        PurchasedProductInstance(
          productPath,
          "UnOGll",
          None,
          None,
          Some(country),
          None,
          Some("GBP"),
          Some(500),
          Some(OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))),
          Some(false),
          Some(false),
          None,
          Some(false),
          None,
          Some(false)
        )
      )
      val declarationResponse               = DeclarationResponse(calculation, liabilityDetails, purchasedProductInstances)
      val retrievedJourneyData: JourneyData = JourneyData(
        prevDeclaration = Some(true),
        euCountryCheck = Some("greatBritain"),
        arrivingNICheck = Some(true),
        ageOver17 = Some(true),
        isUKResident = Some(false),
        privateCraft = Some(true),
        previousDeclarationRequest = Some(previousDeclarationRequest),
        declarationResponse = Some(declarationResponse),
        preUserInformation = Some(getPreUser(userInformation)),
        amendState = Some("pending-payment")
      )
      when(mockCache.fetch(any())) thenReturn Future.successful(Some(retrievedJourneyData))
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
      when(mockPreviousDeclarationService.storePrevDeclarationDetails(any())(any())(any())) thenReturn Future
        .successful(Some(retrievedJourneyData))
      val response                          = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
          .withFormUrlEncodedBody(
            "" +
              "lastName"      -> "Smith",
            "referenceNumber" -> "XXPR0123456789"
          )
      ).get
      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/pending-payment")
      verify(mockPreviousDeclarationService, times(1)).storePrevDeclarationDetails(any())(any())(any())
    }

    "redirect to declaration not found on an unsuccessful POST " in {
      val cachedJourneyData = Future.successful(Some(JourneyData(prevDeclaration = Some(true))))
      when(mockCache.fetch(any())) thenReturn cachedJourneyData
      when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
      when(mockPreviousDeclarationService.storePrevDeclarationDetails(any())(any())(any())) thenReturn cachedJourneyData
      val response          = route(
        app,
        enhancedFakeRequest("POST", "/check-tax-on-goods-you-bring-into-the-uk/declaration-retrieval")
          .withFormUrlEncodedBody(
            "" +
              "lastName"           -> "Smith",
            "identificationNumber" -> "12345",
            "referenceNumber"      -> "XXPR0123456789"
          )
      ).get
      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk/declaration-not-found")
    }
  }

  "declarationNotFound" should {
    "load the declaration not found page" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(Some(true)))))
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-not-found")).get
      status(result) shouldBe OK
      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)
      doc.getElementsByTag("h1").text() shouldBe "Your declaration cannot be found"
    }
  }
}
