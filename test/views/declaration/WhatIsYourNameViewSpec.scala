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

package views.declaration

import models.WhatIsYourNameDto
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.declaration.what_is_your_name

class WhatIsYourNameViewSpec extends BaseViewSpec {

  private val emptyForm: Form[WhatIsYourNameDto] = WhatIsYourNameDto.form
    .bind(
      Map(
        "firstName" -> "",
        "lastName"  -> ""
      )
    )

  private val formWithInvalidFirstName: Form[WhatIsYourNameDto] = WhatIsYourNameDto.form
    .bind(
      Map(
        "firstName" -> "+_)(*&#$%^&",
        "lastName"  -> "Tyler"
      )
    )

  private val formWithInvalidLastName: Form[WhatIsYourNameDto] = WhatIsYourNameDto.form
    .bind(
      Map(
        "firstName" -> "Blake",
        "lastName"  -> "+_)(*&#$%^&"
      )
    )

  private val validForm: Form[WhatIsYourNameDto] = WhatIsYourNameDto.form
    .bind(
      Map(
        "firstName" -> "Blake",
        "lastName"  -> "Tyler"
      )
    )

  val viewViaApply: HtmlFormat.Appendable = injected[what_is_your_name].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[what_is_your_name].render(
    validForm,
    None,
    request,
    messages,
    appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[what_is_your_name].ref.f(
    validForm,
    None
  )(request, messages, appConfig)

  private def buildView(form: Form[WhatIsYourNameDto]): HtmlFormat.Appendable =
    injected[what_is_your_name].apply(
      form = form,
      backLink = None
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val expectedEmptyFormErrors: Seq[(String, String)] = List(
    "#firstName" -> messages("error.required.first_name"),
    "#lastName"  -> messages("error.required.last_name")
  )

  val expectedInvalidFirstNameFormErrors: Seq[(String, String)] = List(
    "#firstName" -> messages("error.first_name.valid")
  )

  val expectedInvalidLastNameFormErrors: Seq[(String, String)] = List(
    "#lastName" -> messages("error.last_name.valid")
  )

  val invalidTestCases: Seq[(String, Form[WhatIsYourNameDto], Seq[(String, String)])] = Seq(
    Tuple3("Empty form", emptyForm, expectedEmptyFormErrors),
    Tuple3("Invalid first name form", formWithInvalidFirstName, expectedInvalidFirstNameFormErrors),
    Tuple3("Invalid last name form", formWithInvalidLastName, expectedInvalidLastNameFormErrors)
  )

  "WhatIsYourNameView" when {
    renderViewTest(
      title = "What is your name? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "What is your name?"
    )

    "formWithErrors" should {

      invalidTestCases.foreach { testCase =>
        s"have error prefix in title for ${testCase._1}" in {
          val doc = document(buildView(form = testCase._2))
          doc.title() should startWith(messages("label.error"))
        }

        s"have all info in error summary for ${testCase._1}" in {
          val doc = document(buildView(form = testCase._2))

          doc.title()                            should startWith(messages("label.error"))
          messages("label.there_is_a_problem") shouldBe getErrorTitle(doc)

          testCase._3 shouldBe getErrorsInSummary(doc)
        }

        s"have all errors in each input for ${testCase._1}" in {
          val doc = document(buildView(form = testCase._2))
          doc.title()                                                          should startWith(messages("label.error"))
          messages("label.there_is_a_problem")                               shouldBe getErrorTitle(doc)
          testCase._3.map(error => messages("label.error") + " " + error._2) shouldBe getErrorsInFieldSet(
            doc
          )
        }
      }
    }
  }
}
