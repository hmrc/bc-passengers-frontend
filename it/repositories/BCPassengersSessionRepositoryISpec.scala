package repositories

import java.util.Date

import models.JourneyData
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsObject, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global


class BCPassengersSessionRepositoryISpec extends WordSpec with Matchers
  with GuiceOneServerPerSuite with FutureAwaits with DefaultAwaitTimeout {
  val repository: BCPassengersSessionRepository = app.injector.instanceOf[BCPassengersSessionRepository]
  class LocalSetup {
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("fakesessionid")))

    await(repository.remove())

  }

  "fetch" should {
    "return None if no data exists" in new LocalSetup {
      await(repository.fetch[JourneyData]("journeyData")) shouldBe None
    }
    "return Some Journey Data if data exists" in new LocalSetup {
      await(repository.insert(Json.obj("_id" -> "fakesessionid", "journeyData" -> JourneyData(euCountryCheck = Some("Yes")))))
      await(repository.fetch[JourneyData]("journeyData")) shouldBe Some(JourneyData(euCountryCheck = Some("Yes")))
    }

    "return Error if no session id exists" in new LocalSetup {
      override implicit val hc: HeaderCarrier = HeaderCarrier()
      intercept[Exception](await(repository.fetch[JourneyData]("journeyData"))).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }
  }

  "store" should {
    "insert new record if no data exists" in new LocalSetup {
      await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))
      await(repository.fetch[JourneyData]("journeyData")) shouldBe Some(JourneyData(arrivingNICheck = Some(true)))
    }

    "update new record if data already exists" in new LocalSetup {
      await(repository.insert(Json.obj("_id" -> "fakesessionid", "journeyData" -> JourneyData(euCountryCheck = Some("Yes")))))
      await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(false), euCountryCheck = Some("Yes"))))
      await(repository.fetch[JourneyData]("journeyData")) shouldBe
        Some(JourneyData(arrivingNICheck = Some(false), euCountryCheck = Some("Yes")))
    }

    "return Error if no session id exists" in new LocalSetup {
      override implicit val hc: HeaderCarrier = HeaderCarrier()
      intercept[Exception](await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }

  }

  "updateUpdatedAtTimestamp" should {
    "update the updateUpdatedAtTimestamp" in new LocalSetup {
      await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))
      val returnJson = await(repository.find("_id" -> "fakesessionid")).headOption
      val firstTimestamp = (returnJson.get \ "updatedAt" \ "$date").as[Date]
      await(repository.updateUpdatedAtTimestamp)
      val updatedTimestampJson = await(repository.find("_id" -> "fakesessionid")).headOption
      val secondTimestamp = (updatedTimestampJson.get \ "updatedAt" \ "$date").as[Date]
      secondTimestamp.after(firstTimestamp)  shouldBe true
    }

    "return Error if no session id exists" in new LocalSetup {
      override implicit val hc: HeaderCarrier = HeaderCarrier()
      intercept[Exception](await(repository.updateUpdatedAtTimestamp)).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }

  }






}
