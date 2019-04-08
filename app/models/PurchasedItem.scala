package models

import play.api.i18n.Messages

case class PurchasedItem(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeLeaf: ProductTreeLeaf,
  currency: Currency,
  gbpCost: BigDecimal,
  exchangeRate: ExchangeRate
) {
  def descriptionArgs(long: Boolean)(implicit messages: Messages): Option[(String, List[String])] = productTreeLeaf.getDescriptionArgs(purchasedProductInstance, long)
  def name: String = productTreeLeaf.name
  def displayCurrency: String = currency.displayName
  def country: Option[String] = purchasedProductInstance.country.map(_.countryName)
}

case class SpeculativeItem(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeLeaf: ProductTreeLeaf,
  gbpCost: BigDecimal
)
