package models

import play.api.libs.json.Json


object PurchasedProductInstance {
  implicit val formats = Json.format[PurchasedProductInstance]
}
case class PurchasedProductInstance(
  path: ProductPath,
  iid: String,
  weightOrVolume: Option[BigDecimal] = None,
  noOfSticks: Option[Int] = None,
  currency: Option[String] = None,
  cost: Option[BigDecimal] = None
)


object PurchasedProduct {
  implicit val formats = Json.format[PurchasedProduct]
}
case class PurchasedProduct(
  path: ProductPath,
  purchasedProductInstances: List[PurchasedProductInstance] = Nil
) {
  def updatePurchasedProductInstance(iid: String)(block: PurchasedProductInstance => PurchasedProductInstance) = {
    val currentItemData = purchasedProductInstances.find(_.iid==iid).getOrElse(PurchasedProductInstance(path, iid))
    val newItemDataList = block(currentItemData) :: purchasedProductInstances.filterNot(_.iid == iid)
    this.copy(purchasedProductInstances = newItemDataList.sortBy(_.iid))
  }
  def removePurchasedProductInstance(iid: String): PurchasedProduct = {
    this.copy(purchasedProductInstances = purchasedProductInstances.filterNot(_.iid==iid))
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
  purchasedProducts: List[PurchasedProduct] = Nil,
  workingInstance: Option[PurchasedProductInstance] = None
) {

  def allCurrencyCodes: Set[String] = (for {
    purchasedProducts <- purchasedProducts
    purchasedProductInstances <- purchasedProducts.purchasedProductInstances
    currencyCode <- purchasedProductInstances.currency
  } yield currencyCode).toSet

  def getOrCreatePurchasedProduct(path: ProductPath): PurchasedProduct
    = purchasedProducts.find(_.path == path).getOrElse(PurchasedProduct(path = path))

  def updatePurchasedProduct(path: ProductPath)(block: PurchasedProduct => PurchasedProduct) = {
    val newPdList = block(getOrCreatePurchasedProduct(path)) :: purchasedProducts.filterNot(_.path == path)
    this.copy(purchasedProducts = newPdList)
  }

  def withUpdatedWorkingInstance(path: ProductPath, iid: String)(block: PurchasedProductInstance => PurchasedProductInstance) = {

    val purchasedProductInstance = block(this.workingInstance.getOrElse(PurchasedProductInstance(path, iid)))

    this.copy(workingInstance = Some(purchasedProductInstance))
  }

  def clearingWorking = this.copy(workingInstance = None)

}
