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
import models.*
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.mongo.MongoComponent
import util.BaseSpec

class StandardBackLinkModelSpec extends BaseSpec {

  override given app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(mock(classOf[BCPassengersSessionRepository])))
    .overrides(bind[MongoComponent].toInstance(mock(classOf[MongoComponent])))
    .overrides(bind[AppConfig].toInstance(mock(classOf[AppConfig])))
    .build()

  trait LocalSetup {

    val m: BackLinkModel = injected[BackLinkModel]

    def isIrishBorderQuestionEnabled: Boolean
    def prevDeclaration: Option[Boolean]               = None
    def euCountryCheck: Option[String]
    def isArrivingNi: Option[Boolean]                  = None
    def isUKVatPaid: Option[Boolean]                   = None
    def isUKVatExcisePaid: Option[Boolean]             = None
    def isUKResident: Option[Boolean]                  = None
    def isUccRelief: Option[Boolean]                   = None
    def isVatResClaimed: Option[Boolean]               = None
    def isBringingDutyFree: Option[Boolean]            = None
    def bringingOverAllowance: Option[Boolean]         = None
    def calculatorResponse: Option[CalculatorResponse] = None

    def call: Call

    lazy val journeyData: Option[JourneyData] = (
      prevDeclaration,
      euCountryCheck,
      isArrivingNi,
      isUKVatPaid,
      isUKVatExcisePaid,
      isUKResident,
      isUccRelief,
      isVatResClaimed,
      isBringingDutyFree,
      calculatorResponse
    ) match {
      case (None, None, None, None, None, None, None, None, None, None) => None
      case (
            prevDeclaration,
            euCountryCheck,
            isArrivingNi,
            isUKVatPaid,
            isUKVatExcisePaid,
            isUKResident,
            isUccRelief,
            isVatResClaimed,
            isBringingDutyFree,
            calculatorResponse
          ) =>
        Some(
          JourneyData(
            prevDeclaration = prevDeclaration,
            euCountryCheck = euCountryCheck,
            arrivingNICheck = isArrivingNi,
            isUKVatPaid = isUKVatPaid,
            isUKVatExcisePaid = isUKVatExcisePaid,
            isUKResident = isUKResident,
            isUccRelief = isUccRelief,
            isVatResClaimed = isVatResClaimed,
            isBringingDutyFree = isBringingDutyFree,
            bringingOverAllowance = bringingOverAllowance,
            calculatorResponse = calculatorResponse
          )
        )
    }

    lazy val crWithinLimitLow: CalculatorResponse = CalculatorResponse(
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
      Calculation("0.00", "0.00", "0.00", "9.00"),
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance = true
    )

    when(
      injected[AppConfig].declareGoodsUrl
    ).thenReturn("https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods")
    lazy val context: LocalContext = LocalContext(FakeRequest(call), "FAKESESSIONID", journeyData)
  }

  import routes._

  "Going back to previous-declaration" should {
    "happen when on where-goods-bought inside amendments journey" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled).thenReturn(true)
      override val isIrishBorderQuestionEnabled           = false
      override val prevDeclaration: Option[Boolean]       = None
      override val euCountryCheck: Option[String]         = None
      override val isArrivingNi: Option[Boolean]          = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = TravelDetailsController.whereGoodsBought

      m.backLink(context) shouldBe Some(routes.PreviousDeclarationController.loadPreviousDeclarationPage.url)
    }
  }

  "Going back to gov.uk start page" should {
    "happen when on where-goods-bought in normal journey" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled).thenReturn(false)
      override val isIrishBorderQuestionEnabled           = false
      override val prevDeclaration: Option[Boolean]       = None
      override val euCountryCheck: Option[String]         = None
      override val isArrivingNi: Option[Boolean]          = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = TravelDetailsController.whereGoodsBought

      m.backLink(context) shouldBe Some("https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods")
    }
  }

  "Going back to gov.uk start page" should {
    "happen when on previous-declaration in amendments journey" in new LocalSetup {

      when(injected[AppConfig].isAmendmentsEnabled).thenReturn(true)
      override val isIrishBorderQuestionEnabled           = false
      override val prevDeclaration: Option[Boolean]       = None
      override val euCountryCheck: Option[String]         = None
      override val isArrivingNi: Option[Boolean]          = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = PreviousDeclarationController.loadPreviousDeclarationPage

      m.backLink(context) shouldBe Some("https://www.gov.uk/duty-free-goods/declare-tax-or-duty-on-goods")
    }
  }

  "Going back to where-goods-bought" should {
    "happen when on arriving-ni" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isArrivingNi: Option[Boolean]          = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = ArrivingNIController.loadArrivingNIPage

      m.backLink(context) shouldBe Some(routes.TravelDetailsController.whereGoodsBought.url)
    }
  }

  "Going back to arriving-ni" should {

    "happen when on goods-brought-into-great-britain-iom and euOnly journey to GB" in new LocalSetup {

      override val isIrishBorderQuestionEnabled   = false
      override val euCountryCheck: Option[String] = Some("euOnly")
      override val isArrivingNi: Option[Boolean]  = Some(false)

      override def call: Call = TravelDetailsController.goodsBoughtIntoGB

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on goods-brought-into-great-britain-iom and nonEuOnly journey to GB" in new LocalSetup {

      override val isIrishBorderQuestionEnabled   = false
      override val euCountryCheck: Option[String] = Some("nonEuOnly")
      override val isArrivingNi: Option[Boolean]  = Some(false)

      override def call: Call = TravelDetailsController.goodsBoughtIntoGB

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on goods-brought-into-northern-ireland and nonEuOnly journey to NI" in new LocalSetup {

      override val isIrishBorderQuestionEnabled   = false
      override val euCountryCheck: Option[String] = Some("nonEuOnly")
      override val isArrivingNi: Option[Boolean]  = Some(true)

      override def call: Call = TravelDetailsController.goodsBoughtIntoNI

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on gb-ni-uk-resident-check and GB journey to NI" in new LocalSetup {

      override val isIrishBorderQuestionEnabled   = false
      override val euCountryCheck: Option[String] = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]  = Some(true)

      override def call: Call = UKResidentController.loadUKResidentPage

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on goods-bought-into-northern-ireland-inside-eu-check and euOnly journey to NI" in new LocalSetup {

      override val isIrishBorderQuestionEnabled   = false
      override val euCountryCheck: Option[String] = Some("euOnly")
      override val isArrivingNi: Option[Boolean]  = Some(true)

      override def call: Call = TravelDetailsController.goodsBoughtInsideEu

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }
  }

  "Going back to gb-ni-uk-resident-check" should {
    "happen when on gb-ni-vat-excise-check" in new LocalSetup {

      override val isIrishBorderQuestionEnabled   = false
      override val euCountryCheck: Option[String] = None

      override def call: Call = UKExcisePaidController.loadUKExcisePaidPage

      m.backLink(context) shouldBe Some(routes.UKResidentController.loadUKResidentPage.url)
    }
  }

  "Going back to gb-ni-uk-resident-check" should {

    "happen when on gb-ni-no-need-to-use-service" in new LocalSetup {

      override val isIrishBorderQuestionEnabled   = false
      override val euCountryCheck: Option[String] = None

      override def call: Call = TravelDetailsController.noNeedToUseServiceGbni

      m.backLink(context) shouldBe Some(routes.UKExcisePaidController.loadUKExcisePaidPage.url)
    }

    "happen when on goods-brought-into-northern-ireland for UK Resident in GB-NI flow" in new LocalSetup {

      override val isIrishBorderQuestionEnabled       = false
      override val euCountryCheck: Option[String]     = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]      = Some(true)
      override val isUKVatExcisePaid: Option[Boolean] = Some(false)
      override val isUKResident: Option[Boolean]      = Some(true)

      override def call: Call = TravelDetailsController.goodsBoughtIntoNI

      m.backLink(context) shouldBe Some(routes.UKExcisePaidController.loadUKExcisePaidPage.url)
    }
  }

  "Going back to gb-ni-uk-resident-check" should {
    "happen when on goods-brought-into-northern-ireland in GB-NI flow for non-UK Resident" in new LocalSetup {

      override val isIrishBorderQuestionEnabled   = false
      override val euCountryCheck: Option[String] = Some("greatBritain")
      override val isUKResident: Option[Boolean]  = Some(false)

      override def call: Call = TravelDetailsController.goodsBoughtIntoNI

      m.backLink(context) shouldBe Some(routes.UKResidentController.loadUKResidentPage.url)
    }
  }

  "Going back from private-travel" should {

    "return user to goods-brought-into-northern-ireland when destination is NI and bringingOverAllowance=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]          = Some(true)
      override val bringingOverAllowance: Option[Boolean] = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtIntoNI.url)
    }

    "return user to no-need-to-use-service when euCountryCheck=both and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("both")
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to goods-brought-into-great-britain-iom when destination is GB and bringingOverAllowance=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("nonEuOnly")
      override val isArrivingNi: Option[Boolean]          = Some(false)
      override val bringingOverAllowance: Option[Boolean] = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtIntoGB.url)
    }

    "return user to no-need-to-use-service when euCountryCheck=nonEuOnly and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("nonEuOnly")
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }
  }

  "Going back from no-need-to-use-service" should {

    "return user to goods-brought-into-northern-ireland when destination is NI" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("nonEuOnly")
      override val isArrivingNi: Option[Boolean]          = Some(true)
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtIntoNI.url)
    }

    "return user to goods-brought-into-great-britain-iom when destination is GB" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("nonEuOnly")
      override val isArrivingNi: Option[Boolean]          = Some(false)
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtIntoGB.url)
    }
  }

  "Going back from declaration-retrieval" should {

    "return user to previous-declaration" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = DeclarationRetrievalController.loadDeclarationRetrievalPage

      m.backLink(context) shouldBe Some(PreviousDeclarationController.loadPreviousDeclarationPage.url)
    }
  }

  "Going back from confirm-age" should {

    "return user to private-travel" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = TravelDetailsController.confirmAge

      m.backLink(context) shouldBe Some(TravelDetailsController.privateTravel.url)
    }
  }

  "Going back from tell-us" should {

    "return user to confirm-age for normal journey " in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None
      override val prevDeclaration: Option[Boolean]       = Some(false)

      override def call: Call = routes.DashboardController.showDashboard

      m.backLink(context) shouldBe Some(TravelDetailsController.confirmAge.url)
    }

    "return user to declaration-retrieval for amendment journey " in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None
      override val prevDeclaration: Option[Boolean]       = Some(true)

      override def call: Call = routes.DashboardController.showDashboard

      m.backLink(context) shouldBe Some(PreviousGoodsController.showPreviousGoods.url)
    }

    "return user to confirm-age for previous declaration journey " in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None
      override val prevDeclaration: Option[Boolean]       = Some(true)

      override def call: Call = routes.DashboardController.showDashboard

      m.backLink(context) shouldBe Some(PreviousGoodsController.showPreviousGoods.url)
    }
  }

  "Going back from user-information-name" should {

    "return user to declare-your-goods" in new LocalSetup {

      override val isIrishBorderQuestionEnabled                   = false
      override val euCountryCheck: Option[String]                 = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]                  = Some(true)
      override val calculatorResponse: Option[CalculatorResponse] = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.whatIsYourName

      m.backLink(context) shouldBe Some(CalculateDeclareController.declareYourGoods.url)
    }
  }

  "Going back from user-information-id" should {

    "return user to user-information-name" in new LocalSetup {

      override val isIrishBorderQuestionEnabled                   = false
      override val euCountryCheck: Option[String]                 = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]                  = Some(true)
      override val calculatorResponse: Option[CalculatorResponse] = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.processTypeOfIdentification

      m.backLink(context) shouldBe Some(CalculateDeclareController.whatIsYourName.url)
    }
  }

  "Going back from user-information-id-number" should {

    "return user to user-information-id" in new LocalSetup {

      override val isIrishBorderQuestionEnabled                   = false
      override val euCountryCheck: Option[String]                 = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]                  = Some(true)
      override val calculatorResponse: Option[CalculatorResponse] = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.processIdentificationNumber

      m.backLink(context) shouldBe Some(CalculateDeclareController.typeOfIdentification.url)
    }
  }

  "Going back from user-information-email" should {

    "return user to user-information-id-number" in new LocalSetup {

      override val isIrishBorderQuestionEnabled                   = false
      override val euCountryCheck: Option[String]                 = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]                  = Some(true)
      override val calculatorResponse: Option[CalculatorResponse] = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.processWhatIsYourEmail

      m.backLink(context) shouldBe Some(CalculateDeclareController.whatIsYourIdentificationNumber.url)
    }
  }

  "Going back from declare-your-goods" should {

    "return user to tax-due" in new LocalSetup {

      override val isIrishBorderQuestionEnabled                   = false
      override val euCountryCheck: Option[String]                 = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]                  = Some(true)
      override val calculatorResponse: Option[CalculatorResponse] = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.declareYourGoods

      m.backLink(context) shouldBe Some(CalculateDeclareController.showCalculation.url)
    }
  }

  "Going back from tax-due" should {

    "return user to tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled                   = false
      override val euCountryCheck: Option[String]                 = Some("greatBritain")
      override val isArrivingNi: Option[Boolean]                  = Some(true)
      override val calculatorResponse: Option[CalculatorResponse] = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.showCalculation

      m.backLink(context) shouldBe Some(DashboardController.showDashboard.url)
    }
  }

  "Going back from ireland-to-northern-ireland" should {

    "return user to /tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.CalculateDeclareController.irishBorder

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }

  "Going back from ireland-to-northern-ireland" should {

    "return user to /tell-us when irishBorder is true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = true
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.CalculateDeclareController.irishBorder

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }

    "return user to /tell-us when irishBorder is false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.CalculateDeclareController.irishBorder

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }

  "Going back from declaration-not-found for declaration-retrieval page" should {

    "return user to the edit declaration-retrieval page" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isArrivingNi: Option[Boolean]          = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.DeclarationRetrievalController.declarationNotFound

      m.backLink(context) shouldBe Some(routes.DeclarationRetrievalController.loadDeclarationRetrievalPage.url)
    }
  }

  "Going back from pending-payment for pending-payment page" should {

    "return user to the declaration-retrieval page" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isArrivingNi: Option[Boolean]          = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.PendingPaymentController.loadPendingPaymentPage

      m.backLink(context) shouldBe Some(routes.DeclarationRetrievalController.loadDeclarationRetrievalPage.url)
    }
  }

  "Going back from no further amendment for pending-payment page" should {

    "return user to the pending payment page" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isArrivingNi: Option[Boolean]          = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.PendingPaymentController.noFurtherAmendment

      m.backLink(context) shouldBe Some(routes.PendingPaymentController.loadPendingPaymentPage.url)
    }
  }
}
