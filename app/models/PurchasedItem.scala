package models

case class PurchasedItem(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeLeaf: ProductTreeLeaf,
  currency: Currency,
  gbpCost: BigDecimal
) {
  def description = productTreeLeaf.getDescription(purchasedProductInstance)
  def displayWeight = productTreeLeaf.getDisplayWeight(purchasedProductInstance)
  def displayCurrency = currency.displayName
}
