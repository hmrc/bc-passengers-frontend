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

import models.AgeOver17Dto.form
import play.api.data.{Form, FormError}
import util.BaseSpec

class AgeOver17DtoSpec extends BaseSpec {

  private val validatedForm: Form[AgeOver17Dto] = form.bind(
    Map(
      "ageOver17" -> "true"
    )
  )

  "AgeOver17Dto" should {
    "return no errors with valid data" in {
      val errors: Seq[FormError] = validatedForm.errors

      errors shouldBe empty
    }

    "return error with empty data" in {
      val validatedForm: Form[AgeOver17Dto] = form.bind(
        Map(
          "ageOver17" -> ""
        )
      )
      val errors: Seq[FormError]            = validatedForm.errors

      errors shouldBe List(FormError("ageOver17", List("error.over_17")))
    }

    "return the correct result when filled" in {
      form.fill(AgeOver17Dto(true)) shouldBe validatedForm
    }
  }
}
