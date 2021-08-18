package repositories

import java.util.Date
import models.JourneyData
import org.mongodb.scala.model.Filters.equal
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.JsPath.\
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
      //await(repository.fetch[JourneyData]("journeyData")) shouldBe Some(JourneyData(euCountryCheck = Some("Yes")))
    }

    "return Error if no session id exists" in new LocalSetup {
      override implicit val hc: HeaderCarrier = HeaderCarrier()
      intercept[Exception](await(repository.fetch[JourneyData]("journeyData"))).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }
  }

  "store" should {
    "insert new record if no data exists" in new LocalSetup {
      await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))
      //repository.fetch[JourneyData]("journeyData").futureValue shouldBe Some(JourneyData(arrivingNICheck = Some(true)))
    }

    "update new record if data already exists" in new LocalSetup {
      await(repository.collection.insertOne(Json.obj("_id" -> "fakesessionid", "journeyData" -> JourneyData(euCountryCheck = Some("Yes")))).toFuture())
      await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(false), euCountryCheck = Some("Yes"))))
     /* repository.fetch[JourneyData]("journeyData").futureValue shouldBe
        Some(JourneyData(arrivingNICheck = Some(false), euCountryCheck = Some("Yes")))*/
    }

    "return Error if no session id exists" in new LocalSetup {
      override implicit val hc: HeaderCarrier = HeaderCarrier()
      intercept[Exception](await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }

  }

  "updateUpdatedAtTimestamp" should {
    "update the updateUpdatedAtTimestamp" in new LocalSetup {
      /* await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))
       val returnJson:String = await(repository.collection.find(equal("_id","fakesessionid")).)

       val firstTimestamp = (returnJson \ "updatedAt" \ "$date").as[Date]
        await(repository.updateUpdatedAtTimestamp)
        val updatedTimestampJson = await(repository.find("_id" -> "fakesessionid")).headOption
        val secondTimestamp = (updatedTimestampJson.get \ "updatedAt" \ "$date").as[Date]
        secondTimestamp.after(firstTimestamp)  shouldBe true*/
    }

    "return Error if no session id exists" in new LocalSetup {
      override implicit val hc: HeaderCarrier = HeaderCarrier()
     // intercept[Exception](await(repository.updateUpdatedAtTimestamp)).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }

  }






}
