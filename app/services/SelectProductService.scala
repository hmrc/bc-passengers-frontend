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
  val currencyService: CurrencyService,
  implicit val ec: ExecutionContext
) extends UsesJourneyData {

  def addSelectedProducts(journeyData: JourneyData, selectedProducts: List[ProductPath])(implicit hc: HeaderCarrier) = {

    val truncatedCurrentSelectedProducts = journeyData.selectedProducts.foldLeft[List[List[String]]](List[List[String]]()) { (acc, ele) =>
      val cat = ele.dropRight(1)
      if(selectedProducts.exists(_.categoryComponent == cat)) acc
      else acc :+ ele
    }

    val newJd = journeyData.copy(selectedProducts = (selectedProducts.map(_.components) ++ truncatedCurrentSelectedProducts))
    cacheJourneyData(newJd) map { _ =>
      newJd
    }
  }

  def removeSelectedProduct()(implicit hc: HeaderCarrier): Future[CacheMap] = {

    localSessionCache.fetchAndGetJourneyData flatMap {
      case Some(journeyData) => {
        localSessionCache.cacheJourneyData(journeyData.copy(selectedProducts = journeyData.selectedProducts.tail))
      }
      case None => localSessionCache.cacheJourneyData(JourneyData())  //FIXME
    }
  }


}
