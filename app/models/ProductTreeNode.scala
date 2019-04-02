package models


import util._

sealed trait ProductTreeNode {
  val name: String
  val token: String
}

case class ProductTreeLeaf(token: String, name: String, rateID: String, templateId: String, applicableLimits: List[String]) extends ProductTreeNode {

  def getDescriptionArgs(purchasedProductInstance: PurchasedProductInstance, long: Boolean): Option[(String, List[String])] = {

    templateId match {
      case "cigars" if long =>
        for(noOfSticks <- purchasedProductInstance.noOfSticks; weightOrVolume <- purchasedProductInstance.weightOrVolume) yield
          ("label.X_X_Xg", List(noOfSticks.toString, name.toLowerCase(), decimalFormat10.format(weightOrVolume*1000)))
      case "cigarettes" | "cigars" =>
        for(noOfSticks <- purchasedProductInstance.noOfSticks) yield
          ("label.X_X", List(noOfSticks.toString, name.toLowerCase()))
      case "tobacco" =>
        for(weightOrVolume <- purchasedProductInstance.weightOrVolume) yield
          ("label.Xg_of_X", List(decimalFormat10.format(weightOrVolume * 1000), name.toLowerCase()))
      case "alcohol" =>
        for(weightOrVolume <- purchasedProductInstance.weightOrVolume) yield
          if (weightOrVolume == BigDecimal(1))
            ("label.X_litre_X", List(weightOrVolume.toString, name.toLowerCase()))
          else
            ("label.X_litres_X", List(weightOrVolume.toString, name.toLowerCase()))
      case "other-goods" =>
        Some( (name, Nil) )
    }
  }

  def isValid(purchasedProductInstance: PurchasedProductInstance): Boolean = {

    templateId match {
      case "cigarettes" =>
        purchasedProductInstance.currency.isDefined &&
        purchasedProductInstance.cost.isDefined &&
        purchasedProductInstance.country.isDefined &&
        purchasedProductInstance.noOfSticks.isDefined
      case "cigars" =>
        purchasedProductInstance.currency.isDefined &&
        purchasedProductInstance.cost.isDefined &&
        purchasedProductInstance.country.isDefined &&
        purchasedProductInstance.weightOrVolume.isDefined &&
        purchasedProductInstance.noOfSticks.isDefined
      case "tobacco" =>
        purchasedProductInstance.currency.isDefined &&
        purchasedProductInstance.cost.isDefined &&
        purchasedProductInstance.country.isDefined &&
        purchasedProductInstance.weightOrVolume.isDefined
      case "alcohol" =>
        purchasedProductInstance.currency.isDefined &&
        purchasedProductInstance.cost.isDefined &&
        purchasedProductInstance.country.isDefined &&
        purchasedProductInstance.weightOrVolume.isDefined
      case "other-goods" =>
        purchasedProductInstance.currency.isDefined &&
        purchasedProductInstance.country.isDefined &&
        purchasedProductInstance.cost.isDefined
      case _ => false
    }
  }

}

case class ProductTreeBranch(token: String, name: String, children: List[ProductTreeNode]) extends ProductTreeNode {

  def getDescendant(path: ProductPath): Option[ProductTreeNode] = {
    children.find(_.token == path.components.head) match {
      case None => None
      case Some(c: ProductTreeLeaf) => Some(c)
      case Some(c: ProductTreeBranch) => path.components.tail match {
        case Nil => Some(c)
        case _ => c.getDescendant(ProductPath(path.components.tail))
      }
    }
  }
}
