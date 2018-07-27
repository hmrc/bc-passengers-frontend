package models

sealed trait ProductTreeNode {
  val name: String
  val token: String
}

case class ProductTreeLeaf(token: String, name: String, rateID: String, templateId: String) extends ProductTreeNode {

  def getDescription(purchasedProductInstance: PurchasedProductInstance): Option[String] = {
    templateId match {
      case "cigarettes" | "cigars" =>
        purchasedProductInstance.noOfSticks map { noOfSticks =>
          noOfSticks + " " + name.toLowerCase()
        }
      case "tobacco" =>
        purchasedProductInstance.weightOrVolume.map { weightOrVolume =>
          weightOrVolume + "g " + name.toLowerCase()
        }
      case "alcohol" =>
        purchasedProductInstance.weightOrVolume.map { weightOrVolume =>
          weightOrVolume + " litres " + name.toLowerCase()
        }
      case "other-goods" => Some(name)
      case _ => None
    }
  }

  def getDisplayWeight(purchasedProductInstance: PurchasedProductInstance): Option[String] = {
    templateId match {
      case "cigars" =>
        purchasedProductInstance.weightOrVolume map { weightOrVolume =>
          weightOrVolume + "g"
        }
      case _ => None
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