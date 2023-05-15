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

package views.declaration

import models.{EnterYourDetailsDto, PortsOfArrival}
import models.EnterYourDetailsDto.form
import org.joda.time.DateTime
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.declaration.enter_your_details

class EnterYourDetailsViewSpec extends BaseViewSpec {

  private val declarationTime: DateTime = DateTime.parse("2023-05-06T21:15:00.000")

  private val validForm: Form[EnterYourDetailsDto] = form(declarationTime).bind(
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
    form = validForm,
    portsOfArrival = portsOfArrival,
    journeyStart = None,
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[enter_your_details].f(
    validForm,
    portsOfArrival,
    None,
    None
  )(request, messages, appConfig)

  "EnterYourDetailsView" when {
    renderViewTest(
      title = "Enter your details - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Enter your details"
    )
  }
}
