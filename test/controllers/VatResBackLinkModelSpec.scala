/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{JourneyData, ProductPath}
import org.mockito.Mockito.{reset, when}
import org.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import repositories.BCPassengersSessionRepository
import util.BaseSpec

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

    lazy val journeyData: Option[JourneyData] =
      (prevDeclaration, euCountryCheck, arrivingNICheck, isVatResClaimed, isBringingDutyFree) match {
        case (None, None, None, None, None)                                                          => None
        case (prevDeclaration, euCountryCheck, arrivingNICheck, isVatResClaimed, isBringingDutyFree) =>
          Some(
            JourneyData(
              prevDeclaration = prevDeclaration,
              euCountryCheck = euCountryCheck,
              arrivingNICheck = arrivingNICheck,
              isVatResClaimed = isVatResClaimed,
              isBringingDutyFree = isBringingDutyFree,
              bringingOverAllowance = bringingOverAllowance
            )
          )
      }

    lazy val context: LocalContext = {

      when(injected[AppConfig].isVatResJourneyEnabled) thenReturn true
      when(injected[AppConfig].isIrishBorderQuestionEnabled) thenReturn isIrishBorderQuestionEnabled

      LocalContext(FakeRequest(call), "FAKESESSIONIN", journeyData)
    }
  }

  override def beforeEach(): Unit =
    reset(injected[AppConfig])

  import routes._

  "Going back to did-you-claim-tax-back" should {

    "happen when on duty-free" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = TravelDetailsController.dutyFree

      m.backLink(context) shouldBe Some(TravelDetailsController.didYouClaimTaxBack.url)
    }

  }

  "Going back from private-travel" should {

    "return user to no-need-to-use-this-service when euCountryCheck=both and isVatResClaimed=false and isBringingDutyFree=false and bringingOverAllowance = false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("both")
      override val isVatResClaimed: Option[Boolean]       = Some(false)
      override val isBringingDutyFree: Option[Boolean]    = Some(false)
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to no-need-to-use-this-service when euCountryCheck=both and isVatResClaimed=false and isBringingDutyFree=true and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("both")
      override val isVatResClaimed: Option[Boolean]       = Some(false)
      override val isBringingDutyFree: Option[Boolean]    = Some(true)
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to goods-bought-outside-eu when euCountryCheck=nonEuOnly and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("nonEuOnly")
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
    }

    "return user to no-need-to-use-this-service when euCountryCheck=euOnly and isVatResClaimed=false and bringingOverAllowance=false" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = Some("euOnly")
      override val isVatResClaimed: Option[Boolean]       = Some(false)
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = Some(false)

      override def call: Call = TravelDetailsController.privateTravel

      m.backLink(context) shouldBe Some(TravelDetailsController.noNeedToUseService.url)
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

  "Going back from calculation" should {

    "return user to /ireland-to-northern-ireland" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = true
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.CalculateDeclareController.showCalculation

      m.backLink(context) shouldBe Some(routes.CalculateDeclareController.irishBorder.url)
    }

    "return user to /tell-us" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.CalculateDeclareController.showCalculation

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }
  "Going back to where-goods-bought" should {
    "happen when on arriving-ni" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = ArrivingNIController.loadArrivingNIPage

      m.backLink(context) shouldBe Some(routes.TravelDetailsController.whereGoodsBought.url)
    }
  }

  "Going back from select-goods/acohol page" should {

    "return user to the dashboard page" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.SelectProductController.askProductSelection(ProductPath("alcohol"))

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }

  "Going back from select-goods/tobacco page" should {

    "return user to the dashboard page" in new LocalSetup {

      override val isIrishBorderQuestionEnabled           = false
      override val euCountryCheck: Option[String]         = None
      override val isVatResClaimed: Option[Boolean]       = None
      override val isBringingDutyFree: Option[Boolean]    = None
      override val bringingOverAllowance: Option[Boolean] = None

      override def call: Call = routes.SelectProductController.askProductSelection(ProductPath("tobacco"))

      m.backLink(context) shouldBe Some(routes.DashboardController.showDashboard.url)
    }
  }

}
