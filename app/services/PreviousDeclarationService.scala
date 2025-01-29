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
import javax.inject.Inject
import models.{JourneyData, PreviousDeclarationRequest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class PreviousDeclarationService @Inject() (
  val cache: Cache,
  val declarationService: DeclarationService,
  implicit val ec: ExecutionContext
) {

  def storePrevDeclaration(
    journeyData: Option[JourneyData]
  )(prevDeclaration: Boolean)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] =
    journeyData match {
      case Some(journeyData) if !journeyData.prevDeclaration.contains(prevDeclaration) =>
        val resetJourneyData = JourneyData(
          prevDeclaration = Some(prevDeclaration)
        )

        cache.storeJourneyData(resetJourneyData)

      case None =>
        cache.storeJourneyData(JourneyData(prevDeclaration = Some(prevDeclaration)))

      case _ =>
        Future.successful(journeyData)
    }

  def storePrevDeclarationDetails(
    journeyData: Option[JourneyData]
  )(previousDeclarationRequest: PreviousDeclarationRequest)(implicit hc: HeaderCarrier): Future[Option[JourneyData]] =
    journeyData match {
      case Some(_) =>
        declarationService.retrieveDeclaration(previousDeclarationRequest).flatMap {
          case DeclarationServiceRetrieveSuccessResponse(retrievedJourneyData) =>
            cache.storeJourneyData(
              retrievedJourneyData.copy(
                pendingPayment = journeyData.get.pendingPayment,
                declarationResponse = retrievedJourneyData.declarationResponse.map { ds =>
                  ds.copy(oldPurchaseProductInstances =
                    ds.oldPurchaseProductInstances.map(ppi => ppi.copy(isEditable = Some(false)))
                  )
                }
              )
            )
          case DeclarationServiceFailureResponse                               =>
            cache.storeJourneyData(
              JourneyData(
                pendingPayment = journeyData.get.pendingPayment,
                prevDeclaration = Some(true),
                previousDeclarationRequest = Some(previousDeclarationRequest)
              )
            )
          case _                                                               =>
            cache.storeJourneyData(
              JourneyData(
                pendingPayment = journeyData.get.pendingPayment,
                prevDeclaration = Some(true),
                previousDeclarationRequest = Some(previousDeclarationRequest)
              )
            )
        }
      case None    =>
        cache.storeJourneyData(
          JourneyData(
            prevDeclaration = Some(true),
            previousDeclarationRequest = Some(previousDeclarationRequest)
          )
        )

      case _ =>
        Future.successful(journeyData)
    }
}
