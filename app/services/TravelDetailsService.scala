package services

import connectors.Cache
import javax.inject.Inject
import models.JourneyData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}


class TravelDetailsService @Inject() (
  val cache: Cache,
  implicit val ec: ExecutionContext
) {

  def storeEuCountryCheck(countryChoice: String)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>
        cache.store( journeyData.copy(euCountryCheck = Some(countryChoice), ageOver17 = None, privateCraft = None, selectedProducts = Nil) )
      case None =>
        cache.store( JourneyData(euCountryCheck = Some(countryChoice)) )
    }
  }

  def storeVatResCheck(vatResCheck: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>
        cache.store(journeyData.copy(isVatResClaimed = Some(vatResCheck)))
      case None =>
        cache.store(JourneyData(isVatResClaimed = Some(vatResCheck)))
    }
  }

  def storeDutyFreeCheck(dutyFreeCheck: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>
        cache.store(journeyData.copy(bringingDutyFree = Some(dutyFreeCheck)))
      case None =>
        cache.store(JourneyData(bringingDutyFree = Some(dutyFreeCheck)))
    }
  }


  def storeAgeOver17(ageOver17: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>
        cache.store(journeyData.copy(ageOver17 = Some(ageOver17)))
      case None =>
        cache.store( JourneyData(ageOver17 = Some(ageOver17)) )
    }
  }

  def storePrivateCraft(privateCraft: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) =>
        cache.store(journeyData.copy(privateCraft = Some(privateCraft)) )
      case None =>
        cache.store( JourneyData(privateCraft = Some(privateCraft)) )
    }
  }
}
