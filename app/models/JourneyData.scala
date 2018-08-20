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
  purchasedProductInstances: List[PurchasedProductInstance] = Nil
) {
  def updatePurchasedProductInstance(index: Int)(block: PurchasedProductInstance => PurchasedProductInstance) = {
    val currentItemData = purchasedProductInstances.find(_.index==index).getOrElse(PurchasedProductInstance(index))
    val newItemDataList = block(currentItemData) :: purchasedProductInstances.filterNot(_.index == index)
    this.copy(purchasedProductInstances = newItemDataList.sortBy(_.index))
  }
}


object JourneyData {
  implicit val formats = Json.format[JourneyData]
}
case class JourneyData(
  country: Option[String] = None,
  ageOver17: Option[Boolean] = None,
  privateCraft: Option[Boolean] = None,
  selectedProducts: List[List[String]] = Nil,
  purchasedProducts: List[PurchasedProduct] = Nil
) {

  def allCurrencyCodes: Set[String] = (for {
    purchasedProducts <- purchasedProducts
    purchasedProductInstances <- purchasedProducts.purchasedProductInstances
    currencyCode <- purchasedProductInstances.currency
  } yield currencyCode).toSet

  def getOrCreatePurchasedProduct(path: ProductPath): PurchasedProduct
    = purchasedProducts.find(_.path==Some(path)).getOrElse(PurchasedProduct(path = Some(path)))

  def updatePurchasedProduct(path: ProductPath)(block: PurchasedProduct => PurchasedProduct) = {
    val newPdList = block(getOrCreatePurchasedProduct(path)) :: purchasedProducts.filterNot(_.path == Some(path))
    this.copy(purchasedProducts = newPdList)
  }
}
