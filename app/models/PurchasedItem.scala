/*
 * Copyright 2025 HM Revenue & Customs
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

case class PurchasedItem(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeLeaf: ProductTreeLeaf,
  currency: Currency,
  gbpCost: BigDecimal,
  exchangeRate: ExchangeRate
) {
  def descriptionLabels(long: Boolean): Option[(String, List[String])] =
    productTreeLeaf.getDescriptionLabels(purchasedProductInstance, long)
  def name: String                                                     = productTreeLeaf.name
  def displayCurrency: String                                          = currency.displayName
  def countryName: Option[String]                                      = purchasedProductInstance.country.map(_.countryName)
  def countryCode: Option[String]                                      = purchasedProductInstance.country.map(_.code)
  def originCountry: Option[String]                                    = purchasedProductInstance.originCountry.map(_.countryName)
  def hasEvidence: Option[Boolean]                                     = purchasedProductInstance.hasEvidence
}

case class SpeculativeItem(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeLeaf: ProductTreeLeaf,
  gbpCost: BigDecimal
)
