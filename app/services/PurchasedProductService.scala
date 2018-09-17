package services

import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductPath}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class PurchasedProductService @Inject()(val localSessionCache: LocalSessionCache) extends UsesJourneyData {

  def storeCurrency(journeyData: JourneyData, path: ProductPath, iid: String, currency: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[CacheMap] = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(iid) { purchasedProductInstance =>
        purchasedProductInstance.copy(currency = Some(currency))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeCost(journeyData: JourneyData, path: ProductPath, iid: String, cost: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[CacheMap] = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(iid) { purchasedProductInstance =>
        purchasedProductInstance.copy(cost = Some(cost))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeWeightOrVolume(journeyData: JourneyData, path: ProductPath, iid: String, weightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[CacheMap] = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(iid) { purchasedProductInstance =>
        purchasedProductInstance.copy(weightOrVolume = Some(weightOrVolume))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeNoOfSticksAndWeightOrVolume(journeyData: JourneyData, path: ProductPath, iid: String, noOfSticks: Int, weightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[CacheMap] = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(iid) { purchasedProductInstance =>
        purchasedProductInstance.copy(noOfSticks = Some(noOfSticks), weightOrVolume = Some(weightOrVolume))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeNoOfSticks(journeyData: JourneyData, path: ProductPath, iid: String, noOfSticks: Int)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[CacheMap] = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(iid) { purchasedProductInstance =>
        purchasedProductInstance.copy(noOfSticks = Some(noOfSticks))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }
}
