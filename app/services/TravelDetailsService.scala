package services

import javax.inject.{Inject, Singleton}
import models.{PurchasedProductInstance, JourneyData, PurchasedProduct}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._


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
        localSessionCache.cacheJourneyData( journeyData.copy(ageOver17 = Some(ageOver17), privateCraft = None, selectedProducts = Nil) )
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(ageOver17 = Some(ageOver17)) )
    }
  }

  def storePrivateCraft(privateCraft: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData( journeyData.copy(privateCraft = Some(privateCraft), selectedProducts = Nil) )
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(privateCraft = Some(privateCraft)) )
    }
  }


  def storeProductDetails(productDetailsToStore: PurchasedProduct)(implicit hc: HeaderCarrier): Future[CacheMap] = {
    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData(journeyData.copy(purchasedProducts = productDetailsToStore :: journeyData.purchasedProducts))

      case None =>
        localSessionCache.cacheJourneyData(JourneyData(purchasedProducts = List(productDetailsToStore)))
    }
  }
}
