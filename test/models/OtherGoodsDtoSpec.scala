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

class OtherGoodsDtoSpec extends BaseSpec {

  private val productPath: ProductPath = ProductPath(path = "other-goods/antiques")

  private val purchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
    path = productPath,
    iid = "iid0",
    country = Some(
      Country(
        code = "FR",
        countryName = "title.france",
        alphaTwoCode = "FR",
        isEu = true,
        isCountry = true,
        countrySynonyms = Nil
      )
    ),
    currency = Some("EUR"),
    cost = Some(100.00),
    searchTerm = Some(
      OtherGoodsSearchItem(
        name = "label.other-goods.antiques",
        path = productPath
      )
    ),
    isVatPaid = Some(false),
    isCustomPaid = Some(false),
    isUccRelief = Some(false),
    hasEvidence = Some(false)
  )

  private val model: OtherGoodsDto = OtherGoodsDto(
    searchTerm = Some(
      OtherGoodsSearchItem(
        name = "label.other-goods.antiques",
        path = productPath
      )
    ),
    country = "FR",
    originCountry = None,
    currency = "EUR",
    cost = 100.00,
    isVatPaid = Some(false),
    isUccRelief = Some(false),
    isCustomPaid = Some(false),
    hasEvidence = Some(false)
  )

  "OtherGoodsDto" when {
    ".fromPurchasedProductInstance" should {
      "produce the expected OtherGoodsDto model" in {
        OtherGoodsDto.fromPurchasedProductInstance(purchasedProductInstance) shouldBe Some(model)
      }
    }
  }
}
