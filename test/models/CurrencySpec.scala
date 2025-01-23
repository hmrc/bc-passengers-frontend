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

import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import util.BaseSpec

class CurrencySpec extends BaseSpec {
  given messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  given messages: Messages       = messagesApi.preferred(Seq(Lang("en"), Lang("cy")))

  private val json: JsValue   = Json.parse(
    """
      |{
      |    "code": "USD",
      |    "displayName": "title.usa_dollars_usd",
      |    "valueForConversion": "USD",
      |    "currencySynonyms": ["USD", "USA", "US", "United States of America", "American"]
      |}
        """.stripMargin
  )
  private val model: Currency = Currency(
    "USD",
    "title.usa_dollars_usd",
    Some("USD"),
    List("USD", "USA", "US", "United States of America", "American")
  )

  "Currency" when {
    "read from valid JSON" should {
      "produce the expected Currency model" in {
        json.as[Currency] shouldBe model
      }
    }

    "written to JSON" should {
      "produce the expected JSON" in {
        Json.toJson(model) shouldBe json
      }
    }

    ".toAutoCompleteJson" should {
      "create json for autocomplete fields" in {
        model.toAutoCompleteJson(messages) shouldBe Json.obj(
          "code"        -> "USD",
          "displayName" -> Messages("title.usa_dollars_usd"),
          "synonyms"    -> List("USD", "USA", "US", "United States of America", "American")
        )
      }
    }
  }
}
