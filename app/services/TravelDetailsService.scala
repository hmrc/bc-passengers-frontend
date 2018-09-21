package services

import javax.inject.Inject
import models.JourneyData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future


class TravelDetailsService @Inject() (val localSessionCache: LocalSessionCache) extends UsesJourneyData {

  def storeCountry(country: String)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData( journeyData.copy(country = Some(country), ageOver17 = None, privateCraft = None, selectedProducts = Nil) )
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(country = Some(country)) )
    }
  }

  def storeAgeOver17(ageOver17: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData(journeyData.copy(ageOver17 = Some(ageOver17)))
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(ageOver17 = Some(ageOver17)) )
    }
  }

  def storePrivateCraft(privateCraft: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData(journeyData.copy(privateCraft = Some(privateCraft)) )
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(privateCraft = Some(privateCraft)) )
    }
  }
}
