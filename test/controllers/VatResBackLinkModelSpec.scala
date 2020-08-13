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

class VatResBackLinkModelSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AppConfig].toInstance(MockitoSugar.mock[AppConfig]))
    .build()

  trait LocalSetup {

    val m = injected[BackLinkModel]

    def isIrishBorderQuestionEnabled: Boolean
    def euCountryCheck: Option[String]
    def arrivingNICheck: Option[Boolean] = None
    def isVatResClaimed: Option[Boolean]
    def isBringingDutyFree: Option[Boolean]
    def bringingOverAllowance: Option[Boolean]


    def call: Call

    lazy val journeyData = (euCountryCheck, arrivingNICheck, isVatResClaimed, isBringingDutyFree) match {
      case (None,None, None, None) => None
      case (euCountryCheck, arrivingNICheck, isVatResClaimed, isBringingDutyFree) =>
        Some(JourneyData(euCountryCheck = euCountryCheck,arrivingNICheck = arrivingNICheck, isVatResClaimed = isVatResClaimed, isBringingDutyFree = isBringingDutyFree, bringingOverAllowance = bringingOverAllowance))
    }

    lazy val context = {

      when(injected[AppConfig].isVatResJourneyEnabled) thenReturn true
      when(injected[AppConfig].isIrishBorderQuestionEnabled) thenReturn isIrishBorderQuestionEnabled

      new LocalContext(FakeRequest(call), "FAKESESSIONIN", journeyData)
    }
  }

  override def beforeEach = {
    reset(injected[AppConfig])
  }

  import routes._

  "Going back to arrvingNi" should {

    "happen when on did-you-claim-tax-back" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.didYouClaimTaxBack

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

    "happen when on goods-bought-outside-eu" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = None
      override val arrivingNICheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.goodsBoughtOutsideEu

      m.backLink(context) shouldBe Some(routes.ArrivingNIController.loadArrivingNIPage.url)
    }

  }

  "Going back to did-you-claim-tax-back" should {

    "happen when on duty-free" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.dutyFree

      m.backLink(context) shouldBe Some(TravelDetailsController.didYouClaimTaxBack.url)
    }

  }

  "Going back to duty-free" should {

    "happen when on goods-bought-inside-eu" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.goodsBoughtInsideEu

      m.backLink(context) shouldBe Some(TravelDetailsController.dutyFree.url)
    }

    "happen when on duty-free-eu" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.bringingDutyFreeQuestionEu()

      m.backLink(context) shouldBe Some(TravelDetailsController.dutyFree.url)
    }

    "happen when on goods-bought-inside-and-outside-eu" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.goodsBoughtInsideAndOutsideEu

      m.backLink(context) shouldBe Some(TravelDetailsController.dutyFree.url)
    }

    "happen when on duty-free-mix" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.bringingDutyFreeQuestionMix()

      m.backLink(context) shouldBe Some(TravelDetailsController.dutyFree.url)
    }
  }

  "Going back from private-travel" should {

    "return user to goods-bought-inside-and-outside-eu when euCountryCheck=both and isVatResClaimed=false and isBringingDutyFree=false and bringingOverAllowance = true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("both")
      override val isVatResClaimed = Some(false)
      override val isBringingDutyFree = Some(false)
      override val bringingOverAllowance = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtInsideAndOutsideEu.url)
    }

    "return user to no-need-to-use-this-service when euCountryCheck=both and isVatResClaimed=false and isBringingDutyFree=false and bringingOverAllowance = false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("both")
      override val isVatResClaimed = Some(false)
      override val isBringingDutyFree = Some(false)
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to duty-free-mix when euCountryCheck=both and isVatResClaimed=false and isBringingDutyFree=true and bringingOverAllowance=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("both")
      override val isVatResClaimed = Some(false)
      override val isBringingDutyFree = Some(true)
      override val bringingOverAllowance = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.bringingDutyFreeQuestionMix().url)
    }

    "return user to no-need-to-use-this-service when euCountryCheck=both and isVatResClaimed=false and isBringingDutyFree=true and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("both")
      override val isVatResClaimed = Some(false)
      override val isBringingDutyFree = Some(true)
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to did-you-claim-tax-back when euCountryCheck=both and isVatResClaimed=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("both")
      override val isVatResClaimed = Some(true)
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.didYouClaimTaxBack.url)
    }

    "return user to goods-bought-outside-eu when euCountryCheck=nonEuOnly and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("nonEuOnly")
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to goods-bought-outside-eu when euCountryCheck=nonEuOnly and bringingOverAllowance=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("nonEuOnly")
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtOutsideEu.url)
    }

    "return user to did-you-claim-tax-back when euCountryCheck=euOnly and isVatResClaimed=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("euOnly")
      override val isVatResClaimed = Some(true)
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.didYouClaimTaxBack.url)
    }

    "return user to duty-free-eu when euCountryCheck=euOnly and isVatResClaimed=false and bringingOverAllowance=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("euOnly")
      override val isVatResClaimed = Some(false)
      override val isBringingDutyFree = None
      override val bringingOverAllowance = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.bringingDutyFreeQuestionEu().url)
    }

    "return user to no-need-to-use-this-service when euCountryCheck=euOnly and isVatResClaimed=false and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck  = Some("euOnly")
      override val isVatResClaimed = Some(false)
      override val isBringingDutyFree = None
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }
  }

  "Going back from no-need-to-use-service" should {

    "return user to goods-bought-inside-and-outside-eu when euCountryCheck=both and isBringingDutyFree=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck = Some("both")
      override val isVatResClaimed = None
      override val isBringingDutyFree = Some(false)
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtInsideAndOutsideEu.url)
    }

    "return user to duty-free-mix when euCountryCheck=both and isBringingDutyFree=true" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck = Some("both")
      override val isVatResClaimed = None
      override val isBringingDutyFree = Some(true)
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.bringingDutyFreeQuestionMix.url)
    }

    "return user to duty-free-eu when euCountryCheck=euOnly" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck = Some("euOnly")
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.bringingDutyFreeQuestionEu.url)
    }

    "return user to goods-bought-outside-eu when euCountryCheck=nonEuOnly" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck = Some("nonEuOnly")
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtOutsideEu.url)
    }
  }

  "Going back from confirm-age" should {

    "return user to private-travel" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
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

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.DashboardController.showDashboard

      m.backLink(context) shouldBe Some(TravelDetailsController.confirmAge.url)
    }
  }

  "Going back from ireland-to-northern-ireland" should {

    "return user to /tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.CalculateDeclareController.irishBorder

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }

  "Going back from calculation" should {

    "return user to /ireland-to-northern-ireland" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = true
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.CalculateDeclareController.showCalculation

      m.backLink(context) shouldBe Some(routes.CalculateDeclareController.irishBorder.url)
    }

    "return user to /tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled = false
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.CalculateDeclareController.showCalculation

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }
  "Going back to where-goods-bought" should {
    "happen when on arriving-ni" in new LocalSetup {

      override val isIrishBorderQuestionEnabled  = false
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val isBringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = ArrivingNIController.loadArrivingNIPage

      m.backLink(context) shouldBe Some(routes.TravelDetailsController.whereGoodsBought.url)
    }
  }
}
