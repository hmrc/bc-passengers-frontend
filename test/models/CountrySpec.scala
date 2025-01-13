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

import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import util.BaseSpec

class CountrySpec extends BaseSpec {
  given messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  given messages: Messages       = messagesApi.preferred(Seq(Lang("en"), Lang("cy")))

  private val json: JsValue  = Json.parse(
    """
      |{
      |    "code": "GB",
      |    "countryName": "title.united_kingdom",
      |    "alphaTwoCode": "GB",
      |    "isEu": false,
      |    "isCountry": true,
      |    "countrySynonyms": ["England", "Scotland", "Wales", "Northern Ireland", "GB", "UK"]
      |}
        """.stripMargin
  )
  private val model: Country = Country(
    "GB",
    "title.united_kingdom",
    "GB",
    isEu = false,
    isCountry = true,
    List("England", "Scotland", "Wales", "Northern Ireland", "GB", "UK")
  )

  "Country" when {
    "read from valid JSON" should {
      "produce the expected Country model" in {
        json.as[Country] shouldBe model
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
          "code"        -> "GB",
          "displayName" -> Messages("title.united_kingdom"),
          "synonyms"    -> List("England", "Scotland", "Wales", "Northern Ireland", "GB", "UK")
        )

      }
    }
  }
}
