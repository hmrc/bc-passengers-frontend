/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.enforce

import config.AppConfig
import controllers.LocalContext
import controllers.enforce.vatres._
import models.JourneyData
import org.scalatest.exceptions.TestFailedException
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.api.test._
import util.BaseSpec
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.immutable.ListMap
import scala.concurrent.Future

class VatResJourneyEnforcerSpec extends BaseSpec {

  lazy val enforcer: JourneyEnforcer = injected[JourneyEnforcer]
  lazy val mockAppConfig: AppConfig = MockitoSugar.mock[AppConfig]

  trait GridSetup {

    def journeyStep: JourneyStep
    def params: ListMap[String, List[Any]]

    def res(implicit jd: JourneyData): Future[Result] = {
      lazy val context = LocalContext(FakeRequest("GET", "/"), "fake-session-id", Some(jd))
      enforcer.apply(journeyStep)(Future.successful(Ok("Ok")))(context)
    }

    def forEachInGrid[T](params: ListMap[String, List[Any]])(callback: List[Any] => T): Unit = {

      def it(acc: ListMap[String, Any], p: ListMap[String, List[Any]]): Unit = {

        if(p.isEmpty) {
          try {
            callback(acc.toList.map(_._2))
          }
          catch {
            case t: TestFailedException =>
              throw t.modifyMessage(_.map(_ + " - when using params: " + acc.map( t => s"${t._1} = ${t._2}" ))) //Show failing params
          }
        }
        else {
          val (paramName, paramVals) = p.head

          for (paramVal <- paramVals) {
            it(acc + (paramName -> paramVal), p.tail)
          }
        }
      }

      it(ListMap.empty, params)
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for PreviousDeclarationStep (Q0) when amendments feature is on" should {

    when(mockAppConfig.isAmendmentsEnabled) thenReturn true

    "pass with no journey data set with" in new GridSetup {

      override lazy val journeyStep: JourneyStep = PreviousDeclarationStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData()
      status(res) shouldBe OK
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DeclarationRetrievalStep when amendments feature is on" should {

    when(mockAppConfig.isAmendmentsEnabled) thenReturn true

    "not pass with no journey data set" in new GridSetup {

      override lazy val journeyStep: JourneyStep = DeclarationRetrievalStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData()
      status(res) shouldBe SEE_OTHER
    }

    "not pass when previous declaration question answered false" in new GridSetup {

      override lazy val journeyStep: JourneyStep = DeclarationRetrievalStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData(prevDeclaration = Some(false))
      status(res) shouldBe SEE_OTHER
    }

    "pass when previous declaration question answered true" in new GridSetup {

      override lazy val journeyStep: JourneyStep = DeclarationRetrievalStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData(prevDeclaration = Some(true))
      status(res) shouldBe OK
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for WhereGoodsBoughtAmendmentStep (Q1) when amendments feature is on" should {

    when(mockAppConfig.isAmendmentsEnabled) thenReturn true
    when(mockAppConfig.isVatResJourneyEnabled) thenReturn true

    "pass if prevDeclaration  = true" in new GridSetup {

      override lazy val journeyStep: JourneyStep = WhereGoodsBoughtAmendmentStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData(prevDeclaration = Some(true))
      status(res) shouldBe OK
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for WhereGoodsBoughtStep (Q1) when amendments feature is off" should {

    when(mockAppConfig.isVatResJourneyEnabled) thenReturn true
    when(mockAppConfig.isAmendmentsEnabled) thenReturn false

    "pass if prevDeclaration  = false" in new GridSetup {

      override lazy val journeyStep: JourneyStep = WhereGoodsBoughtStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData(prevDeclaration = Some(false))
      status(res) shouldBe OK
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DidYouClaimTaxBackEuOnlyStep (Q2)" should {

    "pass if euCountryCheck == euOnly" in new GridSetup {

      override lazy val journeyStep: JourneyStep = DidYouClaimTaxBackEuOnlyStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "arrivingNICheck" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(prevDeclaration: Option[Boolean],euCountryCheck: Option[String],arrivingNICheck: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration,euCountryCheck,arrivingNICheck)

          if (jd == JourneyData(Some(false),  Some("euOnly"),Some(true)) || jd == JourneyData(Some(false), Some("euOnly"), Some(false)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for GoodsBoughtIntoNIStep (Q3)" should {

    "pass if euCountryCheck == nonEuOnly or greatBritain" in new GridSetup {

      override lazy val journeyStep: JourneyStep = GoodsBoughtIntoNIStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("greatBritain"), None),
        "arrivingNICheck" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(prevDeclaration: Option[Boolean],euCountryCheck: Option[String],arrivingNICheck: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration, euCountryCheck,arrivingNICheck)

          if (jd == JourneyData(Some(false), Some("nonEuOnly"),Some(true)) || jd == JourneyData(Some(false), Some("greatBritain"), Some(true)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DidYouClaimTaxBackBothStep (Q4)" should {

    "pass if euCountryCheck == both" in new GridSetup {

      override lazy val journeyStep: JourneyStep = DidYouClaimTaxBackBothStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "arrivingNICheck" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(prevDeclaration: Option[Boolean],euCountryCheck: Option[String],arrivingNICheck: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration,euCountryCheck,arrivingNICheck)

          if (jd == JourneyData(Some(false), Some("both"), Some(true)) || jd == JourneyData(Some(false), Some("both"), Some(false)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for UKVatPaidStep" should {

    "pass if user is coming from greatBritain and arrivingNICheck == true" in new GridSetup {

      override lazy val journeyStep: JourneyStep = UKVatPaidStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("greatBritain"), None),
        "arrivingNICheck" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(prevDeclaration: Option[Boolean], euCountryCheck: Option[String], arrivingNICheck: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration, euCountryCheck, arrivingNICheck)

          if (jd == JourneyData(Some(false), Some("greatBritain"), Some(true)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for GoodsBoughtInsideEuStep (Page1)" should {

    "pass if user is coming from euOnly and arrivingNICheck == true" in new GridSetup {

      override lazy val journeyStep: JourneyStep = GoodsBoughtInsideEuStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "arrivingNICheck" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(prevDeclaration: Option[Boolean], euCountryCheck: Option[String], arrivingNICheck: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration, euCountryCheck, arrivingNICheck)

          if (jd == JourneyData(Some(false), Some("euOnly"), Some(true)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for NoNeedToUseStep (Page2,Page3)" should {

    "pass if user is coming from a valid previous step and bringingOverAllowance == false" in new GridSetup {

      override lazy val journeyStep: JourneyStep = NoNeedToUseStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(prevDeclaration: Option[Boolean], euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean], bringingOverAllowance: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration, euCountryCheck, isVatResClaimed, isBringingDutyFree, bringingOverAllowance)

          jd match {
            case JourneyData(Some(false), Some("nonEuOnly"), _, _, _,_, _, _,_, Some(false), _, _, _, _, _, _, _, _, _, _, _,_, _, _,_,_, _,_)
               | JourneyData(Some(false), Some("euOnly"), _, _,_, _,_,Some(false), Some(true), Some(false), _, _, _, _, _, _, _, _, _, _, _,_, _,_, _,_, _,_) =>
              status(res) shouldBe OK
            case _ =>
              status(res) shouldBe SEE_OTHER
          }
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for PrivateCraftStep (Q10)" should {

    "pass if user is coming from a valid previous step" in new GridSetup {

      override lazy val journeyStep: JourneyStep = PrivateCraftStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "arrivingNICheck" -> List(Some(true), Some(false), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {
        case List(prevDeclaration: Option[Boolean], euCountryCheck: Option[String], arrivingNICheck: Option[Boolean], bringingOverAllowance: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration, euCountryCheck, arrivingNICheck, bringingOverAllowance)

          jd match {
            case JourneyData(Some(false), Some("euOnly"), Some(false), _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _, _, _, _,_, _, _,_,_, _,_) //Q2
                 | JourneyData(Some(false), Some("nonEuOnly"), Some(_), _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _, _, _, _,_, _,_, _,_, _,_) //Q3, NoNeed
            =>
              status(res) shouldBe OK
            case _ =>
              status(res) shouldBe SEE_OTHER
          }
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for Is17OrOverStep (Q11)" should {

    "pass if user is coming from PrivateCraftStep and they have answered the private craft question" in new GridSetup {

      override lazy val journeyStep: JourneyStep = Is17OrOverStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None),
        "privateCraft" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {
        case List(prevDeclaration: Option[Boolean], euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean], bringingOverAllowance: Option[Boolean], privateCraft: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration, euCountryCheck, isVatResClaimed, isBringingDutyFree, bringingOverAllowance, privateCraft)

          jd match {
            case
              JourneyData(Some(false), Some("euOnly"), _, _, _, _, _, Some(false), Some(true), Some(false), Some(_), _, _, _, _, _, _, _, _, _, _,_, _,_, _,_, _,_) //Q6, NoNeed
              | JourneyData(Some(false), Some("nonEuOnly"), _, _, _, _, _, _, _, Some(false), Some(_), _, _, _, _, _, _, _, _, _, _,_, _,_, _,_, _,_) //Q3, NoNeed
              | JourneyData(Some(false), Some("nonEuOnly"), _, _, _, _, _, _, _, Some(true), Some(_), _, _, _, _, _, _, _, _, _, _,_, _,_, _,_, _,_) //Q3
              | JourneyData(Some(false), Some("euOnly"), _, _, _, _, _, Some(true), _, _, Some(_), _, _, _, _, _, _, _, _, _, _,_, _, _,_,_, _,_) //Q2
              | JourneyData(Some(false), Some("euOnly"), _, _, _, _, _, Some(false), Some(true), Some(true), Some(_), _, _, _, _, _, _, _, _, _, _,_, _, _,_,_, _,_) //Q6
            =>
              status(res) shouldBe OK
            case _ =>
              status(res) shouldBe SEE_OTHER
          }
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DashboardStep" should {

    "pass if user is coming from Is17OrOverStep and they have answered the 17 or over question" in new GridSetup {

      override lazy val journeyStep: JourneyStep = DashboardStep

      override lazy val params: ListMap[String, List[Any]] = ListMap(
        "prevDeclaration" -> List(Some(false)),
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None),
        "privateCraft" -> List(Some(true), Some(false), None),
        "ageOver17" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {
        case List(prevDeclaration: Option[Boolean], euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean], bringingOverAllowance: Option[Boolean], privateCraft: Option[Boolean], ageOver17: Option[Boolean]) =>

          implicit val jd: JourneyData = JourneyData(prevDeclaration, euCountryCheck, isVatResClaimed, isBringingDutyFree, bringingOverAllowance, privateCraft, ageOver17)

          jd match {
            case JourneyData(Some(false), Some("euOnly"), _, _, _, _, _, Some(false), Some(true), Some(false), Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, _,_, _,_, _,_) //Q6, NoNeed
                 | JourneyData(Some(false), Some("nonEuOnly"), _, _, _, _, _, _, _, Some(false), Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, _, _,_,_, _,_) //Q3, NoNeed
                 | JourneyData(Some(false), Some("nonEuOnly"), _, _, _, _, _, _, _, Some(true), Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, _, _,_,_, _,_) //Q3
                 | JourneyData(Some(false), Some("euOnly"), _, _, _, _, _, Some(true), _, _, Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, _,_, _,_, _,_) //Q2
                 | JourneyData(Some(false), Some("euOnly"), _, _, _, _, _, Some(false), Some(true), Some(true), Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, _,_, _,_, _,_) //Q6
            =>
              status(res) shouldBe OK
            case _ =>
              status(res) shouldBe SEE_OTHER
          }
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for noFurtherAmendmentStep when amendments feature is on" should {

    when(mockAppConfig.isAmendmentsEnabled) thenReturn true

    "not pass with no journey data set" in new GridSetup {

      override lazy val journeyStep: JourneyStep = noFurtherAmendmentStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData()
      status(res) shouldBe SEE_OTHER
    }

    "not pass when previous declaration question answered false" in new GridSetup {

      override lazy val journeyStep: JourneyStep = noFurtherAmendmentStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData(prevDeclaration = Some(false))
      status(res) shouldBe SEE_OTHER
    }

    "pass when previous declaration question answered true" in new GridSetup {

      override lazy val journeyStep: JourneyStep = noFurtherAmendmentStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData(prevDeclaration = Some(true))
      status(res) shouldBe OK
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for pendingPaymentStep when amendments feature is on" should {

    when(mockAppConfig.isAmendmentsEnabled) thenReturn true

    "not pass with no journey data set" in new GridSetup {

      override lazy val journeyStep: JourneyStep = pendingPaymentStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData()
      status(res) shouldBe SEE_OTHER
    }

    "not pass when previous declaration question answered false" in new GridSetup {

      override lazy val journeyStep: JourneyStep = pendingPaymentStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData(prevDeclaration = Some(false))
      status(res) shouldBe SEE_OTHER
    }

    "pass when previous declaration question answered true" in new GridSetup {

      override lazy val journeyStep: JourneyStep = pendingPaymentStep

      override lazy val params: ListMap[String, List[Any]] = ListMap[String, List[Any]]()

      implicit val jd: JourneyData = JourneyData(prevDeclaration = Some(true))
      status(res) shouldBe OK
    }
  }

}
