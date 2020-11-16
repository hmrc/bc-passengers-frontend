/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import models.{Alcohol, Band, Calculation, CalculatorResponse, Country, Currency, ExchangeRate, Item, JourneyData, Metadata, OtherGoods, Tobacco}
import models.{IdentifierRequest, JourneyData}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import repositories.BCPassengersSessionRepository
import util.BaseSpec

class StandardBackLinkModelSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[AppConfig].toInstance(MockitoSugar.mock[AppConfig]))
    .build()


  trait LocalSetup {

    val m = injected[BackLinkModel]

    def isIrishBorderQuestionEnabled: Boolean
    def euCountryCheck: Option[String]
    def isArrivingNi: Option[Boolean] = None
    def isUKVatPaid: Option[Boolean] = None
    def isUKExcisePaid: Option[Boolean] = None
    def isUKResident: Option[Boolean] = None
    def isUccRelief: Option[Boolean] = None
    def isVatResClaimed: Option[Boolean]= None
    def isBringingDutyFree: Option[Boolean]= None
    def bringingOverAllowance: Option[Boolean]= None
    def calculatorResponse: Option[CalculatorResponse] = None

    def call: Call

    lazy val journeyData = (euCountryCheck, isArrivingNi,isUKVatPaid,isUKExcisePaid,isUKResident,isUccRelief, isVatResClaimed, isBringingDutyFree, calculatorResponse) match {
      case (None, None, None, None,None, None, None, None, None) => None
      case (euCountryCheck, isArrivingNi,isUKVatPaid,isUKExcisePaid,isUKResident,isUccRelief, isVatResClaimed, isBringingDutyFree, calculatorResponse) =>
        Some(JourneyData(euCountryCheck = euCountryCheck, arrivingNICheck = isArrivingNi, isUKVatPaid = isUKVatPaid,isUKExcisePaid=isUKExcisePaid,
          isUKResident=isUKResident,isUccRelief=isUccRelief,isVatResClaimed = isVatResClaimed, isBringingDutyFree = isBringingDutyFree,
          bringingOverAllowance = bringingOverAllowance, calculatorResponse = calculatorResponse))
    }

    lazy val crWithinLimitLow: CalculatorResponse = CalculatorResponse(
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
          Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc","100.00", Currency("USD", "USA Dollar (USD)", Some("USD"), Nil), Country("US", "United States of America (the)", "US", isEu = false, Nil), ExchangeRate("1.20", "2018-10-29")))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))
      ), Calculation("0.00", "0.00", "0.00", "0.00"))),
      Calculation("0.00", "0.00", "0.00", "9.00"),
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance =true
    )

    lazy val context = LocalContext(IdentifierRequest(FakeRequest(call), "somePID"), "FAKESESSIONID", journeyData)
  }

  import routes._

  "Going back to where-goods-bought" should {
    "happen when on arriving-ni" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = None
      override val isArrivingNi  = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = ArrivingNIController.loadArrivingNIPage

      m.backLink(context) shouldBe Some(routes.TravelDetailsController.whereGoodsBought.url)
    }
  }

  "Going back to arriving-ni" should {

    "happen when on goods-brought-into-great-britain-iom and euOnly journey to GB" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("euOnly")
      override val isArrivingNi  = Some(false)

      override def call: Call = TravelDetailsController.goodsBoughtIntoGB

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on goods-brought-into-great-britain-iom and nonEuOnly journey to GB" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("nonEuOnly")
      override val isArrivingNi  = Some(false)

      override def call: Call = TravelDetailsController.goodsBoughtIntoGB

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on goods-brought-into-northern-ireland and nonEuOnly journey to NI" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("nonEuOnly")
      override val isArrivingNi  = Some(true)

      override def call: Call = TravelDetailsController.goodsBoughtIntoNI

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on gb-ni-vat-check and GB journey to NI" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("greatBritain")
      override val isArrivingNi  = Some(true)

      override def call: Call = UKVatPaidController.loadUKVatPaidPage

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on goods-bought-into-northern-ireland-inside-eu-check and euOnly journey to NI" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("euOnly")
      override val isArrivingNi  = Some(true)

      override def call: Call = TravelDetailsController.goodsBoughtInsideEu

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }
  }

  "Going back to gb-ni-vat-check" should {
    "happen when on gb-ni-excise-check" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = None

      override def call: Call = UKExcisePaidController.loadUKExcisePaidPage

      m.backLink(context) shouldBe Some(routes.UKVatPaidController.loadUKVatPaidPage.url)
    }
  }

  "Going back to gb-ni-excise-check" should {
    "happen when on gb-ni-uk-resident-check" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = None

      override def call: Call = UKResidentController.loadUKResidentPage

      m.backLink(context) shouldBe Some(routes.UKExcisePaidController.loadUKExcisePaidPage.url)
    }
  }

  "Going back to gb-ni-uk-resident-check" should {

    "happen when on gb-ni-exemptions" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = None

      override def call: Call = UccReliefController.loadUccReliefPage

      m.backLink(context) shouldBe Some(routes.UKResidentController.loadUKResidentPage.url)
    }

    "happen when on gb-ni-no-need-to-use-service" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = None

      override def call: Call = TravelDetailsController.noNeedToUseServiceGbni()

      m.backLink(context) shouldBe Some(routes.UKResidentController.loadUKResidentPage.url)
    }

    "happen when on goods-brought-into-northern-ireland for UK Resident in GB-NI flow" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("greatBritain")
      override val isArrivingNi = Some(true)
      override val isUKVatPaid = Some(true)
      override val isUKExcisePaid = Some(false)
      override val isUKResident = Some(true)

      override def call: Call = TravelDetailsController.goodsBoughtIntoNI()

      m.backLink(context) shouldBe Some(routes.UKResidentController.loadUKResidentPage.url)
    }
  }

  "Going back to gb-ni-exemptions" should {
    "happen when on goods-brought-into-northern-ireland in GB-NI flow for non-UK Resident" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("greatBritain")
      override val isUKResident = Some(false)

      override def call: Call = TravelDetailsController.goodsBoughtIntoNI()

      m.backLink(context) shouldBe Some(routes.UccReliefController.loadUccReliefPage().url)
    }
  }

  "Going back from private-travel" should {

    "return user to goods-brought-into-northern-ireland when destination is NI and bringingOverAllowance=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("greatBritain")
      override val isArrivingNi  = Some(true)
      override val bringingOverAllowance = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtIntoNI.url)
    }


    "return user to no-need-to-use-service when euCountryCheck=both and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("both")
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to goods-brought-into-great-britain-iom when destination is GB and bringingOverAllowance=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("nonEuOnly")
      override val isArrivingNi  = Some(false)
      override val bringingOverAllowance = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtIntoGB.url)
    }

    "return user to no-need-to-use-service when euCountryCheck=nonEuOnly and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = Some("nonEuOnly")
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }
  }

  "Going back from no-need-to-use-service" should {

    "return user to goods-brought-into-northern-ireland when destination is NI" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck = Some("nonEuOnly")
      override val isArrivingNi  = Some(true)
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtIntoNI.url)
    }

    "return user to goods-brought-into-great-britain-iom when destination is GB" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck = Some("nonEuOnly")
      override val isArrivingNi  = Some(false)
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtIntoGB.url)
    }
  }

  "Going back from confirm-age" should {

    "return user to private-travel" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.confirmAge

      m.backLink(context) shouldBe Some(TravelDetailsController.privateTravel.url)
    }
  }

  "Going back from tell-us" should {

    "return user to confirm-age" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.DashboardController.showDashboard

      m.backLink(context) shouldBe Some(TravelDetailsController.confirmAge.url)
    }
  }

  "Going back from user-information" should {

    "return user to declare-your-goods" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck = Some("greatBritain")
      override val isArrivingNi = Some(true)
      override val calculatorResponse = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.enterYourDetails()

      m.backLink(context) shouldBe Some(CalculateDeclareController.declareYourGoods().url)
    }
  }

  "Going back from declare-your-goods" should {

    "return user to tax-due" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck = Some("greatBritain")
      override val isArrivingNi = Some(true)
      override val calculatorResponse = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.declareYourGoods()

      m.backLink(context) shouldBe Some(CalculateDeclareController.showCalculation().url)
    }
  }

  "Going back from tax-due" should {

    "return user to tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck = Some("greatBritain")
      override val isArrivingNi = Some(true)
      override val calculatorResponse = Some(crWithinLimitLow)

      override def call: Call = CalculateDeclareController.showCalculation()

      m.backLink(context) shouldBe Some(DashboardController.showDashboard().url)
    }
  }

  "Going back from ireland-to-northern-ireland" should {

    "return user to /tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.CalculateDeclareController.irishBorder

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }

  "Going back from ireland-to-northern-ireland" should {

    "return user to /tell-us when irishBorder is true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = true
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.CalculateDeclareController.irishBorder

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }

    "return user to /tell-us when irishBorder is false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.CalculateDeclareController.irishBorder

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }
}
