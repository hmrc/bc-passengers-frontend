package services

import javax.inject.Inject
import models.JourneyData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}


class TravelDetailsService @Inject() (
  val localSessionCache: LocalSessionCache,
  implicit val ec: ExecutionContext
) extends UsesJourneyData {

  def storeEuCountryCheck(countryChoice: String)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData( journeyData.copy(euCountryCheck = Some(countryChoice), ageOver17 = None, privateCraft = None, selectedProducts = Nil) )
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(euCountryCheck = Some(countryChoice)) )
    }
  }

  def storeVatResCheck(vatResCheck: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData(journeyData.copy(isVatResClaimed = Some(vatResCheck)))
      case None =>
        localSessionCache.cacheJourneyData(JourneyData(isVatResClaimed = Some(vatResCheck)))
    }
  }

  def storeDutyFreeCheck(dutyFreeCheck: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData(journeyData.copy(bringingDutyFree = Some(dutyFreeCheck)))
      case None =>
        localSessionCache.cacheJourneyData(JourneyData(bringingDutyFree = Some(dutyFreeCheck)))
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
