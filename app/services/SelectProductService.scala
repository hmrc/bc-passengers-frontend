/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import connectors.Cache
import javax.inject.{Inject, Singleton}
import models.{JourneyData, ProductAlias, ProductPath, ProductTreeBranch, ProductTreeLeaf}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectProductService @Inject() (
  val cache: Cache,
  val productTreeService: ProductTreeService,
  implicit val ec: ExecutionContext
) {

  def addSelectedProductsAsAliases(journeyData: JourneyData, selectedProducts: List[ProductPath])(implicit
    hc: HeaderCarrier
  ): Future[JourneyData] = {

    val aliases: List[ProductAlias] = journeyData.selectedAliases ++ selectedProducts.map(productPath =>
      ProductAlias("label." + productPath.toMessageKey, productPath)
    )

    val aliasesOrdered = aliases.sortWith { (aliasA, aliasB) =>
      val dA = productTreeService.productTree.getDescendant(aliasA.productPath)
      val dB = productTreeService.productTree.getDescendant(aliasB.productPath)

      (dA, dB) match {
        case (Some(ProductTreeBranch(_, _, _)), Some(ProductTreeLeaf(_, _, _, _, _))) => true
        case (Some(ProductTreeLeaf(_, _, _, _, _)), Some(ProductTreeBranch(_, _, _))) => false
        case _                                                                        => false
      }
    }

    val newJd = journeyData.copy(selectedAliases = aliasesOrdered)
    cache.store(newJd) map { _ =>
      newJd
    }
  }

  def removeSelectedAlias(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[JourneyData] =
    cache.store(journeyData.copy(workingInstance = None, selectedAliases = journeyData.selectedAliases.tail))

}
