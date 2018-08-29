package services

import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductPath}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

@Singleton
class PurchasedProductService @Inject()(val localSessionCache: LocalSessionCache) extends UsesJourneyData {

  def storeQuantity(journeyData: JourneyData, path: ProductPath, quantity: Int)(implicit hc: HeaderCarrier, ex: ExecutionContext) = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      val newQuantity = purchasedProduct.quantity.fold(quantity)(q => q + quantity)

      purchasedProduct.copy(quantity = Some(newQuantity), purchasedProductInstances = purchasedProduct.purchasedProductInstances)
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeCurrency(journeyData: JourneyData, path: ProductPath, index: Int, currency: String)(implicit hc: HeaderCarrier, ex: ExecutionContext) = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(index) { purchasedProductInstance =>
        purchasedProductInstance.copy(currency = Some(currency))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeCost(journeyData: JourneyData, path: ProductPath, index: Int, cost: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext) = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(index) { purchasedProductInstance =>
        purchasedProductInstance.copy(cost = Some(cost))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeWeightOrVolume(journeyData: JourneyData, path: ProductPath, index: Int, weightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext) = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(index) { purchasedProductInstance =>
        purchasedProductInstance.copy(weightOrVolume = Some(weightOrVolume))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeNoOfSticksAndWeightOrVolume(journeyData: JourneyData, path: ProductPath, index: Int, noOfSticks: Int, weightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext) = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(index) { purchasedProductInstance =>
        purchasedProductInstance.copy(noOfSticks = Some(noOfSticks), weightOrVolume = Some(weightOrVolume))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }

  def storeNoOfSticks(journeyData: JourneyData, path: ProductPath, index: Int, noOfSticks: Int)(implicit hc: HeaderCarrier, ex: ExecutionContext) = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { purchasedProduct =>

      purchasedProduct.updatePurchasedProductInstance(index) { purchasedProductInstance =>
        purchasedProductInstance.copy(noOfSticks = Some(noOfSticks))
      }
    }

    cacheJourneyData( updatedJourneyData )
  }


}
