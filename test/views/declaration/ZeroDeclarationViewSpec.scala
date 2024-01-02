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

import models._

import java.time.{LocalDate, LocalTime}
import play.twirl.api.HtmlFormat
import util.parseLocalTime
import views.BaseViewSpec
import views.html.declaration.zero_declaration

class ZeroDeclarationViewSpec extends BaseViewSpec {

  private val weightOrVolume: BigDecimal = 40

  private val calculation: Calculation = Calculation(
    excise = "0.00",
    customs = "0.00",
    vat = "0.00",
    allTax = "0.00"
  )

  private val descriptionLabels: DescriptionLabels = DescriptionLabels(
    description = "label.X_litres_X",
    args = List("40", "label.alcohol.wine")
  )

  private val currency: Currency = Currency(
    code = "GBP",
    displayName = "title.british_pounds_gbp",
    valueForConversion = None,
    currencySynonyms = List("England", "Scotland", "Wales", "Northern Ireland", "British", "sterling", "pound", "GB")
  )

  private val country: Country = Country(
    code = "GB",
    countryName = "title.united_kingdom",
    alphaTwoCode = "GB",
    isEu = false,
    isCountry = true,
    countrySynonyms = List("England", "Scotland", "Wales", "Northern Ireland", "GB", "UK")
  )

  private val exchangeRate: ExchangeRate = ExchangeRate(
    rate = "1.20",
    date = "2023-05-06"
  )

  private val metadata: Metadata = Metadata(
    description = "40 litres wine",
    name = "label.alcohol.wine",
    cost = "100.00",
    descriptionLabels = descriptionLabels,
    currency = currency,
    country = country,
    exchangeRate = exchangeRate,
    originCountry = None
  )

  private val items: List[Item] = List(
    Item(
      rateId = "ALC/A3/WINE",
      purchaseCost = "100.00",
      noOfUnits = None,
      weightOrVolume = Some(weightOrVolume),
      calculation = calculation,
      metadata = metadata,
      isVatPaid = Some(false),
      isCustomPaid = Some(false),
      isExcisePaid = Some(false),
      isUccRelief = Some(false)
    )
  )

  private val bands: List[Band] = List(
    Band(
      code = "A",
      items = items,
      calculation = calculation
    )
  )

  private val alcohol: Alcohol = Alcohol(
    bands = bands,
    calculation = calculation
  )

  private val userInformation: UserInformation = UserInformation(
    firstName = "Blake",
    lastName = "Tyler",
    identificationType = "passport",
    identificationNumber = "SX12345",
    emailAddress = "blaketyler@gmail.com",
    selectPlaceOfArrival = "",
    enterPlaceOfArrival = "Newcastle Airport",
    dateOfArrival = LocalDate.parse("2023-05-06"),
    timeOfArrival = parseLocalTime("12:20 pm")
  )

  private val calculatorResponseDto: CalculatorResponseDto = CalculatorResponseDto(
    items = items,
    calculation = calculation,
    allItemsUseGBP = true
  )

  private val calculatorResponse: CalculatorResponse = CalculatorResponse(
    alcohol = Some(alcohol),
    tobacco = None,
    otherGoods = None,
    calculation = calculation,
    withinFreeAllowance = true,
    limits = Map("L-WINE" -> "0.011"),
    isAnyItemOverAllowance = false
  )

  val viewViaApply: HtmlFormat.Appendable = injected[zero_declaration].apply(
    previousDeclaration = true,
    deltaCalc = Some(calculation),
    oldAllTax = Some("0.00"),
    userInformation = Some(userInformation),
    calculatorResponse = calculatorResponse,
    calculatorResponseDto = calculatorResponseDto,
    chargeReference = "XJPR5768524625",
    placeOfArrivalValue = "Newcastle Airport"
  )(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[zero_declaration].render(
    previousDeclaration = true,
    deltaCalc = Some(calculation),
    oldAllTax = Some("0.00"),
    userInformation = Some(userInformation),
    calculatorResponse = calculatorResponse,
    calculatorResponseDto = calculatorResponseDto,
    chargeReference = "XJPR5768524625",
    placeOfArrivalValue = "Newcastle Airport",
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[zero_declaration].f(
    true,
    Some(calculation),
    Some("0.00"),
    Some(userInformation),
    calculatorResponse,
    calculatorResponseDto,
    "XJPR5768524625",
    "Newcastle Airport"
  )(request, messages, appConfig)

  "ZeroDeclarationView" when {
    renderViewTest(
      title = "Declaration complete - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Declaration complete"
    )
  }
}
