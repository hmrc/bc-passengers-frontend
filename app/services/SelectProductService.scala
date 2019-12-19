package services


import connectors.Cache
import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductAlias, ProductPath, ProductTreeBranch, ProductTreeLeaf}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectProductService @Inject()(
  val cache: Cache,
  val productTreeService: ProductTreeService,
  implicit val ec: ExecutionContext
) {

  def addSelectedProductsAsAliases(journeyData: JourneyData, selectedProducts: List[ProductPath])(implicit hc: HeaderCarrier) = {

    val aliases: List[ProductAlias] = journeyData.selectedAliases ++ selectedProducts.map(productPath => ProductAlias("label." + productPath.toMessageKey, productPath))

    val aliasesOrdered = aliases.sortWith { (aliasA, aliasB) =>

      val dA = productTreeService.productTree.getDescendant(aliasA.productPath)
      val dB = productTreeService.productTree.getDescendant(aliasB.productPath)

      (dA, dB) match {
        case (Some(ProductTreeBranch(_, _, _)), Some(ProductTreeLeaf(_, _, _, _, _))) => true
        case (Some(ProductTreeLeaf(_, _, _, _, _)), Some(ProductTreeBranch(_, _, _))) => false
        case _ => false
      }
    }

    val newJd = journeyData.copy(selectedAliases = aliasesOrdered)
    cache.store(newJd) map { _ =>
      newJd
    }
  }

  def removeSelectedAlias(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[CacheMap] = {
    cache.store(journeyData.copy(selectedAliases = journeyData.selectedAliases.tail))
  }


}
