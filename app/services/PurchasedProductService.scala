package services

import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductPath, PurchasedProductInstance}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurchasedProductService @Inject()(val localSessionCache: LocalSessionCache) extends UsesJourneyData {

  def clearWorkingInstance(journeyData: JourneyData)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.clearingWorking
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def makeWorkingInstance(journeyData: JourneyData, purchasedProductInstance: PurchasedProductInstance)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(purchasedProductInstance.path, purchasedProductInstance.iid) { _ =>
      purchasedProductInstance
    }

    cacheJourneyData(updatedJourneyData).map(_ => updatedJourneyData)
  }

  def removePurchasedProductInstance(journeyData: JourneyData, path: ProductPath, iid: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.removePurchasedProductInstance(iid)
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeCountry(journeyData: JourneyData, path: ProductPath, iid: String, country: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {
    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, country = Some(country))
    }

    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def updateCountry(journeyData: JourneyData, path: ProductPath, iid: String, updatedCountry:String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {
    val updatedProductInstances = journeyData.purchasedProductInstances.map { ppi =>
      if (ppi.iid == iid)
        ppi.copy(country = Some(updatedCountry))
      else
        ppi
    }

    val updatedJourneyData = journeyData.copy(purchasedProductInstances = updatedProductInstances)
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

  def updateWeightOrVolume(journeyData: JourneyData, path: ProductPath, iid: String, updatedWeightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {
    val updatedProductInstances = journeyData.purchasedProductInstances.map { ppi =>
      if (ppi.iid == iid)
        ppi.copy(weightOrVolume = Some(updatedWeightOrVolume))
      else
        ppi
    }

    val updatedJourneyData = journeyData.copy(purchasedProductInstances = updatedProductInstances)
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeNoOfSticksAndWeightOrVolume(journeyData: JourneyData, path: ProductPath, iid: String, noOfSticks: Int, weightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, noOfSticks = Some(noOfSticks), weightOrVolume = Some(weightOrVolume))
    }
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def updateNoOfSticksAndWeightOrVolume(journeyData: JourneyData, path: ProductPath, iid: String, updatedNoOfSticks: Int, weightOrVolume: BigDecimal)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {
    val updatedProductInstances = journeyData.purchasedProductInstances.map { ppi =>
      if (ppi.iid == iid)
        ppi.copy(noOfSticks = Some(updatedNoOfSticks), weightOrVolume = Some(weightOrVolume))
      else
        ppi
    }

    val updatedJourneyData = journeyData.copy(purchasedProductInstances = updatedProductInstances)
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeNoOfSticks(journeyData: JourneyData, path: ProductPath, iid: String, noOfSticks: Int)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, noOfSticks = Some(noOfSticks))
    }

    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def updateNoOfSticks(journeyData: JourneyData, path: ProductPath, iid: String, updatedNoOfSticks: Int)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {
    val updatedProductInstances = journeyData.purchasedProductInstances.map { ppi =>
      if (ppi.iid == iid)
        ppi.copy(noOfSticks = Some(updatedNoOfSticks))
      else
        ppi
    }

    val updatedJourneyData = journeyData.copy(purchasedProductInstances = updatedProductInstances)
    cacheJourneyData( updatedJourneyData ).map(_ => updatedJourneyData)
  }
}
