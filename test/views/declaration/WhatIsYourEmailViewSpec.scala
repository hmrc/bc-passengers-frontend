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

package views.declaration

import models.EmailAddressDto
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.declaration.what_is_your_email

class WhatIsYourEmailViewSpec extends BaseViewSpec {

  private val emptyForm: Form[EmailAddressDto] = EmailAddressDto.form
    .bind(
      Map(
        "emailAddress.email"        -> "",
        "emailAddress.confirmEmail" -> ""
      )
    )

  private val formWithInvalidEmailMaxLength: Form[EmailAddressDto] = EmailAddressDto.form
    .bind(
      Map(
        "emailAddress.email"        -> "blaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketyler@gmail.com",
        "emailAddress.confirmEmail" -> "blaketyler@gmail.com"
      )
    )

  private val formWithInvalidConfirmEmailMaxLength: Form[EmailAddressDto] = EmailAddressDto.form
    .bind(
      Map(
        "emailAddress.email"        -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail" -> "blaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketyler@gmail.com"
      )
    )

  private val formWithInvalidEmailLength: Form[EmailAddressDto] = EmailAddressDto.form
    .bind(
      Map(
        "emailAddress.email"        -> "",
        "emailAddress.confirmEmail" -> "blaketyler@gmail.com"
      )
    )

  private val formWithInvalidConfirmEmailLength: Form[EmailAddressDto] = EmailAddressDto.form
    .bind(
      Map(
        "emailAddress.email"        -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail" -> ""
      )
    )

  private val formWithInvalidEmailDontMatch: Form[EmailAddressDto] = EmailAddressDto.form
    .bind(
      Map(
        "emailAddress.email"        -> "blaketyler2@gmail.com",
        "emailAddress.confirmEmail" -> "blaketyler3@gmail.com"
      )
    )

  private val formWithInvalidEmailDontMatchPattern: Form[EmailAddressDto] = EmailAddressDto.form
    .bind(
      Map(
        "emailAddress.email"        -> ".123.]p[[;'@gmail.com",
        "emailAddress.confirmEmail" -> ".123.]p[[;'@gmail.com"
      )
    )

  private val validForm: Form[EmailAddressDto] = EmailAddressDto.form
    .bind(
      Map(
        "emailAddress.email"        -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail" -> "blaketyler@gmail.com"
      )
    )

  val viewViaApply: HtmlFormat.Appendable = injected[what_is_your_email].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[what_is_your_email].render(
    validForm,
    None,
    request,
    messages,
    appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[what_is_your_email].f(
    validForm,
    None
  )(request, messages, appConfig)

  private def buildView(form: Form[EmailAddressDto]): HtmlFormat.Appendable =
    injected[what_is_your_email].apply(
      form = form,
      backLink = None
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val expectedEmptyFormErrors: Seq[(String, String)] = List(
    "#emailAddress.email"        -> messages("error.required.emailAddress.email"),
    "#emailAddress.confirmEmail" -> messages("error.required.emailAddress.confirmEmail")
  )

  val expectedInvalidEmailMaxLengthFormErrors: Seq[(String, String)] = List(
    "#emailAddress.email" -> messages("error.max-length.email")
  )

  val expectedInvalidConfirmEmailMaxLengthFormErrors: Seq[(String, String)] = List(
    "#emailAddress.confirmEmail" -> messages("error.max-length.email")
  )

  val expectedInvalidEmailFormErrors: Seq[(String, String)] = List(
    "#emailAddress" -> messages("error.required.emailAddress.email")
  )

  val expectedInvalidConfirmEmailFormErrors: Seq[(String, String)] = List(
    "#emailAddress" -> messages("error.required.emailAddress.confirmEmail")
  )

  val expectedInvalidEmailDontMatchFormErrors: Seq[(String, String)] = List(
    "#emailAddress" -> messages("error.required.emailAddress.no_match")
  )

  val expectedInvalidEmailDontMatchPatternFormErrors: Seq[(String, String)] = List(
    "#emailAddress" -> messages("error.format.emailAddress")
  )

  val invalidTestCases: Seq[(String, Form[EmailAddressDto], Seq[(String, String)])] = Seq(
    Tuple3("Empty form", emptyForm, expectedEmptyFormErrors),
    Tuple3("Invalid email max length form", formWithInvalidEmailMaxLength, expectedInvalidEmailMaxLengthFormErrors),
    Tuple3(
      "Invalid confirm email max length form",
      formWithInvalidConfirmEmailMaxLength,
      expectedInvalidConfirmEmailMaxLengthFormErrors
    ),
    Tuple3("Invalid email form", formWithInvalidEmailLength, expectedInvalidEmailFormErrors),
    Tuple3("Invalid confirm email form", formWithInvalidConfirmEmailLength, expectedInvalidConfirmEmailFormErrors),
    Tuple3("Invalid email dont match form", formWithInvalidEmailDontMatch, expectedInvalidEmailDontMatchFormErrors),
    Tuple3(
      "Invalid email dont match pattern form",
      formWithInvalidEmailDontMatchPattern,
      expectedInvalidEmailDontMatchPatternFormErrors
    )
  )

  "WhatIsYourEmailView" when {
    renderViewTest(
      title = "What is your email address? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "What is your email address?"
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