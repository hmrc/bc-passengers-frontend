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
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, route, status, *}
import repositories.BCPassengersSessionRepository
import services.{DeclarationService, DeclarationServiceFailureResponse, DeclarationServiceSuccessResponse}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import util.{BaseSpec, FakeSessionCookieCryptoFilter, parseLocalDate, parseLocalTime}

import scala.concurrent.Future

class ZeroDeclarationControllerSpec extends BaseSpec {

  val mockCache: Cache         = mock(classOf[Cache])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[DeclarationService].toInstance(mock(classOf[DeclarationService])))
    .overrides(bind[Cache].toInstance(mockCache))
    .overrides(bind[SessionCookieCryptoFilter].to[FakeSessionCookieCryptoFilter])
    .overrides(bind[AppConfig].toInstance(mockAppConfig))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCache)
    reset(mockAppConfig)
    reset(injected[DeclarationService])
  }

  lazy val crZeroTax: CalculatorResponse                                = CalculatorResponse(
    Some(
      Alcohol(
        List(
          Band(
            "A",
            List(
              Item(
                "ANYTHING",
                "100.00",
                Some(1),
                None,
                Calculation("0.00", "0.00", "0.00", "0.00"),
                Metadata(
                  "Desc",
                  "Desc",
                  "100.00",
                  DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                  Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                  Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                  ExchangeRate("1.20", "2018-10-29"),
                  None
                ),
                None,
                None,
                None,
                None
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Calculation("0.00", "0.00", "0.00", "0.00")
      )
    ),
    Some(
      Tobacco(
        List(
          Band(
            "A",
            List(
              Item(
                "ANYTHING",
                "100.00",
                Some(1),
                None,
                Calculation("0.00", "0.00", "0.00", "0.00"),
                Metadata(
                  "Desc",
                  "Desc",
                  "100.00",
                  DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                  Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                  Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                  ExchangeRate("1.20", "2018-10-29"),
                  None
                ),
                None,
                None,
                None,
                None
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Calculation("0.00", "0.00", "0.00", "0.00")
      )
    ),
    Some(
      OtherGoods(
        List(
          Band(
            "A",
            List(
              Item(
                "ANYTHING",
                "100.00",
                Some(1),
                None,
                Calculation("0.00", "0.00", "0.00", "0.00"),
                Metadata(
                  "Desc",
                  "Desc",
                  "100.00",
                  DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                  Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                  Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                  ExchangeRate("1.20", "2018-10-29"),
                  None
                ),
                None,
                None,
                None,
                None
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Calculation("0.00", "0.00", "0.00", "0.00")
      )
    ),
    Calculation("0.00", "0.00", "0.00", "0.00"),
    withinFreeAllowance = true,
    limits = Map.empty,
    isAnyItemOverAllowance = false
  )
  lazy val oldAlcohol: PurchasedProductInstance                         = PurchasedProductInstance(
    ProductPath("alcohol/beer"),
    "iid0",
    Some(1.54332),
    None,
    Some(Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil)),
    None,
    Some("AUD"),
    Some(BigDecimal(10.234)),
    None,
    None,
    None,
    isEditable = Some(false)
  )
  lazy val oldPurchasedProductInstances: List[PurchasedProductInstance] = List(oldAlcohol)
  lazy val calculation: Calculation                                     = Calculation("1.00", "1.00", "1.00", "3.00")
  lazy val liabilityDetails: LiabilityDetails                           = LiabilityDetails("32.0", "0.0", "126.4", "158.40")
  lazy val declarationResponse: DeclarationResponse                     = DeclarationResponse(
    calculation = calculation,
    oldPurchaseProductInstances = oldPurchasedProductInstances,
    liabilityDetails = liabilityDetails
  )
  lazy val deltaCalculation: Calculation                                = Calculation("1.00", "1.00", "1.00", "3.00")
  lazy val zeroDeltaCalculation: Calculation                            = Calculation("0.00", "0.00", "0.00", "0.00")

  lazy val ui: UserInformation = UserInformation(
    "Harry",
    "Potter",
    "passport",
    "SX12345",
    "abc@gmail.com",
    "LHR",
    "",
    parseLocalDate("2018-11-12"),
    parseLocalTime("12:20 pm")
  )

  "loadDeclarationPage" should {

    "load the page for declaration" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())).thenReturn(
        Future.successful(
          DeclarationServiceSuccessResponse
        )
      )
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(
            JourneyData(
              Some(false),
              Some("greatBritain"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crZeroTax),
              chargeReference = Some("XJPR5768524625"),
              preUserInformation = Some(getPreUser(ui))
            )
          )
        )
      )
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text() shouldBe "Declaration complete"
      content                             should include("Make a note of your reference number, you may need to provide it to Border Force.")
      content                             should include("Your declaration receipt has been sent to the email address you provided.")
      content                             should include(
        "You can use this service to add goods to your existing declaration before you arrive in the UK. You will need to enter your reference number."
      )
      content                             should not include "Amount paid previously"
      content                             should not include "Total due now"
    }

    "load the page for amendments" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())).thenReturn(
        Future.successful(
          DeclarationServiceSuccessResponse
        )
      )
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(
            JourneyData(
              Some(true),
              Some("greatBritain"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crZeroTax),
              declarationResponse = Some(declarationResponse),
              deltaCalculation = Some(deltaCalculation),
              chargeReference = Some("XJPR5768524625"),
              preUserInformation = Some(getPreUser(ui))
            )
          )
        )
      )
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()      shouldBe "Declaration complete"
      content                                  should include(
        "Make a note of your reference number, you may need to provide it to Border Force. This is the same reference number as your previous declaration for this journey."
      )
      content                                  should include("Your amended declaration receipt has been sent to the email address you provided.")
      content                                  should include(
        "You can use this service to add goods to your existing declaration before you arrive in the UK. You will need to enter your reference number."
      )
      doc.getElementById("prev-paid").text() shouldBe "Amount paid previously"
      doc.getElementById("oldAllTax").text() shouldBe "£3.00"
      doc.getElementById("total").text()     shouldBe "Total paid now"
      doc.getElementById("allTax").text()    shouldBe "£3.00"
    }

    "loading the page and populate data from keyStore when place of arrival is selected" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())).thenReturn(
        Future.successful(
          DeclarationServiceSuccessResponse
        )
      )
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(
            JourneyData(
              Some(false),
              Some("greatBritain"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crZeroTax),
              declarationResponse = Some(declarationResponse),
              chargeReference = Some("XJPR5768524625"),
              preUserInformation = Some(getPreUser(ui))
            )
          )
        )
      )
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                  shouldBe "Declaration complete"
      doc.getElementsByClass("govuk-panel__body").text() shouldBe "Your reference number XJPR5768524625"
    }

    "loading the page and populate data from keyStore when place of arrival is entered" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())).thenReturn(
        Future.successful(
          DeclarationServiceSuccessResponse
        )
      )
      val userInformationMock    = ui.copy(selectPlaceOfArrival = "", enterPlaceOfArrival = "Belfast Seaport")
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(
            JourneyData(
              Some(false),
              Some("greatBritain"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crZeroTax),
              declarationResponse = Some(declarationResponse),
              chargeReference = Some("XJPR5768524625"),
              preUserInformation = Some(getPreUser(userInformationMock))
            )
          )
        )
      )
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result) shouldBe OK

      val content = contentAsString(result)
      val doc     = Jsoup.parse(content)

      doc.getElementsByTag("h1").text()                  shouldBe "Declaration complete"
      doc.getElementsByClass("govuk-panel__body").text() shouldBe "Your reference number XJPR5768524625"
    }

    "redirect to the start page when there is no journey data" in {
      when(mockCache.fetch(any())).thenReturn(Future.successful(Some(JourneyData(None))))
      val result: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-tax-on-goods-you-bring-into-the-uk")
    }

    "return a INTERNAL_SERVER_ERROR for update if the declaration returns 500" in {
      when(injected[DeclarationService].updateDeclaration(any())(any())).thenReturn(
        Future.successful(
          DeclarationServiceFailureResponse
        )
      )

      val userInformationMock      = ui.copy(selectPlaceOfArrival = "", enterPlaceOfArrival = "Belfast Seaport")
      when(mockCache.fetch(any())).thenReturn(
        Future.successful(
          Some(
            JourneyData(
              Some(false),
              Some("greatBritain"),
              arrivingNICheck = Some(true),
              isVatResClaimed = None,
              isBringingDutyFree = None,
              bringingOverAllowance = Some(true),
              ageOver17 = Some(true),
              privateCraft = Some(false),
              calculatorResponse = Some(crZeroTax),
              chargeReference = Some("XJPR5768524625"),
              preUserInformation = Some(getPreUser(userInformationMock))
            )
          )
        )
      )
      val response: Future[Result] =
        route(app, enhancedFakeRequest("GET", "/check-tax-on-goods-you-bring-into-the-uk/declaration-complete")).get

      status(response) shouldBe INTERNAL_SERVER_ERROR

    }
  }

}
