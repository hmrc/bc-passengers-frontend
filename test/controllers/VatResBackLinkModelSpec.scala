/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import models.JourneyData
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import util.BaseSpec
import play.api.inject.bind
import repositories.BCPassengersSessionRepository

class VatResBackLinkModelSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[BCPassengersSessionRepository].toInstance(MockitoSugar.mock[BCPassengersSessionRepository]))
    .overrides(bind[AppConfig].toInstance(MockitoSugar.mock[AppConfig]))
    .build()

  trait LocalSetup {

    val m: BackLinkModel = injected[BackLinkModel]

    def isIrishBorderQuestionEnabled: Boolean
    def prevDeclaration: Option[Boolean] = None
    def euCountryCheck: Option[String]
    def arrivingNICheck: Option[Boolean] = None
    def isVatResClaimed: Option[Boolean]
    def isBringingDutyFree: Option[Boolean]
    def bringingOverAllowance: Option[Boolean]


    def call: Call

    lazy val journeyData: Option[JourneyData] = (prevDeclaration, euCountryCheck, arrivingNICheck, isVatResClaimed, isBringingDutyFree) match {
      case (None, None,None, None, None) => None
      case (prevDeclaration, euCountryCheck, arrivingNICheck, isVatResClaimed, isBringingDutyFree) =>
        Some(JourneyData(prevDeclaration = prevDeclaration, euCountryCheck = euCountryCheck,arrivingNICheck = arrivingNICheck, isVatResClaimed = isVatResClaimed, isBringingDutyFree = isBringingDutyFree, bringingOverAllowance = bringingOverAllowance))
    }

    lazy val context: LocalContext = {

      when(injected[AppConfig].isVatResJourneyEnabled) thenReturn true
      when(injected[AppConfig].isIrishBorderQuestionEnabled) thenReturn isIrishBorderQuestionEnabled

      LocalContext(FakeRequest(call), "FAKESESSIONIN", journeyData)
    }
  }

  override def beforeEach: Unit = {
    reset(injected[AppConfig])
  }

  import routes._

  "Going back to did-you-claim-tax-back" should {

    "happen when on duty-free" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = None
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = TravelDetailsController.dutyFree()

      m.backLink(context) shouldBe Some(TravelDetailsController.didYouClaimTaxBack().url)
    }

  }

  "Going back from private-travel" should {

    "return user to no-need-to-use-this-service when euCountryCheck=both and isVatResClaimed=false and isBringingDutyFree=false and bringingOverAllowance = false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = Some("both")
      override val isVatResClaimed: Option[Boolean] = Some(false)
      override val isBringingDutyFree: Option[Boolean] = Some(false)
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel()

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService().url)
    }

    "return user to no-need-to-use-this-service when euCountryCheck=both and isVatResClaimed=false and isBringingDutyFree=true and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = Some("both")
      override val isVatResClaimed: Option[Boolean] = Some(false)
      override val isBringingDutyFree: Option[Boolean] = Some(true)
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel()

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService().url)
    }

    "return user to goods-bought-outside-eu when euCountryCheck=nonEuOnly and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = Some("nonEuOnly")
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel()

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService().url)
    }

    "return user to no-need-to-use-this-service when euCountryCheck=euOnly and isVatResClaimed=false and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = Some("euOnly")
      override val isVatResClaimed: Option[Boolean] = Some(false)
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel()

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService().url)
    }
  }

  "Going back from confirm-age" should {

    "return user to private-travel" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = None
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = TravelDetailsController.confirmAge()

      m.backLink(context) shouldBe Some(TravelDetailsController.privateTravel().url)
    }
  }

  "Going back from tell-us" should {

    "return user to confirm-age" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = None
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.DashboardController.showDashboard()

      m.backLink(context) shouldBe Some(TravelDetailsController.confirmAge().url)
    }
  }

  "Going back from ireland-to-northern-ireland" should {

    "return user to /tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = None
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.CalculateDeclareController.irishBorder()

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard().url)
    }
  }

  "Going back from calculation" should {

    "return user to /ireland-to-northern-ireland" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = true
      override val euCountryCheck: Option[String] = None
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.CalculateDeclareController.showCalculation()

      m.backLink(context) shouldBe Some(routes.CalculateDeclareController.irishBorder().url)
    }

    "return user to /tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = None
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.CalculateDeclareController.showCalculation()

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard().url)
    }
  }
  "Going back to where-goods-bought" should {
    "happen when on arriving-ni" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck: Option[String] = None
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = ArrivingNIController.loadArrivingNIPage()

      m.backLink(context) shouldBe Some(routes.TravelDetailsController.whereGoodsBought().url)
    }
  }

  "Going back from gb-ni-no-need-to-use-service" should {

    "return user to gb-ni-uk-resident-check" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck: Option[String] = None
      override val isVatResClaimed: Option[Boolean] = None
      override val isBringingDutyFree: Option[Boolean] = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = TravelDetailsController.noNeedToUseServiceGbni()

      m.backLink(context) shouldBe Some(UKResidentController.loadUKResidentPage().url)
    }
  }
}
