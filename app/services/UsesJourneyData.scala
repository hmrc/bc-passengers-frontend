package services

import models.JourneyData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

trait UsesJourneyData {

  def localSessionCache: LocalSessionCache

  def cacheJourneyData(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[CacheMap] =
    localSessionCache.cacheJourneyData(journeyData)

  def getJourneyData(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = localSessionCache.fetchAndGetJourneyData
}
