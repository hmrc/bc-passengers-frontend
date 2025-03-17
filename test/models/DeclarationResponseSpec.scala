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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class DeclarationResponseSpec extends AnyWordSpec with Matchers {

  "Declaration Response" should {

    val calculation: Calculation                 = Calculation("0.00", "12.50", "102.50", "115.00")
    val liabilityDetails: LiabilityDetails       = LiabilityDetails("0.00", "12.50", "102.50", "115.00")
    val productPath: ProductPath                 = ProductPath("other-goods/adult/adult-footwear")
    val otherGoodsSearchItem                     =
      OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))
    val country: Country                         = Country("IN", "title.india", "IN", isEu = false, isCountry = true, List())
    val purchasedProductInstances                = List(
      PurchasedProductInstance(
        productPath,
        "UnOGll",
        None,
        None,
        Some(country),
        None,
        Some("GBP"),
        Some(500),
        Some(otherGoodsSearchItem),
        Some(false),
        Some(false),
        None,
        Some(false),
        None,
        isEditable = Some(false)
      )
    )
    val declarationResponse: DeclarationResponse = DeclarationResponse(
      calculation = calculation,
      liabilityDetails = liabilityDetails,
      oldPurchaseProductInstances = purchasedProductInstances,
      amendmentCount = Some(0)
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(declarationResponse) shouldBe Json.obj(
          "calculation"                 -> calculation,
          "liabilityDetails"            -> liabilityDetails,
          "oldPurchaseProductInstances" -> purchasedProductInstances,
          "amendmentCount"              -> Some(0)
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "calculation"                 -> calculation,
          "liabilityDetails"            -> liabilityDetails,
          "oldPurchaseProductInstances" -> purchasedProductInstances
        )
        json.validate[DeclarationResponse] shouldBe JsSuccess(declarationResponse)
      }

      "fail to read from json" when {
        "there is type mismatch" in {
          Json.arr("a" -> "b").validate[DeclarationResponse] shouldBe a[JsError]
        }
        "empty json" in {
          Json.obj().validate[DeclarationResponse] shouldBe a[JsError]
        }
      }
    }
  }

  "Liability Details" should {

    val liabilityDetails: LiabilityDetails = LiabilityDetails(
      totalExciseGBP = "0.00",
      totalCustomsGBP = "0.00",
      totalVATGBP = "0.00",
      grandTotalGBP = "0.00"
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(liabilityDetails) shouldBe Json.obj(
          "totalExciseGBP"  -> "0.00",
          "totalCustomsGBP" -> "0.00",
          "totalVATGBP"     -> "0.00",
          "grandTotalGBP"   -> "0.00"
        )
      }
    }

    "deserialize from JSON" when {

      "all fields are valid" in {
        val json = Json.obj(
          "totalExciseGBP"  -> "0.00",
          "totalCustomsGBP" -> "0.00",
          "totalVATGBP"     -> "0.00",
          "grandTotalGBP"   -> "0.00"
        )
        json.validate[LiabilityDetails] shouldBe JsSuccess(liabilityDetails)
      }

      "fail to read from json" when {
        "there is type mismatch" in {
          Json.arr("a" -> "b").validate[LiabilityDetails] shouldBe a[JsError]
        }

        "empty json" in {
          Json.obj().validate[LiabilityDetails] shouldBe a[JsError]
        }
      }
    }
  }
}
