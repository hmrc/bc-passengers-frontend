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

package views.travel_details

import models.BringingDutyFreeDto
import models.BringingDutyFreeDto.form
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.travel_details.bringing_duty_free_question

class BringingDutyFreeQuestionViewSpec extends BaseViewSpec {

  private val validForm: Form[BringingDutyFreeDto] = form.bind(Map("isBringingDutyFree" -> "true"))

  val viewViaApply: HtmlFormat.Appendable = injected[bringing_duty_free_question].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[bringing_duty_free_question].render(
    form = validForm,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[bringing_duty_free_question].f(
    validForm,
    None
  )(request, messages, appConfig)

  "BringingDutyFreeQuestionView" when {
    renderViewTest(
      title = "Are you bringing in duty-free alcohol or tobacco bought in UK or EU shops?",
      heading = "Are you bringing in duty-free alcohol or tobacco bought in UK or EU shops?"
    )
  }
}
