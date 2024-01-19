/*
 * Copyright 2024 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import models._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurchasedProductService @Inject() (val cache: Cache) {

  //TODO - move to NewPurchaseService
  def clearWorkingInstance(
    journeyData: JourneyData
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.clearingWorking
    cache.store(updatedJourneyData).map(_ => updatedJourneyData)
  }

  //TODO - move to NewPurchaseService
  def removePurchasedProductInstance(journeyData: JourneyData, iid: String)(implicit
    hc: HeaderCarrier,
    ex: ExecutionContext
  ): Future[JourneyData] = {

    val updatedJourneyData = journeyData.removePurchasedProductInstance(iid)
    cache.store(updatedJourneyData).map(_ => updatedJourneyData)
  }
}
