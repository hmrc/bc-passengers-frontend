package services

import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductPath}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectProductService @Inject()(val localSessionCache: LocalSessionCache) extends UsesJourneyData {

  def addSelectedProducts(selectedProducts: List[ProductPath])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {

    loanAndUpdateJourneyData { jd =>
      val sp = selectedProducts.map(_.components) ++ jd.selectedProducts.getOrElse(Nil)
      jd.copy(selectedProducts = Some(sp))
    }
  }

  def removeSelectedProduct()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) => {
        journeyData.selectedProducts match {
          case Some(products) => localSessionCache.cacheJourneyData(journeyData.copy(selectedProducts = Some(products.tail)))
          case None => localSessionCache.cacheJourneyData(journeyData.copy(selectedProducts = None))
        }
      }
      case None => localSessionCache.cacheJourneyData(JourneyData())  //FIXME
    }
  }



}
