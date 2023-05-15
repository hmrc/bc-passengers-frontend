/*
 * Copyright 2023 HM Revenue & Customs
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

import util.BaseSpec

class PurchasedItemSpec extends BaseSpec {

  private val weightOrVolume: BigDecimal = 50

  private val productPath: ProductPath = ProductPath(path = "tobacco/chewing-tobacco")

  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "chewing-tobacco",
    name = "label.tobacco.chewing-tobacco",
    rateID = "TOB/A1/OTHER",
    templateId = "tobacco",
    applicableLimits = List("L-LOOSE")
  )

  private val currency: Currency = Currency(
    code = "EUR",
    displayName = "title.euro_eur",
    valueForConversion = Some("EUR"),
    currencySynonyms = List("Europe", "European")
  )

  private val country: Country = Country(
    code = "FR",
    countryName = "title.france",
    alphaTwoCode = "FR",
    isEu = true,
    isCountry = true,
    countrySynonyms = Nil
  )

  private val purchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
    path = productPath,
    iid = "iid0",
    weightOrVolume = Some(weightOrVolume),
    noOfSticks = None,
    country = Some(country)
  )

  private val exchangeRate: ExchangeRate = ExchangeRate(
    rate = "1.20",
    date = "2023-05-06"
  )

  private val model: PurchasedItem = PurchasedItem(
    purchasedProductInstance = purchasedProductInstance,
    productTreeLeaf = productTreeLeaf,
    currency = currency,
    gbpCost = 100.00,
    exchangeRate = exchangeRate
  )

  "PurchasedItem" when {
    ".countryCode" should {
      "produce the expected countryCode" in {
        model.countryCode shouldBe Some("FR")
      }
    }
  }
}
