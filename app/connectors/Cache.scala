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

package connectors

import javax.inject.{Inject, Singleton}
import models.JourneyData
import reactivemongo.api.commands.UpdateWriteResult
import repositories.BCPassengersSessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class Cache @Inject()(
                       val sessionRepository: BCPassengersSessionRepository,
                       implicit val ec: ExecutionContext
) {

  def store(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[JourneyData] = sessionRepository.store("journeyData", journeyData)

  def storeJourneyData(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] =
    sessionRepository.store("journeyData", journeyData).flatMap(_ => fetch)

  def fetch(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = sessionRepository.fetch[JourneyData]("journeyData")

  def updateUpdatedAtTimestamp(implicit hc: HeaderCarrier) : Future[UpdateWriteResult] = sessionRepository.updateUpdatedAtTimestamp
}
