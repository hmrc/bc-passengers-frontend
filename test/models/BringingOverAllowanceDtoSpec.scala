/*
 * Copyright 2026 HM Revenue & Customs
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

import config.AppConfig
import models.BringingOverAllowanceDto.{form, formForJourney}
import org.mockito.Mockito.{mock, when}
import play.api.data.{Form, FormError}
import util.BaseSpec

class BringingOverAllowanceDtoSpec extends BaseSpec {

  val mockAppConfig: AppConfig = mock(classOf[AppConfig])

  private val validatedForm: Form[BringingOverAllowanceDto] = form.bind(
    Map(
      "bringingOverAllowance" -> "true"
    )
  )

  "BringingOverAllowanceDto" should {
    "return no errors with valid data" in {
      val errors: Seq[FormError] = validatedForm.errors

      errors shouldBe empty
    }

    "return error with empty data isVapingJourneyEnabled toggle is off" in {
      val validatedForm: Form[BringingOverAllowanceDto] = form.bind(
        Map(
          "bringingOverAllowance" -> ""
        )
      )
      val errors: Seq[FormError]                        = validatedForm.errors

      errors shouldBe List(FormError("bringingOverAllowance", List("error.bringing_over_allowance")))
    }

    "return error with empty data isVapingJourneyEnabled toggle is on" in {
      when(mockAppConfig.isVapingJourneyEnabled).thenReturn(true)
      val validatedForm: Form[BringingOverAllowanceDto] = formForJourney(mockAppConfig.isVapingJourneyEnabled).bind(
        Map(
          "bringingOverAllowance" -> ""
        )
      )
      val errors: Seq[FormError]                        = validatedForm.errors

      errors shouldBe List(FormError("bringingOverAllowance", List("error.bringing_over_allowance_ni")))
    }

    "return the correct result when filled" in {
      form.fill(BringingOverAllowanceDto(true)) shouldBe validatedForm
    }
  }
}
