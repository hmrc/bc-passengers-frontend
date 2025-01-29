/*
 * Copyright 2024 HM Revenue & Customs
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

class TobaccoDtoSpec extends BaseSpec {

  private val weightOrVolume: BigDecimal = 50

  private val productPath: ProductPath = ProductPath(path = "tobacco/chewing-tobacco")

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
    country = Some(country),
    originCountry = Some(country),
    currency = Some("EUR"),
    cost = Some(100.00),
    isVatPaid = Some(false),
    isCustomPaid = Some(false),
    isExcisePaid = Some(false),
    hasEvidence = Some(false)
  )

  private val model: TobaccoDto = TobaccoDto(
    noOfSticks = None,
    weightOrVolume = Some(weightOrVolume),
    country = "FR",
    originCountry = Some("FR"),
    currency = "EUR",
    cost = 100.00,
    isVatPaid = Some(false),
    isExcisePaid = Some(false),
    isCustomPaid = Some(false),
    hasEvidence = Some(false)
  )

  "TobaccoDto" when {
    ".fromPurchasedProductInstance" should {
      "produce the expected TobaccoDto model" in {
        TobaccoDto.fromPurchasedProductInstance(purchasedProductInstance) shouldBe Some(model)
      }
    }
  }
}
