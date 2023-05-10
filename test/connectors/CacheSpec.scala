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

package connectors

import models.JourneyData
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.{JsObject, Json, Writes}
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import util.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class CacheSpec extends BaseSpec with MockitoSugar {

  private val mockBCPassengersSessionRepository: BCPassengersSessionRepository = mock[BCPassengersSessionRepository]

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val cache: Cache = new Cache(mockBCPassengersSessionRepository, ec)

  private val journeyData: JourneyData = JourneyData()

  "Cache" when {
    ".store" should {
      "return journeyData" in {
        when(
          mockBCPassengersSessionRepository.store(
            any[String],
            any[JourneyData]
          )(
            any[Writes[JourneyData]],
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(journeyData))

        val result: Future[JourneyData] = cache.store(journeyData)

        result.map(_ shouldBe journeyData)
      }
    }

    ".storeJourneyData" should {
      "return journeyData" in {
        when(
          mockBCPassengersSessionRepository.store(
            any[String],
            any[JourneyData]
          )(
            any[Writes[JourneyData]],
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(journeyData.copy(arrivingNICheck = Some(true))))

        val result: Future[Option[JourneyData]] = cache.storeJourneyData(journeyData.copy(arrivingNICheck = Some(true)))

        result.map(_ shouldBe Some(journeyData.copy(arrivingNICheck = Some(true))))
      }
    }

    ".fetch" should {
      "return None" in {
        when(mockBCPassengersSessionRepository.fetch(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val result: Future[Option[JourneyData]] = cache.fetch

        result.map(_ shouldBe None)
      }

      "return journeyData" in {
        when(mockBCPassengersSessionRepository.fetch(any[String])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                Json.obj(
                  "_id"         -> "sessionId",
                  "journeyData" -> JourneyData(euCountryCheck = Some("euOnly"))
                )
              )
            )
          )

        val result: Future[Option[JourneyData]] = cache.fetch

        result.map(_ shouldBe Some(journeyData.copy(euCountryCheck = Some("euOnly"))))
      }
    }

    "updateUpdatedAtTimestamp" should {
      "return an empty object" in {
        when(mockBCPassengersSessionRepository.updateUpdatedAtTimestamp)
          .thenReturn(Future.successful(JsObject.empty))

        val result: Future[JsObject] = cache.updateUpdatedAtTimestamp

        result.map(_ shouldBe JsObject.empty)
      }
    }
  }
}
