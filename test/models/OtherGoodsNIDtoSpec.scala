/*
 * Copyright 2026 HM Revenue & Customs
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

class OtherGoodsNIDtoSpec extends BaseSpec {

  private val productPath: ProductPath = ProductPath(path = "other-ni-goods/antiques")

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
    country = Some(country),
    originCountry = Some(country),
    currency = Some("EUR"),
    cost = Some(100.00),
    weightOrVolume = Some(0),
    isVatPaid = Some(false),
    isExcisePaid = Some(false),
    isCustomPaid = Some(false),
    isUccRelief = Some(false),
    hasEvidence = Some(false)
  )

  private val model: OtherGoodsNIDto = OtherGoodsNIDto(
    weightOrVolume = 0,
    country = "FR",
    originCountry = Some("FR"),
    currency = "EUR",
    cost = 100.00,
    isVatPaid = Some(false),
    isUccRelief = Some(false),
    isExcisePaid = Some(false),
    isCustomPaid = Some(false),
    hasEvidence = Some(false)
  )

  "OtherGoodsNIDto" when {
    ".fromPurchasedProductInstance" should {
      "produce the expected OtherGoodsNIDto model" in {
        OtherGoodsNIDto.fromPurchasedProductInstance(purchasedProductInstance) shouldBe Some(model)
      }
    }
  }
}
