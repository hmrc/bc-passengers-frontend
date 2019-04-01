package services


import connectors.Cache
import javax.inject.{Inject, Singleton}
import models._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurchasedProductService @Inject()(val cache: Cache) {

  //TODO - move to NewPurchaseService
  def clearWorkingInstance(journeyData: JourneyData)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.clearingWorking
    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }

  //TODO - move to NewPurchaseService
  def removePurchasedProductInstance(journeyData: JourneyData, path: ProductPath, iid: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[JourneyData] = {

    val updatedJourneyData = journeyData.removePurchasedProductInstance(iid)
    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }
}
