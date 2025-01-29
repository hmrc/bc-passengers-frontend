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

import play.api.libs.json.{JsValue, Json}

trait TimeInputConstants {

  val modelMandatoryFields: TimeInput =
    TimeInput(
      id = "someId",
      namePrefix = None,
      items = Seq(),
      periodSelectItems = Seq(),
      hint = None,
      errorMessage = None,
      formGroupClasses = "aFakeFormClass",
      fieldset = None,
      classes = "aClass",
      attributes = Map("id" -> "someId"),
      showSelectPeriod = true
    )

  val jsonMandatoryFields: JsValue = Json.parse(
    """
      |{
      |    "id": "someId",
      |    "items": [],
      |    "periodSelectItems": [],
      |    "formGroupClasses": "aFakeFormClass",
      |    "classes": "aClass",
      |    "attributes": {
      |        "id": "someId"
      |    },
      |    "showSelectPeriod": true
      |}
    """.stripMargin
  )

  val modelEmptyFields: TimeInput =
    TimeInput(
      id = "",
      namePrefix = None,
      items = Seq(),
      periodSelectItems = Seq(),
      hint = None,
      errorMessage = None,
      formGroupClasses = "",
      fieldset = None,
      classes = "",
      attributes = Map(),
      showSelectPeriod = true
    )

  val jsonEmptyFields: JsValue =
    Json.parse(
      """
        |{
        |    "id": "",
        |    "items": [],
        |    "periodSelectItems": [],
        |    "formGroupClasses": "",
        |    "classes": "",
        |    "attributes": {},
        |    "showSelectPeriod": true
        |}
      """.stripMargin
    )
}
