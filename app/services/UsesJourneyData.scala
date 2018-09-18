package services

import models.JourneyData
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait UsesJourneyData {

  def localSessionCache: LocalSessionCache

  def cacheJourneyData(journeyData: JourneyData)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    localSessionCache.cacheJourneyData(journeyData)

  def getJourneyData(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = localSessionCache.fetchAndGetJourneyData
}
