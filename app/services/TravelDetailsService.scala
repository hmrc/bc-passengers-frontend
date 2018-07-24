package services

import javax.inject.{Inject, Singleton}

import models.JourneyData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._



@Singleton
class TravelDetailsService @Inject() ( val localSessionCache: LocalSessionCache) {

  def storeCountry(country: String)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData( journeyData.copy(country = Some(country), ageOver17 = None, privateCraft = None, selectedProducts = None) )
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(country = Some(country)) )
    }
  }

  def storeAgeOver17(ageOver17: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData( journeyData.copy(ageOver17 = Some(ageOver17), privateCraft = None, selectedProducts = None) )
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(ageOver17 = Some(ageOver17)) )
    }
  }

  def storePrivateCraft(privateCraft: Boolean)(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) =>
        localSessionCache.cacheJourneyData( journeyData.copy(privateCraft = Some(privateCraft), selectedProducts = None) )
      case None =>
        localSessionCache.cacheJourneyData( JourneyData(privateCraft = Some(privateCraft)) )
    }
  }

  def addSelectedProducts(selectedProducts: List[List[String]])(implicit hc: HeaderCarrier): Future[CacheMap] = {

      localSessionCache.fetchAndGetJourneyData flatMap {
        case Some(journeyData) => {
          journeyData.selectedProducts match {
            case Some(products) => localSessionCache.cacheJourneyData(journeyData.copy(selectedProducts = Some(selectedProducts ::: products)))
            case None => localSessionCache.cacheJourneyData(journeyData.copy(selectedProducts = Some(selectedProducts)))
          }
        }
        case None => localSessionCache.cacheJourneyData(JourneyData(selectedProducts = Some(selectedProducts)))
      }
  }

  def removeSelectedProduct()(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) => {
        journeyData.selectedProducts match {
          case Some(products) => localSessionCache.cacheJourneyData(journeyData.copy(selectedProducts = Some(products.tail)))
          case None => localSessionCache.cacheJourneyData(journeyData.copy(selectedProducts = None))
        }
      }
      case None => localSessionCache.cacheJourneyData(JourneyData())
    }
  }

  def getUserInputData(implicit hc: HeaderCarrier): Future[Option[JourneyData]] = localSessionCache.fetchAndGetJourneyData
}
