package models

import play.api.libs.json.Json


object PurchasedProductInstance {
  implicit val formats = Json.format[PurchasedProductInstance]
}
case class PurchasedProductInstance(
  index: Int,
  weightOrVolume: Option[BigDecimal] = None,
  noOfSticks: Option[Int] = None,
  currency: Option[String] = None,
  cost: Option[BigDecimal] = None
)


object PurchasedProduct {
  implicit val formats = Json.format[PurchasedProduct]
}
case class PurchasedProduct(
  path: Option[ProductPath] = None,
  quantity: Option[Int] = None,
  purchasedProductInstances: Option[List[PurchasedProductInstance]] = None
) {
  def updatePurchasedProductInstance(index: Int)(block: PurchasedProductInstance => PurchasedProductInstance) = {
    val currentItemData = purchasedProductInstances.getOrElse(Nil).find(_.index==index).getOrElse(PurchasedProductInstance(index))
    val newItemDataList = block(currentItemData) :: purchasedProductInstances.getOrElse(Nil).filterNot(_.index == index)
    this.copy(purchasedProductInstances = Some(newItemDataList.sortBy(_.index)))
  }
}


object JourneyData {
  implicit val formats = Json.format[JourneyData]
}
case class JourneyData(
  country: Option[String] = None,
  ageOver17: Option[Boolean] = None,
  privateCraft: Option[Boolean] = None,
  selectedProducts: Option[List[List[String]]] = None,
  purchasedProducts: Option[List[PurchasedProduct]] = None
) {

  def getOrCreatePurchasedProduct(path: ProductPath): PurchasedProduct
    = purchasedProducts.getOrElse(Nil).find(_.path==Some(path)).getOrElse(PurchasedProduct(path = Some(path)))

  def updatePurchasedProduct(path: ProductPath)(block: PurchasedProduct => PurchasedProduct) = {
    val newPdList = block(getOrCreatePurchasedProduct(path)) :: purchasedProducts.getOrElse(Nil).filterNot(_.path == Some(path))
    this.copy(purchasedProducts = Some(newPdList))
  }
}
