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

import play.api.libs.json.{JsValue, Json}
import util.BaseSpec

class LimitRequestSpec extends BaseSpec {

  private val json: JsValue = Json.parse(
    """
      |{
      |    "isPrivateCraft": false,
      |    "isAgeOver17": true,
      |    "isArrivingNI": true,
      |    "items": [
      |        {
      |            "purchaseCost": "100.00",
      |            "rateId": "TOB/A1/CIGAR",
      |            "weightOrVolume": 50,
      |            "noOfUnits": 10,
      |            "metadata": {}
      |        }
      |    ]
      |}
    """.stripMargin
  )

  private val (weightOrVolume, noOfSticks): (BigDecimal, Int) = (BigDecimal(50), 10)

  private val productPath: ProductPath = ProductPath(path = "tobacco/cigars")

  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "cigars",
    name = "label.tobacco.cigars",
    rateID = "TOB/A1/CIGAR",
    templateId = "cigars",
    applicableLimits = List("L-CIGAR")
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
    noOfSticks = Some(noOfSticks),
    country = Some(country)
  )

  private val items: List[SpeculativeItem] = List(
    SpeculativeItem(
      purchasedProductInstance = purchasedProductInstance,
      productTreeLeaf = productTreeLeaf,
      gbpCost = 100.00
    )
  )

  private val model: LimitRequest = LimitRequest(
    isPrivateCraft = false,
    isAgeOver17 = true,
    isArrivingNI = true,
    items = items
  )

  "LimitRequest" when {
    "written to JSON" should {
      "produce the expected JSON" in {
        Json.toJson(model) shouldBe json
      }
    }
  }
}
