package services

import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductPath}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurchasedProductService @Inject()(val localSessionCache: LocalSessionCache) extends UsesJourneyData {

  def clearWorkingInstance(journeyData: JourneyData)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.clearingWorking
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def removePurchasedProductInstance(journeyData: JourneyData, path: ProductPath, iid: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.updatePurchasedProduct(path) { pp =>
      pp.removePurchasedProductInstance(iid)
    }
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeCurrency(journeyData: JourneyData, path: ProductPath, iid: String, currency: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, currency = Some(currency))
    }

    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeCost(journeyData: JourneyData, path: ProductPath, iid: String, cost: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, cost = Some(cost))
    }

    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeWeightOrVolume(journeyData: JourneyData, path: ProductPath, iid: String, weightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, weightOrVolume = Some(weightOrVolume))
    }

    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeNoOfSticksAndWeightOrVolume(journeyData: JourneyData, path: ProductPath, iid: String, noOfSticks: Int, weightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, noOfSticks = Some(noOfSticks), weightOrVolume = Some(weightOrVolume))
    }
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeNoOfSticks(journeyData: JourneyData, path: ProductPath, iid: String, noOfSticks: Int)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, noOfSticks = Some(noOfSticks))
    }

    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }
}
