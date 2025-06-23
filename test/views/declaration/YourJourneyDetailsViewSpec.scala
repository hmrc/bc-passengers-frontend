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

import models.{PortsOfArrival, YourJourneyDetailsDto}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.declaration.journey_details

import java.time.LocalDateTime

class YourJourneyDetailsViewSpec extends BaseViewSpec {

  private val declarationTime: LocalDateTime = LocalDateTime.now().withHour(21).withMinute(15).withSecond(0).withNano(0)

  private val emptyForm: Form[YourJourneyDetailsDto] = YourJourneyDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "placeOfArrival.selectPlaceOfArrival"    -> "",
        "placeOfArrival.enterPlaceOfArrival"     -> "",
        "dateTimeOfArrival.dateOfArrival.day"    -> "",
        "dateTimeOfArrival.dateOfArrival.month"  -> "",
        "dateTimeOfArrival.dateOfArrival.year"   -> "",
        "dateTimeOfArrival.timeOfArrival.hour"   -> "",
        "dateTimeOfArrival.timeOfArrival.minute" -> ""
      )
    )

  private val formWithInvalidDate: Form[YourJourneyDetailsDto] = YourJourneyDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "placeOfArrival.selectPlaceOfArrival"    -> "",
        "placeOfArrival.enterPlaceOfArrival"     -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"    -> "DD",
        "dateTimeOfArrival.dateOfArrival.month"  -> "11",
        "dateTimeOfArrival.dateOfArrival.year"   -> "SS",
        "dateTimeOfArrival.timeOfArrival.hour"   -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15"
      )
    )

  private val formWithInvalidTime: Form[YourJourneyDetailsDto] = YourJourneyDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "placeOfArrival.selectPlaceOfArrival"    -> "",
        "placeOfArrival.enterPlaceOfArrival"     -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"    -> "06",
        "dateTimeOfArrival.dateOfArrival.month"  -> "05",
        "dateTimeOfArrival.dateOfArrival.year"   -> "2023",
        "dateTimeOfArrival.timeOfArrival.hour"   -> "SS",
        "dateTimeOfArrival.timeOfArrival.minute" -> "ZZ"
      )
    )

  private val formWithInvalidYearLength: Form[YourJourneyDetailsDto] = YourJourneyDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "placeOfArrival.selectPlaceOfArrival"    -> "",
        "placeOfArrival.enterPlaceOfArrival"     -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"    -> "06",
        "dateTimeOfArrival.dateOfArrival.month"  -> "05",
        "dateTimeOfArrival.dateOfArrival.year"   -> "2023123",
        "dateTimeOfArrival.timeOfArrival.hour"   -> "09",
        "dateTimeOfArrival.timeOfArrival.minute" -> "15"
      )
    )

  private val amountOfDaysInFuture        = 6
  private val dateInFuture: LocalDateTime =
    LocalDateTime.now().plusDays(amountOfDaysInFuture).withHour(9).withMinute(15)
  private val today: LocalDateTime        = LocalDateTime.now().withHour(23).withMinute(1).withSecond(1)

  private val formWithInvalidDateInFuture: Form[YourJourneyDetailsDto]  = YourJourneyDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "placeOfArrival.selectPlaceOfArrival"    -> "",
        "placeOfArrival.enterPlaceOfArrival"     -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"    -> dateInFuture.getDayOfMonth.toString,
        "dateTimeOfArrival.dateOfArrival.month"  -> dateInFuture.getMonthValue.toString,
        "dateTimeOfArrival.dateOfArrival.year"   -> dateInFuture.getYear.toString,
        "dateTimeOfArrival.timeOfArrival.hour"   -> dateInFuture.getHour.toString,
        "dateTimeOfArrival.timeOfArrival.minute" -> dateInFuture.getMinute.toString
      )
    )
  private val formWithInvalidMonthInFuture: Form[YourJourneyDetailsDto] = YourJourneyDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "placeOfArrival.selectPlaceOfArrival"    -> "",
        "placeOfArrival.enterPlaceOfArrival"     -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"    -> dateInFuture.getDayOfMonth.toString,
        "dateTimeOfArrival.dateOfArrival.month"  -> "AA",
        "dateTimeOfArrival.dateOfArrival.year"   -> dateInFuture.getYear.toString,
        "dateTimeOfArrival.timeOfArrival.hour"   -> dateInFuture.getHour.toString,
        "dateTimeOfArrival.timeOfArrival.minute" -> dateInFuture.getMinute.toString
      )
    )

  private val validForm: Form[YourJourneyDetailsDto] = YourJourneyDetailsDto
    .form(declarationTime)
    .bind(
      Map(
        "placeOfArrival.selectPlaceOfArrival"    -> "",
        "placeOfArrival.enterPlaceOfArrival"     -> "Newcastle Airport",
        "dateTimeOfArrival.dateOfArrival.day"    -> today.getDayOfMonth.toString,
        "dateTimeOfArrival.dateOfArrival.month"  -> today.getMonthValue.toString,
        "dateTimeOfArrival.dateOfArrival.year"   -> today.getYear.toString,
        "dateTimeOfArrival.timeOfArrival.hour"   -> today.getHour.toString,
        "dateTimeOfArrival.timeOfArrival.minute" -> today.getMinute.toString
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

  val viewViaApply: HtmlFormat.Appendable = injected[journey_details].apply(
    form = validForm,
    portsOfArrival = portsOfArrival,
    journeyStart = None,
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[journey_details].render(
    validForm,
    portsOfArrival,
    None,
    None,
    request,
    messages,
    appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[journey_details].ref.f(
    validForm,
    portsOfArrival,
    None,
    None
  )(request, messages, appConfig)

  val greatBritainView: HtmlFormat.Appendable = injected[journey_details].apply(
    form = validForm,
    portsOfArrival = portsOfArrival,
    journeyStart = Some("greatBritain"),
    backLink = None
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  private def buildView(form: Form[YourJourneyDetailsDto]): HtmlFormat.Appendable =
    injected[journey_details].apply(
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
    "#placeOfArrival.selectPlaceOfArrival"  -> messages("error.required.place_of_arrival"),
    "#dateTimeOfArrival.dateOfArrival.day"  -> messages("error.date.enter_a_date"),
    "#dateTimeOfArrival.timeOfArrival.hour" -> messages("error.time.enter_a_time")
  )

  val expectedInvalidDateFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.dateOfArrival.day" -> messages("error.date.enter_a_real_date")
  )

  val expectedInvalidTimeFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.timeOfArrival.hour" -> messages("error.time.enter_a_real_time")
  )

  val expectedInvalidFutureDateFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.dateOfArrival.day" -> messages("error.5_days")
  )

  val expectedInvalidYearLengthFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.dateOfArrival.day" -> messages("error.date.year_length")
  )

  val expectedInvalidMonthFormErrors: Seq[(String, String)] = List(
    "#dateTimeOfArrival.dateOfArrival.day" -> messages("error.date.invalid_month")
  )

  val invalidTestCases: Seq[(String, Form[YourJourneyDetailsDto], Seq[(String, String)])] = Seq(
    Tuple3("Empty form", emptyForm, expectedEmptyFormErrors),
    Tuple3("Invalid date form", formWithInvalidDate, expectedInvalidDateFormErrors),
    Tuple3("Invalid time form", formWithInvalidTime, expectedInvalidTimeFormErrors),
    Tuple3("Invalid date in future form", formWithInvalidDateInFuture, expectedInvalidFutureDateFormErrors),
    Tuple3("Invalid year length form", formWithInvalidYearLength, expectedInvalidYearLengthFormErrors),
    Tuple3("Invalid month form", formWithInvalidMonthInFuture, expectedInvalidMonthFormErrors)
  )

  "YourJourneyDetailsView" when {
    renderViewTest(
      title = "What are your journey details? - Check tax on goods you bring into the UK - GOV.UK",
      heading = "What are your journey details?"
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
