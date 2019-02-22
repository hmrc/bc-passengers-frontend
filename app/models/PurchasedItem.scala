package models


case class PurchasedItem(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeLeaf: ProductTreeLeaf,
  currency: Currency,
  gbpCost: BigDecimal,
  exchangeRate: ExchangeRate
) {
  def description: Option[String] = productTreeLeaf.getDescription(purchasedProductInstance)
  def declarationMessageDescription: String = productTreeLeaf.name
  def displayWeight: Option[String] = productTreeLeaf.getDisplayWeight(purchasedProductInstance)
  def displayCurrency: String = currency.displayName
  def country: Option[String] = purchasedProductInstance.country.map(_.countryName)
}

case class SpeculativeItem(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeLeaf: ProductTreeLeaf,
  gbpCost: BigDecimal
)
