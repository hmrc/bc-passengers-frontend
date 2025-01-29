/*
 * Copyright 2025 HM Revenue & Customs
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

package repositories

import models.JourneyData
import models.JourneyData.format
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsLookupResult, JsObject}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.ZoneOffset.UTC
import java.time.{Instant, LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global

class BCPassengersSessionRepositoryISpec
    extends AnyWordSpecLike
    with Matchers
    with GuiceOneServerPerSuite
    with FutureAwaits
    with DefaultAwaitTimeout
    with OptionValues
    with DefaultPlayMongoRepositorySupport[JsObject] {

  val repository: BCPassengersSessionRepository = new BCPassengersSessionRepository(mongoComponent)
  implicit val hc: HeaderCarrier                = HeaderCarrier(sessionId = Some(SessionId("fakesessionid")))

  "fetch" should {
    "return None if no data exists" in {
      repository.get().futureValue shouldBe None
    }
    "return Some Journey Data if data exists" in {
      await(repository.store[JourneyData]("journeyData", JourneyData(euCountryCheck = Some("Yes"))))

      val data = await(repository.get())

      (data.value \ "_id").toString                     should include("fakesessionid")
      Some(JourneyData(euCountryCheck = Some("Yes"))) shouldBe (data.value \ "journeyData").asOpt[JourneyData]
    }

    "return Error if no session id exists" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      intercept[Exception](
        await(repository.get())
      ).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }
  }

  "store" should {
    "insert new record if no data exists" in {
      await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))

      val journeyData: Option[JourneyData] = await(repository.get().map {
        case Some(jobs) => (jobs \ "journeyData").asOpt[JourneyData]
        case _          => Option.empty
      })

      journeyData.get.arrivingNICheck shouldBe Some(true)

    }

    "update new record if data already exists" in {
      await(
        repository
          .store[JourneyData]("journeyData", JourneyData(euCountryCheck = Some("Yes")))
      )

      await(
        repository
          .store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(false), euCountryCheck = Some("Yes")))
      )

      val journeyData: Option[JourneyData] = await(repository.get().map {
        case Some(jobs) => (jobs \ "journeyData").asOpt[JourneyData]
        case _          => Option.empty
      })

      journeyData.get.arrivingNICheck shouldBe Some(false)
      journeyData.get.euCountryCheck  shouldBe Some("Yes")
    }

    "return Error if no session id exists" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      intercept[Exception](
        await(repository.store[JourneyData]("journeyData", JourneyData(arrivingNICheck = Some(true))))
      ).getMessage shouldBe "Could not find sessionId in HeaderCarrier"
    }
  }

  "updateUpdatedAtTimestamp" should {
    "update the numberLong field within updatedAt object with current timestamp" in {
      val result: JsObject = await(repository.updateUpdatedAtTimestamp)

      val lookupResult: JsLookupResult = result \ "updatedAt" \ s"$$date" \ s"$$numberLong"
      val instant: Instant             = Instant.ofEpochMilli(lookupResult.as[String].toLong)
      val date: LocalDate              = LocalDateTime.ofInstant(instant, UTC).toLocalDate

      date shouldBe LocalDate.now(UTC)
    }

    "throw Exception if no session id exists" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      intercept[Exception](
        await(repository.updateUpdatedAtTimestamp)
      ).getMessage shouldBe "[BCPassengersSessionRepository][updateUpdatedAtTimestamp]Could not find sessionId in HeaderCarrier"
    }
  }
}
