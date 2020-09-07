/*
 * Copyright 2020 HM Revenue & Customs
 *
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
