package repositories

import models.JourneyData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsObject, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global



class BCPassengersSessionRepositoryISpec extends AnyWordSpecLike with Matchers
  with GuiceOneServerPerSuite with FutureAwaits with DefaultAwaitTimeout with DefaultPlayMongoRepositorySupport[JsObject] {
  val repository: BCPassengersSessionRepository = new BCPassengersSessionRepository(mongoComponent)
  class LocalSetup {
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("fakesessionid")))

    await(repository.collection.drop().toFuture())

  }

  "fetch" should {
    "return None if no data exists" in new LocalSetup {
      repository.fetch[JourneyData]("journeyData").futureValue shouldBe None
    }
    "return Some Journey Data if data exists" in new LocalSetup {
      await(repository.collection.insertOne(Json.obj("_id" -> "fakesessionid", "journeyData" -> JourneyData(euCountryCheck = Some("Yes")))).toFuture())
      await(repository.fetch[JourneyData]("journeyData")) shouldBe Some(Json.obj("_id" -> "fakesessionid", "journeyData" -> JourneyData(euCountryCheck = Some("Yes"))))
    }

    "return Error if no session id exists" in new LocalSetup {
      override implicit val hc: HeaderCarrier = HeaderCarrier()
      intercept[Exception](await(repository.fetch[JourneyData]("journeyData"))).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }
  }

  "store" should {
    "insert new record if no data exists" in new LocalSetup {
      await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))

      val journeyData: Option[JourneyData] = await(repository.fetch[JourneyData]("journeyData").map {
        case Some(jobs) => (jobs \ "journeyData").asOpt[JourneyData]
        case _ => Option.empty
      })

      journeyData.get.arrivingNICheck shouldBe Some(true)

    }

    "update new record if data already exists" in new LocalSetup {
      await(repository.collection.insertOne(Json.obj("_id" -> "fakesessionid", "journeyData" -> JourneyData(euCountryCheck = Some("Yes")))).toFuture())
      await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(false), euCountryCheck = Some("Yes"))))

     val journeyData: Option[JourneyData] = await(repository.fetch[JourneyData]("journeyData").map {
       case Some(jobs) => (jobs \ "journeyData").asOpt[JourneyData]
       case _ => Option.empty
     })

      journeyData.get.arrivingNICheck shouldBe Some(false)
      journeyData.get.euCountryCheck shouldBe Some("Yes")
    }

    "return Error if no session id exists" in new LocalSetup {
      override implicit val hc: HeaderCarrier = HeaderCarrier()
      intercept[Exception](await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }

  }
}
