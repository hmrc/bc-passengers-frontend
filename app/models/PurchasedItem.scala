/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

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
  def countryName: Option[String] = purchasedProductInstance.country.map(_.countryName)
  def countryCode: Option[String] = purchasedProductInstance.country.map(_.code)
}

case class SpeculativeItem(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeLeaf: ProductTreeLeaf,
  gbpCost: BigDecimal
)
