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

package views.viewmodels

import play.api.libs.json.{JsObject, JsValue, Json}
import util.BaseSpec

class TimeInputSpec extends BaseSpec {

  private val json: JsValue = Json.parse(
    """
      |{
      |    "id": "",
      |    "items": [],
      |    "periodSelectItems": [],
      |    "formGroup": {
      |        "classes": ""
      |    },
      |    "classes": "",
      |    "attributes": {
      |        "id": "someId"
      |    },
      |    "showSelectPeriod": true
      |}
    """.stripMargin
  )

  private val model: TimeInput = TimeInput(attributes = Map("id" -> "someId"))

  "TimeInput" when {
    ".defaultObject" should {
      "return default model" in {
        TimeInput.defaultObject shouldBe model.copy(attributes = Map.empty)
      }
    }

    "read from valid JSON" should {
      "produce the expected TimeInput model" when {
        "all mandatory defaulted fields are specified" in {
          json.as[TimeInput] shouldBe model
        }

        "all mandatory defaulted fields are not specified" in {
          val json: JsValue = JsObject.empty

          json.as[TimeInput] shouldBe model.copy(attributes = Map.empty)
        }
      }
    }

    "written to JSON" should {
      "produce the expected JSON" in {
        Json.toJson(model) shouldBe json
      }
    }
  }
}
