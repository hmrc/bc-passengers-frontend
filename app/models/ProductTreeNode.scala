/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import utils.FormatsAndConversions

import util.decimalFormat10
import utils.FormatsAndConversions

sealed trait ProductTreeNode {
  def name: String
  def token: String
  def isLeaf: Boolean
  def isBranch: Boolean = !isLeaf
}

case class ProductTreeLeaf(
  token: String,
  name: String,
  rateID: String,
  templateId: String,
  applicableLimits: List[String]
) extends ProductTreeNode
    with FormatsAndConversions {

  override def isLeaf: Boolean = true

  def getDescriptionLabels(
    purchasedProductInstance: PurchasedProductInstance,
    long: Boolean
  ): Option[(String, List[String])] =
    templateId match {
      case "cigars" if long        =>
        for {
          noOfSticks     <- purchasedProductInstance.noOfSticks
          weightOrVolume <- purchasedProductInstance.weightOrVolume
        } yield
          if (noOfSticks == 1) {
            (
              "label.X_X_Xg",
              List(
                noOfSticks.toString,
                name + ".single",
                decimalFormat10.format(weightOrVolume * 1000)
              )
            )
          } else {
            ("label.X_X_Xg", List(noOfSticks.toString, name, decimalFormat10.format(weightOrVolume * 1000)))
          }
      case "cigarettes" | "cigars" =>
        for (noOfSticks <- purchasedProductInstance.noOfSticks)
          yield
            if (noOfSticks == 1) {
              ("label.X_X", List(noOfSticks.toString, name + ".single"))
            } else {
              ("label.X_X", List(noOfSticks.toString, name))
            }
      case "tobacco"               =>
        for (weightOrVolume <- purchasedProductInstance.weightOrVolume)
          yield ("label.Xg_of_X", List(decimalFormat10.format(weightOrVolume * 1000), name))
      case "alcohol"               =>
        for (weightOrVolume <- purchasedProductInstance.weightOrVolume)
          yield
            if (weightOrVolume == BigDecimal(1)) {
              ("label.X_litre_X", List(weightOrVolume.toString, name))
            } else {
              ("label.X_litres_X", List(weightOrVolume.toString, name))
            }
      case "other-goods"           =>
        Some((name, Nil))
    }

  def isValid(purchasedProductInstance: PurchasedProductInstance): Boolean =
    templateId match {
      case "cigarettes"  =>
        purchasedProductInstance.currency.isDefined &&
          purchasedProductInstance.cost.isDefined &&
          purchasedProductInstance.country.isDefined &&
          purchasedProductInstance.noOfSticks.isDefined
      case "cigars"      =>
        purchasedProductInstance.currency.isDefined &&
          purchasedProductInstance.cost.isDefined &&
          purchasedProductInstance.country.isDefined &&
          purchasedProductInstance.weightOrVolume.isDefined &&
          purchasedProductInstance.noOfSticks.isDefined
      case "tobacco"     =>
        purchasedProductInstance.currency.isDefined &&
          purchasedProductInstance.cost.isDefined &&
          purchasedProductInstance.country.isDefined &&
          purchasedProductInstance.weightOrVolume.isDefined
      case "alcohol"     =>
        purchasedProductInstance.currency.isDefined &&
          purchasedProductInstance.cost.isDefined &&
          purchasedProductInstance.country.isDefined &&
          purchasedProductInstance.weightOrVolume.isDefined
      case "other-goods" =>
        purchasedProductInstance.currency.isDefined &&
          purchasedProductInstance.country.isDefined &&
          purchasedProductInstance.cost.isDefined
      case _             => false
    }

}

case class ProductTreeBranch(token: String, name: String, children: List[ProductTreeNode]) extends ProductTreeNode {

  override def isLeaf: Boolean = false

  def getDescendant(path: ProductPath): Option[ProductTreeNode] =
    children.find(_.token == path.components.head) match {
      case None                       => None
      case Some(c: ProductTreeLeaf)   => Some(c)
      case Some(c: ProductTreeBranch) =>
        path.components.tail match {
          case Nil => Some(c)
          case _   => c.getDescendant(ProductPath(path.components.tail))
        }
    }
}
