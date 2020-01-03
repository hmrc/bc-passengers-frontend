package controllers.enforce

import controllers.LocalContext
import controllers.enforce.nonvatres._
import models.JourneyData
import org.scalatest.exceptions.TestFailedException
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.api.test._
import util.BaseSpec

import scala.collection.immutable.ListMap
import scala.concurrent.Future

class NonVatResJourneyEnforcerSpec extends BaseSpec {

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

  "Calling NonVatResJourneyEnforcer.enforcePrereqs for WhereGoodsBoughtStep (Q1)" should {

    "pass with no journey data set" in new GridSetup {

      override lazy val journeyStep = WhereGoodsBoughtStep

      override lazy val params = ListMap[String, List[Any]]()

      implicit val jd = JourneyData()
      status(res) shouldBe OK
    }
  }

  "Calling NonVatResJourneyEnforcer.enforcePrereqs for GoodsBoughtOutsideEuStep (Q2)" should {

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

  "Calling NonVatResJourneyEnforcer.enforcePrereqs for GoodsBoughtInsideEuStep (Page1)" should {

    "pass if euCountryCheck == euOnly" in new GridSetup {

      override lazy val journeyStep = GoodsBoughtInsideEuStep

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

  "Calling NonVatResJourneyEnforcer.enforcePrereqs for GoodsBoughtInAndOutEuStep (Q3)" should {

    "pass if euCountryCheck == both" in new GridSetup {

      override lazy val journeyStep = GoodsBoughtInAndOutEuStep

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

  "Calling NonVatResJourneyEnforcer.enforcePrereqs for NoNeedToUseStep (Page2)" should {

    "pass if user is coming from a valid previous step and bringingOverAllowance == false" in new GridSetup {

      override lazy val journeyStep = NoNeedToUseStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) {

        case List(euCountryCheck: Option[String], bringingOverAllowance: Option[Boolean]) =>

          implicit val jd = JourneyData(euCountryCheck, None, None, bringingOverAllowance)

          jd match {
            case JourneyData(Some("both"), _, _, Some(false), _, _, _, _, _, _, _, _, _, _)
               | JourneyData(Some("nonEuOnly"), _, _, Some(false), _, _, _, _, _, _, _, _, _, _)
            =>
              status(res) shouldBe OK
            case _ =>
              status(res) shouldBe SEE_OTHER
          }
      }
    }
  }

  "Calling NonVatResJourneyEnforcer.enforcePrereqs for PrivateCraftStep (Q4)" should {

    "pass if user is coming from a valid previous step" in new GridSetup {

      override lazy val journeyStep = PrivateCraftStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) { p =>

        p match {
          case List(euCountryCheck: Option[String], bringingOverAllowance: Option[Boolean]) =>

            implicit val jd = JourneyData(euCountryCheck, None, None, bringingOverAllowance)

            jd match {
              case JourneyData(Some("nonEuOnly"), _, _, Some(_), _, _, _, _, _, _, _, _, _, _)
                   | JourneyData(Some("both"), _, _, Some(_), _, _, _, _, _, _, _, _, _, _)
              =>
                status(res) shouldBe OK
              case _ =>
                status(res) shouldBe SEE_OTHER
            }
        }
      }
    }
  }

  "Calling NonVatResJourneyEnforcer.enforcePrereqs for Is17OrOverStep (Q5)" should {

    "pass if user is coming from PrivateCraftStep and they have answered the private craft question" in new GridSetup {

      override lazy val journeyStep = Is17OrOverStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None),
        "privateCraft" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) { p =>

        p match {
          case List(euCountryCheck: Option[String], bringingOverAllowance: Option[Boolean], privateCraft: Option[Boolean]) =>

            implicit val jd = JourneyData(euCountryCheck, None, None, bringingOverAllowance, privateCraft)

            jd match {
              case JourneyData(Some("nonEuOnly"), _, _, Some(_), Some(_), _, _, _, _, _, _, _, _, _)
                   | JourneyData(Some("both"), _, _, Some(_), Some(_), _, _, _, _, _, _, _, _, _)
              =>
                status(res) shouldBe OK
              case _ =>
                status(res) shouldBe SEE_OTHER
            }
        }
      }
    }
  }

  "Calling NonVatResJourneyEnforcer.enforcePrereqs for DashboardStep" should {

    "pass if user is coming from Is17OrOverStep and they have answered the 17 or over question" in new GridSetup {

      override lazy val journeyStep = DashboardStep

      override lazy val params = ListMap(
        "euCountryCheck" -> List(Some("euOnly"), Some("nonEuOnly"), Some("both"), None),
        "bringingOverAllowance" -> List(Some(true), Some(false), None),
        "privateCraft" -> List(Some(true), Some(false), None),
        "ageOver17" -> List(Some(true), Some(false), None)
      )

      forEachInGrid(params) { p =>

        p match {
          case List(euCountryCheck: Option[String], bringingOverAllowance: Option[Boolean], privateCraft: Option[Boolean], ageOver17: Option[Boolean]) =>

            implicit val jd = JourneyData(euCountryCheck, None, None, bringingOverAllowance, privateCraft, ageOver17)

            jd match {
              case JourneyData(Some("nonEuOnly"), _, _, Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _)
                   | JourneyData(Some("both"), _, _, Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _)
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