/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import connectors.Cache
import javax.inject.Inject
import models.{JourneyData, PreviousDeclarationRequest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


class PreviousDeclarationService @Inject()(
  val cache: Cache,
  val declarationService: DeclarationService,
  implicit val ec: ExecutionContext
) {

  def storePrevDeclaration(journeyData: Option[JourneyData])(prevDeclaration: Boolean)
                          (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {

    journeyData match {
      case Some(journeyData) if !journeyData.prevDeclaration.contains(prevDeclaration) =>

        val resetJourneyData = JourneyData(
          prevDeclaration = Some(prevDeclaration)
        )

        cache.storeJourneyData(resetJourneyData)

      case None =>
        cache.storeJourneyData( JourneyData(
          prevDeclaration = Some(prevDeclaration)) )

      case _ =>
        Future.successful( journeyData )
    }
  }

  def storePrevDeclarationDetails(journeyData: Option[JourneyData])(previousDeclarationRequest: PreviousDeclarationRequest)
                                 (implicit hc: HeaderCarrier): Future[Option[JourneyData]] = {
    journeyData match {
      case Some(_) =>
        declarationService.retrieveDeclaration(previousDeclarationRequest) flatMap {
          case DeclarationServiceRetrieveSuccessResponse(retrievedJourneyData) =>
            cache.storeJourneyData(retrievedJourneyData.copy(
              declarationResponse = retrievedJourneyData.declarationResponse.map { ds =>
                ds.copy(oldPurchaseProductInstances = ds.oldPurchaseProductInstances.map(ppi =>
                  ppi.copy(isEditable = Some(false))
                ))
              })
            )
          case DeclarationServiceFailureResponse =>
            cache.storeJourneyData(JourneyData(
              prevDeclaration = Some(true),
              previousDeclarationRequest = Some(previousDeclarationRequest)
            ))
        }
      case None =>
        cache.storeJourneyData(JourneyData(
          prevDeclaration = Some(true),
          previousDeclarationRequest = Some(previousDeclarationRequest)
        ))

      case _ =>
        Future.successful(journeyData)
    }
  }
}
