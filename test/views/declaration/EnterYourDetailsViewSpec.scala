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

import models.{EnterYourDetailsDto, PortsOfArrival}
import java.time.LocalDateTime
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.declaration.enter_your_details

class EnterYourDetailsViewSpec extends BaseViewSpec {

  private val declarationTime: LocalDateTime = LocalDateTime.parse("2023-05-06T21:15:00.000")

  private val emptyForm: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "",
        "lastName"                                -> "",
        "identification.identificationType"       -> "",
        "identification.identificationNumber"     -> "",
        "emailAddress.email"                      -> "",
        "emailAddress.confirmEmail"               -> "",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "",
        "dateTimeOfArrival.dateOfArrival.day"     -> "",
        "dateTimeOfArrival.dateOfArrival.month"   -> "",
        "dateTimeOfArrival.dateOfArrival.year"    -> "",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "",
        "dateTimeOfArrival.timeOfArrival.halfday" -> ""
      )
    )

  private val formWithInvalidDate: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "passport",
        "identification.identificationNumber"     -> "SX12345",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "DD",
        "dateTimeOfArrival.dateOfArrival.month"   -> "AA",
        "dateTimeOfArrival.dateOfArrival.year"    -> "SS",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidTime: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "passport",
        "identification.identificationNumber"     -> "SX12345",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "SS",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "ZZ",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "PP"
      )
    )

  private val formWithInvalidTelephone: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "AAA-DDD-SSS",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidPhoneMaxLength: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "12345678901234567890123456789012345678901234567890",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidYearLength: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023123",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidEmailMaxLength: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> "blaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidConfirmEmailMaxLength: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketylerblaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidEmailLength: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> "",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidConfirmEmailLength: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidEmailDontMatch: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> "blaketyler2@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler3@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidEmailDontMatchPattern: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> ".123.]p[[;'@gmail.com",
        "emailAddress.confirmEmail"               -> ".123.]p[[;'@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidFirstName: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "+_)(*&#$%^&",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val formWithInvalidLastName: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "+_)(*&#$%^&",
        "identification.identificationType"       -> "telephone",
        "identification.identificationNumber"     -> "1234567890",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val amountOfDaysInFuture                = 5
  private val dateInFutureFiveDays: LocalDateTime = LocalDateTime.now().plusDays(amountOfDaysInFuture)

  private val formWithInvalidDateInFuture: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "passport",
        "identification.identificationNumber"     -> "SX12345",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> dateInFutureFiveDays.getDayOfMonth.toString,
        "dateTimeOfArrival.dateOfArrival.month"   -> dateInFutureFiveDays.getMonthValue.toString,
        "dateTimeOfArrival.dateOfArrival.year"    -> dateInFutureFiveDays.getYear.toString,
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val validForm: Form[EnterYourDetailsDto] = EnterYourDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "firstName"                               -> "Blake",
        "lastName"                                -> "Tyler",
        "identification.identificationType"       -> "passport",
        "identification.identificationNumber"     -> "SX12345",
        "emailAddress.email"                      -> "blaketyler@gmail.com",
        "emailAddress.confirmEmail"               -> "blaketyler@gmail.com",
        "placeOfArrival.selectPlaceOfArrival"     -> "",
        "placeOfArrival.enterPlaceOfArrival"      -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"     -> "06",
        "dateTimeOfArrival.dateOfArrival.month"   -> "05",
        "dateTimeOfArrival.dateOfArrival.year"    -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"    -> "09",
        "dateTimeOfArrival.timeOfArrival.minute"  -> "15",
        "dateTimeOfArrival.timeOfArrival.halfday" -> "pm"
      )
    )

  private val portsOfArrival: List[PortsOfArrival] = List(
    PortsOfArrival(
      code = "NCL",
      displayName = "title.newcastle_airport",
      isGB = true,
      portSynonyms = List("Newcastle International Airport", "NCL")
    )
  )

  val viewViaApply: HtmlFormat.Appendable = injected[enter_your_details].apply(
    form = validForm,
    portsOfArrival = portsOfArrival,
    journeyStart = None,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[enter_your_details].render(
    validForm,
    portsOfArrival,
    None,
    None,
    request,
    messages,
    appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[enter_your_details].f(
    validForm,
    portsOfArrival,
    None,
    None
  )(request, messages, appConfig)

  private def buildView(form: Form[EnterYourDetailsDto]): HtmlFormat.Appendable =
    injected[enter_your_details].apply(
      form = form,
      portsOfArrival = portsOfArrival,
      journeyStart = None,
      backLink = None
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val expectedEmptyFormErrors: Seq[(String, String)] = List(
    "#firstName"                            -> messages("error.required.first_name"),
    "#lastName"                             -> messages("error.required.last_name"),
    "#identification.identificationType"    -> messages("error.identification_type"),
    "#identification.identificationNumber"  -> messages("error.required.identification_number"),
    "#placeOfArrival.selectPlaceOfArrival"  -> messages("error.required.place_of_arrival"),
    "#dateTimeOfArrival.dateOfArrival.day"  -> messages("error.enter_a_date"),
    "#dateTimeOfArrival.timeOfArrival.hour" -> messages("error.enter_a_time")
  )

  val expectedInvalidDateFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.dateOfArrival.day" -> messages("error.enter_a_real_date")
  )

  val expectedInvalidTimeFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.timeOfArrival.hour" -> messages("error.enter_a_real_time")
  )

  val expectedInvalidTelephoneFormErrors: Seq[(String, String)] = List(
    "#identification" -> messages("error.telephone_number.format")
  )

  val expectedInvalidFutureDateFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.dateOfArrival.day" -> messages("error.5_days")
  )

  val expectedInvalidPhoneMaxLengthFormErrors: Seq[(String, String)] = List(
    "#identification.identificationNumber" -> messages("error.max-length.identification_number")
  )

  val expectedInvalidYearLengthFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.dateOfArrival.day" -> messages("error.year_length")
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

  val expectedInvalidFirstNameFormErrors: Seq[(String, String)] = List(
    "#firstName" -> messages("error.first_name.valid")
  )

  val expectedInvalidLastNameFormErrors: Seq[(String, String)] = List(
    "#lastName" -> messages("error.last_name.valid")
  )

  val invalidTestCases: Seq[(String, Form[EnterYourDetailsDto], Seq[(String, String)])] = Seq(
    Tuple3("Empty form", emptyForm, expectedEmptyFormErrors),
    Tuple3("Invalid date form", formWithInvalidDate, expectedInvalidDateFormErrors),
    Tuple3("Invalid time form", formWithInvalidTime, expectedInvalidTimeFormErrors),
    Tuple3("Invalid telephone form", formWithInvalidTelephone, expectedInvalidTelephoneFormErrors),
    Tuple3("Invalid date in future form", formWithInvalidDateInFuture, expectedInvalidFutureDateFormErrors),
    Tuple3("Invalid telephone max length form", formWithInvalidPhoneMaxLength, expectedInvalidPhoneMaxLengthFormErrors),
    Tuple3("Invalid year length form", formWithInvalidYearLength, expectedInvalidYearLengthFormErrors),
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
    ),
    Tuple3("Invalid first name form", formWithInvalidFirstName, expectedInvalidFirstNameFormErrors),
    Tuple3("Invalid last name form", formWithInvalidLastName, expectedInvalidLastNameFormErrors)
  )

  "EnterYourDetailsView" when {
    renderViewTest(
      title = "Enter your details - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Enter your details"
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
