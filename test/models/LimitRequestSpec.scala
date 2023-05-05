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
      |            "rateId": "TOB/A1/OTHER",
      |            "weightOrVolume": 500,
      |            "noOfUnits": 1000,
      |            "metadata": {}
      |        }
      |    ]
      |}
    """.stripMargin
  )

  private val (weightOrVolume, noOfSticks): (Int, Int) = (500, 1000)

  private val productPath: ProductPath = ProductPath(path = "tobacco/chewing-tobacco")

  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "chewing-tobacco",
    name = "label.tobacco.chewing-tobacco",
    rateID = "TOB/A1/OTHER",
    templateId = "tobacco",
    applicableLimits = List("L-LOOSE")
  )

  private val items = SpeculativeItem(
    purchasedProductInstance = PurchasedProductInstance(
      path = productPath,
      iid = "iid0",
      weightOrVolume = Some(weightOrVolume),
      noOfSticks = Some(noOfSticks),
      country = Some(
        Country(
          code = "FR",
          countryName = "title.france",
          alphaTwoCode = "FR",
          isEu = true,
          isCountry = true,
          countrySynonyms = Nil
        )
      )
    ),
    productTreeLeaf = productTreeLeaf,
    gbpCost = 100.00
  )

  private val model: LimitRequest = LimitRequest(
    isPrivateCraft = false,
    isAgeOver17 = true,
    isArrivingNI = true,
    items = List(items)
  )

  "LimitRequest" when {
    "written to JSON" should {
      "produce the expected JSON" in {
        Json.toJson(model) shouldBe json
      }
    }
  }
}
