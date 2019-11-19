package controllers.enforce

import controllers.LocalContext
import controllers.enforce.vatres._
import models.JourneyData
import org.scalatest.exceptions.TestFailedException
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.api.test._
import util.BaseSpec

import scala.collection.immutable.ListMap
import scala.concurrent.Future

class VatResJourneyEnforcerSpec extends BaseSpec {

  lazy val enforcer = injected[JourneyEnforcer]

  trait GridSetup {

    def journeyStep: JourneyStep
    def params: ListMap[String, List[Any]]

    def res(implicit jd: JourneyData) = {
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

  "Calling VatResJourneyEnforcer.enforcePrereqs for WhereGoodsBoughtStep (Q1)" should {

    "pass with no journey data set" in new GridSetup {

      override lazy val journeyStep = WhereGoodsBoughtStep

      override lazy val params = ListMap[String, List[Any]]()

      implicit val jd = JourneyData()
      status(res) shouldBe OK
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DidYouClaimTaxBackEuOnlyStep (Q2)" should {

    "pass if euCountryCheck == euOnly" in new GridSetup {

      override lazy val journeyStep = DidYouClaimTaxBackEuOnlyStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String]) =>

          implicit val jd = JourneyData(euCountryCheck)

          if (jd == JourneyData(Some("euOnly")))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for GoodsBoughtOutsideEuStep (Q3)" should {

    "pass if euCountryCheck == nonEuOnly" in new GridSetup {

      override lazy val journeyStep = GoodsBoughtOutsideEuStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String]) =>

          implicit val jd = JourneyData(euCountryCheck)

          if (jd == JourneyData(Some("nonEuOnly")))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DidYouClaimTaxBackBothStep (Q4)" should {

    "pass if euCountryCheck == both" in new GridSetup {

      override lazy val journeyStep = DidYouClaimTaxBackBothStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String]) =>

          implicit val jd = JourneyData(euCountryCheck)

          if (jd == JourneyData(Some("both")))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for BringingDutyFreeEuStep (Q5)" should {

    "pass if user is coming from DidYouClaimTaxBackEuOnlyStep and isVatResClaimed == false" in new GridSetup {

      override lazy val journeyStep = BringingDutyFreeEuStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean]) =>

          implicit val jd = JourneyData(euCountryCheck, isVatResClaimed)

          if (jd == JourneyData(Some("euOnly"), Some(false)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for GoodsBoughtInsideEuStep (Page1)" should {

    "pass if user is coming from BringingDutyFreeEuStep and isBringingDutyFree == false" in new GridSetup {

      override lazy val journeyStep = GoodsBoughtInsideEuStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean]) =>

          implicit val jd = JourneyData(euCountryCheck, isVatResClaimed, isBringingDutyFree)

          if (jd == JourneyData(Some("euOnly"), Some(false), Some(false)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DeclareDutyFreeEuStep (Q6)" should {

    "pass if user is coming from BringingDutyFreeEuStep and isBringingDutyFree == true" in new GridSetup {

      override lazy val journeyStep = DeclareDutyFreeEuStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean]) =>

          implicit val jd = JourneyData(euCountryCheck, isVatResClaimed, isBringingDutyFree)

          if (jd == JourneyData(Some("euOnly"), Some(false), Some(true)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for BringingDutyFreeBothStep (Q7)" should {

    "pass if user is coming from DidYouClaimTaxBackBothStep isVatResClaimed == false" in new GridSetup {

      override lazy val journeyStep = BringingDutyFreeBothStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean]) =>

          implicit val jd = JourneyData(euCountryCheck, isVatResClaimed)

          if (jd == JourneyData(Some("both"), Some(false)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for GoodsBoughtInAndOutEuStep (Q8)" should {

    "pass if user is coming from BringingDutyFreeBothStep and bringingOverAllowance == false" in new GridSetup {

      override lazy val journeyStep = GoodsBoughtInAndOutEuStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean]) =>

          implicit val jd = JourneyData(euCountryCheck, isVatResClaimed, isBringingDutyFree)

          if (jd == JourneyData(Some("both"), Some(false), Some(false)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for NoNeedToUseStep (Page2,Page3)" should {

    "pass if user is coming from a valid previous step and bringingOverAllowance == false" in new GridSetup {

      override lazy val journeyStep = NoNeedToUseStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean], bringingOverAllowance: Option[Boolean]) =>

          implicit val jd = JourneyData(euCountryCheck, isVatResClaimed, isBringingDutyFree, bringingOverAllowance)

          jd match {
            case JourneyData(Some("both"), Some(false), Some(true), Some(false), _, _, _, _, _, _, _, _, _, _)
               | JourneyData(Some("both"), Some(false), Some(false), Some(false), _, _, _, _, _, _, _, _, _, _)
               | JourneyData(Some("nonEuOnly"), _, _, Some(false), _, _, _, _, _, _, _, _, _, _)
               | JourneyData(Some("euOnly"), Some(false), Some(true), Some(false), _, _, _, _, _, _, _, _, _, _) =>
              status(res) shouldBe OK
            case _ =>
              status(res) shouldBe SEE_OTHER
          }
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DeclareDutyFreeMixStep (Q9)" should {

    "pass if user is coming from BringingDutyFreeBothStep and answered yes" in new GridSetup {

      override lazy val journeyStep = DeclareDutyFreeMixStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean]) =>

          implicit val jd = JourneyData(euCountryCheck, isVatResClaimed, isBringingDutyFree)

          if (jd == JourneyData(Some("both"), Some(false), Some(true)))
            status(res) shouldBe OK
          else
            status(res) shouldBe SEE_OTHER
      }
    }
  }



  "Calling VatResJourneyEnforcer.enforcePrereqs for PrivateCraftStep (Q10)" should {

    "pass if user is coming from a valid previous step" in new GridSetup {

      override lazy val journeyStep = PrivateCraftStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) { p =>

        p match {
          case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean], bringingOverAllowance: Option[Boolean]) =>

            implicit val jd = JourneyData(euCountryCheck, isVatResClaimed, isBringingDutyFree, bringingOverAllowance)

            jd match {
              case JourneyData(Some("both"), Some(false), Some(false), Some(false), _, _, _, _, _, _, _, _, _, _) //Q8, NoNeed
                   | JourneyData(Some("both"), Some(false), Some(true), Some(false), _, _, _, _, _, _, _, _, _, _) //Q9, NoNeed
                   | JourneyData(Some("euOnly"), Some(false), Some(true), Some(false), _, _, _, _, _, _, _, _, _, _) //Q6, NoNeed
                   | JourneyData(Some("nonEuOnly"), _, _, Some(false), _, _, _, _, _, _, _, _, _, _) //Q3, NoNeed
                   | JourneyData(Some("both"), Some(false), Some(true), Some(true), _, _, _, _, _, _, _, _, _, _)  //Q9
                   | JourneyData(Some("both"), Some(false), Some(false), Some(true), _, _, _, _, _, _, _, _, _, _) //Q8
                   | JourneyData(Some("nonEuOnly"), _, _, Some(true), _, _, _, _, _, _, _, _, _, _) //Q3
                   | JourneyData(Some("both"), Some(true), _, _, _, _, _, _, _, _, _, _, _, _) //Q4
                   | JourneyData(Some("euOnly"), Some(true), _, _, _, _, _, _, _, _, _, _, _, _) //Q2
                   | JourneyData(Some("euOnly"), Some(false), Some(true), Some(true), _, _, _, _, _, _, _, _, _, _) //Q6
              =>
                status(res) shouldBe OK
              case _ =>
                status(res) shouldBe SEE_OTHER
            }
        }
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for Is17OrOverStep (Q11)" should {

    "pass if user is coming from PrivateCraftStep and they have answered the private craft question" in new GridSetup {

      override lazy val journeyStep = Is17OrOverStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None),
        "privateCraft" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) { p =>

        p match {
          case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean], bringingOverAllowance: Option[Boolean], privateCraft: Option[Boolean]) =>

            implicit val jd = JourneyData(euCountryCheck, isVatResClaimed, isBringingDutyFree, bringingOverAllowance, privateCraft)

            jd match {
              case JourneyData(Some("both"), Some(false), Some(false), Some(false), Some(_), _, _, _, _, _, _, _, _, _) //Q8, NoNeed
                   | JourneyData(Some("both"), Some(false), Some(true), Some(false), Some(_), _, _, _, _, _, _, _, _, _) //Q9, NoNeed
                   | JourneyData(Some("euOnly"), Some(false), Some(true), Some(false), Some(_), _, _, _, _, _, _, _, _, _) //Q6, NoNeed
                   | JourneyData(Some("nonEuOnly"), _, _, Some(false), Some(_), _, _, _, _, _, _, _, _, _) //Q3, NoNeed
                   | JourneyData(Some("both"), Some(false), Some(true), Some(true), Some(_), _, _, _, _, _, _, _, _, _)  //Q9
                   | JourneyData(Some("both"), Some(false), Some(false), Some(true), Some(_), _, _, _, _, _, _, _, _, _) //Q8
                   | JourneyData(Some("nonEuOnly"), _, _, Some(true), Some(_), _, _, _, _, _, _, _, _, _) //Q3
                   | JourneyData(Some("both"), Some(true), _, _, Some(_), _, _, _, _, _, _, _, _, _) //Q4
                   | JourneyData(Some("euOnly"), Some(true), _, _, Some(_), _, _, _, _, _, _, _, _, _) //Q2
                   | JourneyData(Some("euOnly"), Some(false), Some(true), Some(true), Some(_), _, _, _, _, _, _, _, _, _) //Q6
              =>
                status(res) shouldBe OK
              case _ =>
                status(res) shouldBe SEE_OTHER
            }
        }
      }
    }
  }

  "Calling VatResJourneyEnforcer.enforcePrereqs for DashboardStep" should {

    "pass if user is coming from Is17OrOverStep and they have answered the 17 or over question" in new GridSetup {

      override lazy val journeyStep = DashboardStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "isVatResClaimed" -> List(Some(true), Some(false), None),
        "isBringingDutyFree" -> List(Some(true), Some(false), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None),
        "privateCraft" -> List(Some(true), Some(false), None),
        "ageOver17" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) { p =>

        p match {
          case List(euCountryCheck: Option[String], isVatResClaimed: Option[Boolean], isBringingDutyFree: Option[Boolean], bringingOverAllowance: Option[Boolean], privateCraft: Option[Boolean], ageOver17: Option[Boolean]) =>

            implicit val jd = JourneyData(euCountryCheck, isVatResClaimed, isBringingDutyFree, bringingOverAllowance, privateCraft, ageOver17)

            jd match {
              case JourneyData(Some("both"), Some(false), Some(false), Some(false), Some(_), Some(_), _, _, _, _, _, _, _, _) //Q8, NoNeed
                   | JourneyData(Some("both"), Some(false), Some(true), Some(false), Some(_), Some(_), _, _, _, _, _, _, _, _) //Q9, NoNeed
                   | JourneyData(Some("euOnly"), Some(false), Some(true), Some(false), Some(_), Some(_), _, _, _, _, _, _, _, _) //Q6, NoNeed
                   | JourneyData(Some("nonEuOnly"), _, _, Some(false), Some(_), Some(_), _, _, _, _, _, _, _, _) //Q3, NoNeed
                   | JourneyData(Some("both"), Some(false), Some(true), Some(true), Some(_), Some(_), _, _, _, _, _, _, _, _)  //Q9
                   | JourneyData(Some("both"), Some(false), Some(false), Some(true), Some(_), Some(_), _, _, _, _, _, _, _, _) //Q8
                   | JourneyData(Some("nonEuOnly"), _, _, Some(true), Some(_), Some(_), _, _, _, _, _, _, _, _) //Q3
                   | JourneyData(Some("both"), Some(true), _, _, Some(_), Some(_), _, _, _, _, _, _, _, _) //Q4
                   | JourneyData(Some("euOnly"), Some(true), _, _, Some(_), Some(_), _, _, _, _, _, _, _, _) //Q2
                   | JourneyData(Some("euOnly"), Some(false), Some(true), Some(true), Some(_), Some(_), _, _, _, _, _, _, _, _) //Q6
              =>
                status(res) shouldBe OK
              case _ =>
                status(res) shouldBe SEE_OTHER
            }
        }
      }
    }
  }

}
