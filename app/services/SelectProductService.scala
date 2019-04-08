package services


import connectors.Cache
import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductPath}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectProductService @Inject()(
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService,
  implicit val ec: ExecutionContext
) {

  def addSelectedProducts(journeyData: JourneyData, selectedProducts: List[ProductPath])(implicit hc: HeaderCarrier) = {
    val truncatedCurrentSelectedProducts = journeyData.selectedProducts.foldLeft[List[List[String]]](List[List[String]]()) { (acc, ele) =>
      val cat = ele.dropRight(1)
      if(selectedProducts.exists(_.categoryComponent == cat)) acc
      else acc :+ ele
    }

    val newJd = journeyData.copy(selectedProducts = (selectedProducts.map(_.components) ++ truncatedCurrentSelectedProducts))
    cache.store(newJd) map { _ =>
      newJd
    }
  }

  def removeSelectedProduct()(implicit hc: HeaderCarrier): Future[CacheMap] = {

    cache.fetch flatMap {
      case Some(journeyData) => {
        cache.store(journeyData.copy(selectedProducts = journeyData.selectedProducts.tail))
      }
      case None => cache.store(JourneyData())  //FIXME
    }
  }


}
