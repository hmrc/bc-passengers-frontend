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

import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import util.BaseSpec

class ProductListEntrySpec extends BaseSpec {

  private val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private val messagesApi: MessagesApi                 = injected[MessagesApi]
  given messages: Messages                             = messagesApi.preferred(request)

  private val json: JsValue = Json.parse(
    """
      |{
      |    "code": "alcohol/wine",
      |    "displayName": "Wine"
      |}
    """.stripMargin
  )

  private val productPath: ProductPath = ProductPath(path = "alcohol/wine")

  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "wine",
    name = "label.alcohol.wine",
    rateID = "ALC/A3/WINE",
    templateId = "alcohol",
    applicableLimits = List("L-WINE")
  )

  private val model: ProductListEntry = ProductListEntry(
    productPath = productPath,
    productTreeLeaf = productTreeLeaf
  )

  "ProductListEntry" when {
    ".toAutoCompleteJson" should {
      "produce the expected JSON" in {
        model.toAutoCompleteJson(messages) shouldBe json
      }
    }
  }
}
