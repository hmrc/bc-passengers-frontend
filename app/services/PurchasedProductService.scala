package services


import connectors.Cache
import javax.inject.{Inject, Singleton}
import models._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurchasedProductService @Inject()(val cache: Cache) {

  def clearWorkingInstance(journeyData: JourneyData)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.clearingWorking
    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def makeWorkingInstance(journeyData: JourneyData, purchasedProductInstance: PurchasedProductInstance)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(purchasedProductInstance.path, purchasedProductInstance.iid) { _ =>
      purchasedProductInstance
    }

    cache.store(updatedJourneyData).map(_ => updatedJourneyData)
  }

  def removePurchasedProductInstance(journeyData: JourneyData, path: ProductPath, iid: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.removePurchasedProductInstance(iid)
    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeCountry(journeyData: JourneyData, path: ProductPath, iid: String, country: Country)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {
    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, country = Some(country))
    }

    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def updateCountry(journeyData: JourneyData, path: ProductPath, iid: String, updatedCountry: Country)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {
    val updatedProductInstances = journeyData.purchasedProductInstances.map { ppi =>
      if (ppi.iid == iid)
        ppi.copy(country = Some(updatedCountry))
      else
        ppi
    }

    val updatedJourneyData = journeyData.copy(purchasedProductInstances = updatedProductInstances)
    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  def storeCurrency(journeyData: JourneyData, path: ProductPath, iid: String, currency: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.withUpdatedWorkingInstance(path, iid) { purchasedProductInstance =>
      purchasedProductInstance.copy(path = path, iid = iid, currency = Some(currency))
    }

    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }

}
