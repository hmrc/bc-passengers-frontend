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

import models.{IdentificationNumberDto, IdentificationTypeDto}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.declaration.{driving_licence_number, eu_id_number, passport_id_number, phone_number, type_of_identification}

class TypeOfIdentificationViewSpec extends BaseViewSpec {

  private val identificationForm: Form[IdentificationTypeDto] = IdentificationTypeDto.form
    .bind(
      Map(
        "identificationType" -> ""
      )
    )

  private val emptyForm: Form[IdentificationNumberDto] = IdentificationNumberDto
    .form("euId")
    .bind(
      Map(
        "identificationNumber" -> ""
      )
    )

  private val formWithInvalidTelephone: Form[IdentificationNumberDto] = IdentificationNumberDto
    .form("telephone")
    .bind(
      Map(
        "identificationNumber" -> "AAA-DDD-SSS"
      )
    )

  private val formWithInvalidEuIdMaxLength: Form[IdentificationNumberDto] = IdentificationNumberDto
    .form("euId")
    .bind(
      Map(
        "identificationNumber" -> "12345678901234567890123456789012345678901234567890"
      )
    )

  private val validForm: Form[IdentificationNumberDto] = IdentificationNumberDto
    .form("telephone")
    .bind(
      Map(
        "identificationNumber" -> "03888111222"
      )
    )

  val viewViaApply: HtmlFormat.Appendable = injected[phone_number].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaApplyPassport: HtmlFormat.Appendable = injected[passport_id_number].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaApplyDriving: HtmlFormat.Appendable = injected[driving_licence_number].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaApplyEuId: HtmlFormat.Appendable = injected[eu_id_number].apply(
    form = validForm,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[phone_number].render(
    validForm,
    None,
    request,
    messages,
    appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[phone_number].ref.f(
    validForm,
    None
  )(request, messages, appConfig)

  val greatBritainView: HtmlFormat.Appendable = injected[type_of_identification].apply(
    form = identificationForm,
    backLink = None,
    journeyStart = Some("greatBritain")
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  private def buildView(form: Form[IdentificationNumberDto]): HtmlFormat.Appendable =
    injected[phone_number].apply(
      form = form,
      backLink = None
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val expectedEmptyFormErrors: Seq[(String, String)] = List(
    "#identificationNumber" -> messages("error.required.euId")
  )

  val expectedInvalidTelephoneFormErrors: Seq[(String, String)] = List(
    "#identificationNumber" -> messages("error.telephone_number.format")
  )

  val expectedInvalidEuIdMaxLengthFormErrors: Seq[(String, String)] = List(
    "#identificationNumber" -> messages("error.max-length.euId")
  )

  val invalidTestCases: Seq[(String, Form[IdentificationNumberDto], Seq[(String, String)])] = Seq(
    Tuple3("Empty form", emptyForm, expectedEmptyFormErrors),
    Tuple3("Invalid telephone form", formWithInvalidTelephone, expectedInvalidTelephoneFormErrors),
    Tuple3("Invalid euId max length form", formWithInvalidEuIdMaxLength, expectedInvalidEuIdMaxLengthFormErrors)
  )

  "TypeOfIdentificationView" when {
    renderViewTest(
      title = "Your phone number - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Your phone number"
    )

    "formWithErrors" should
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
