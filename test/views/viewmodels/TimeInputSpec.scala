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

package views.viewmodels

import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.Comet.json
import play.api.libs.json.{JsError, JsSuccess, Json}
import util.BaseSpec

class TimeInputSpec extends BaseSpec with TimeInputConstants {

  "TimeInput" when {

    ".defaultObject" should {

      "return default empty model" in {

        TimeInput.defaultObject shouldBe modelEmptyFields
      }
    }

    "Json Reads" should {

      "produce the expected TimeInput model" when {

        "all mandatory defaulted fields are specified" in {

          jsonMandatoryFields.as[TimeInput] shouldBe modelMandatoryFields
        }

        "all mandatory defaulted fields are empty" in {

          jsonEmptyFields.as[TimeInput] shouldBe modelEmptyFields
        }
      }
    }

    "Json Writes" when {

      "given the mandatory fields are empty" should {

        "produce the expected JSON" in {

          Json.toJson(modelEmptyFields) shouldBe jsonEmptyFields
        }
      }

      "given all the mandatory fields are filled" should {

        "produce the expected JSON" in {

          Json.toJson(modelMandatoryFields) shouldBe jsonMandatoryFields
        }
      }
    }

    "TimeInput" should {
      "serialize to JSON" when {
        "all fields are valid" in {
          Json.toJson(modelMandatoryFields) shouldBe jsonMandatoryFields
        }

        "fields are empty" in {
          Json.toJson(modelEmptyFields) shouldBe jsonEmptyFields
        }
      }

      "deserialize from JSON" when {
        "all fields are valid" in {
          jsonMandatoryFields.validate[TimeInput] shouldBe JsSuccess(modelMandatoryFields)
        }

        "fields are empty" in {
          jsonEmptyFields.validate[TimeInput] shouldBe JsSuccess(modelEmptyFields)
        }

        "an empty JSON object" in {
          val json = Json.obj()
          json.validate[TimeInput] shouldBe JsSuccess(
            TimeInput("", None, List(), List(), None, None, "", None, "", Map(), true)
          )
        }
      }

    }

  }
}
