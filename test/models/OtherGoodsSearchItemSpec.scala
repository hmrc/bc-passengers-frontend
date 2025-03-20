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

import play.api.libs.json.{JsError, JsSuccess, Json}
import util.BaseSpec

class OtherGoodsSearchItemSpec extends BaseSpec {
  "Declaration Response" should {

    val otherGoodsSearchItem =
      OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear"))

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(otherGoodsSearchItem) shouldBe Json.obj(
          "name" -> "label.other-goods.mans_shoes",
          "path" -> ProductPath("other-goods/adult/adult-footwear")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "name" -> "label.other-goods.mans_shoes",
          "path" -> ProductPath("other-goods/adult/adult-footwear")
        )
        json.validate[OtherGoodsSearchItem] shouldBe JsSuccess(otherGoodsSearchItem)
      }

      "fields are empty" in {
        val json = Json.obj()
        json.validate[OtherGoodsSearchItem] shouldBe a[JsError]
      }

      "invalid field types" in {
        val json = Json.obj(
          "name" -> "label.other-goods.mans_shoes",
          "path" -> 0
        )
        json.validate[OtherGoodsSearchItem] shouldBe a[JsError]
      }
    }
  }
}
