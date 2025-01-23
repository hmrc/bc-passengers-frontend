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

import play.api.libs.json.*
import _root_.util.BaseSpec

class ProductPathSpec extends BaseSpec {

  private val model: ProductPath  = ProductPath(components = List("tobacco", "chewing-tobacco"))
  private val jsonString: JsValue = JsString(model.toString)

  "ProductPath" when {
    "read from valid JSON" should {
      "produce the expected ProductPath model" in {
        jsonString.as[ProductPath] shouldBe model
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        JsNull.validate[ProductPath].map(_ shouldBe JsError("Invalid ProductPath json"))
      }
    }

    "written to JSON" should {
      "produce the expected JSON" in {
        Json.toJson(model) shouldBe jsonString
      }
    }
  }
}
