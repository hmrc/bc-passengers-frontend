package services

import javax.inject.{Inject, Singleton}
import models.{JourneyData, PurchasedItem, ProductPath, ProductTreeLeaf}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectProductService @Inject()(
  val localSessionCache: LocalSessionCache,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService
) extends UsesJourneyData {

  def addSelectedProducts(selectedProducts: List[ProductPath])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {

    loanAndUpdateJourneyData { jd =>
      val sp = selectedProducts.map(_.components) ++ jd.selectedProducts
      jd.copy(selectedProducts = sp)
    }
  }

  def removeSelectedProduct()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) => {
        localSessionCache.cacheJourneyData(journeyData.copy(selectedProducts = journeyData.selectedProducts.tail))
      }
      case None => localSessionCache.cacheJourneyData(JourneyData())  //FIXME
    }
  }


}
