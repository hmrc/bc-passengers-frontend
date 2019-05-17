package controllers

import models.JourneyData
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import util.BaseSpec

class StandardBackLinkModelSpec extends BaseSpec {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .configure("features.vat-res" -> false)
    .build()

  trait LocalSetup {

    val m = injected[BackLinkModel]

    def euCountryCheck: Option[String]
    def isVatResClaimed: Option[Boolean]
    def bringingDutyFree: Option[Boolean]
    def bringingOverAllowance: Option[Boolean]

    def call: Call

    lazy val journeyData = (euCountryCheck, isVatResClaimed, bringingDutyFree) match {
      case (None, None, None) => None
      case (euCountryCheck, isVatResClaimed, bringingDutyFree) =>
        Some(JourneyData(euCountryCheck = euCountryCheck, isVatResClaimed = isVatResClaimed, bringingDutyFree = bringingDutyFree, bringingOverAllowance = bringingOverAllowance))
    }

    lazy val context = new LocalContext(FakeRequest(call), "FAKESESSIONID", journeyData)
  }

  import routes._

  "Going back to where-goods-bought" should {

    "happen when on goods-bought-outside-eu" in new LocalSetup {
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.goodsBoughtOutsideEu

      m.backLink(context) shouldBe Some(routes.TravelDetailsController.whereGoodsBought.url)
    }

    "happen when on goods-bought-inside-eu" in new LocalSetup {
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.goodsBoughtInsideEu

      m.backLink(context) shouldBe Some(TravelDetailsController.whereGoodsBought.url)
    }

    "happen when on goods-bought-inside-and-outside-eu" in new LocalSetup {
      override val euCountryCheck  = None
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.goodsBoughtInsideAndOutsideEu

      m.backLink(context) shouldBe Some(TravelDetailsController.whereGoodsBought.url)
    }
  }


  "Going back from private-travel" should {

    "return user to goods-bought-inside-and-outside-eu when euCountryCheck=both and bringingOverAllowance=true" in new LocalSetup {
      override val euCountryCheck  = Some("both")
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtInsideAndOutsideEu.url)
    }


    "return user to no-need-to-use-service when euCountryCheck=both and bringingOverAllowance=false" in new LocalSetup {
      override val euCountryCheck  = Some("both")
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to goods-bought-outside-eu when euCountryCheck=nonEuOnly and bringingOverAllowance=true" in new LocalSetup {
      override val euCountryCheck  = Some("nonEuOnly")
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = Some(true)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtOutsideEu.url)
    }

    "return user to no-need-to-use-service when euCountryCheck=nonEuOnly and bringingOverAllowance=false" in new LocalSetup {
      override val euCountryCheck  = Some("nonEuOnly")
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }
  }

  "Going back from no-need-to-use-service" should {

    "return user to goods-bought-inside-and-outside-eu when euCountryCheck=both and bringingDutyFree=false" in new LocalSetup {
      override val euCountryCheck = Some("both")
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtInsideAndOutsideEu.url)
    }

    "return user to goods-bought-outside-eu when euCountryCheck=nonEuOnly" in new LocalSetup {
      override val euCountryCheck = Some("nonEuOnly")
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.noNeedToUseService

      m.backLink(context) shouldBe Some(TravelDetailsController.goodsBoughtOutsideEu.url)
    }
  }

  "Going back from confirm-age" should {

    "return user to private-travel" in new LocalSetup {
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = TravelDetailsController.confirmAge

      m.backLink(context) shouldBe Some(TravelDetailsController.privateTravel.url)
    }
  }

  "Going back from tell-us" should {

    "return user to confirm-age" in new LocalSetup {
      override val euCountryCheck = None
      override val isVatResClaimed = None
      override val bringingDutyFree = None
      override val bringingOverAllowance = None

      override def call: Call = routes.DashboardController.showDashboard

      m.backLink(context) shouldBe Some(TravelDetailsController.confirmAge.url)
    }
  }
}
