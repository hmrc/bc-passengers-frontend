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

package services

import connectors.Cache
import controllers.LocalContext
import models.YourJourneyDetailsDto.toArrivalForm

import javax.inject.{Inject, Singleton}
import models.{JourneyData, PreUserInformation, YourJourneyDetailsDto}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreUserInformationService @Inject() (val cache: Cache) {

  def storePreUserInformation(journeyData: JourneyData, preUserInformation: Option[PreUserInformation])(implicit
    hc: HeaderCarrier,
    ex: ExecutionContext
  ): Future[JourneyData] = {

    val updatedJourneyData = journeyData.copy(preUserInformation = preUserInformation)

    cache.store(updatedJourneyData).map(_ => updatedJourneyData)
  }

  def storeCompleteUserInformation(journeyData: JourneyData, dto: YourJourneyDetailsDto, rawMonth: Option[String])(
    implicit
    hc: HeaderCarrier,
    ex: ExecutionContext,
    lc: LocalContext
  ): Future[JourneyData] = {

    val preUserInfo = lc.getJourneyData.preUserInformation.map { pre =>
      val newArrival = toArrivalForm(dto).copy(
        monthOfArrivalRaw = rawMonth.orElse(pre.arrivalForm.flatMap(_.monthOfArrivalRaw))
      )
      pre.copy(arrivalForm = Some(newArrival))
    }

    val updatedJourneyData = journeyData.copy(preUserInformation = preUserInfo)
    cache.store(updatedJourneyData).map(_ => updatedJourneyData)
  }
}
