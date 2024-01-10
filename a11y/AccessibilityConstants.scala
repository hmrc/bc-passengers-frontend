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

import models._

import java.time.{LocalDate, LocalTime}
import util.parseLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

trait AccessibilityConstants {
  private val weightOrVolume: BigDecimal = 40

  private val productPath: ProductPath = ProductPath("tobacco/cigars")

  private val descriptionLabels: DescriptionLabels = DescriptionLabels(
    description = "label.X_litres_X",
    args = List("40", "label.alcohol.wine")
  )

  private val exchangeRate: ExchangeRate = ExchangeRate(
    rate = "1.20",
    date = "2023-05-06"
  )

  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "cigars",
    name = "label.tobacco.cigars",
    rateID = "TOB/A1/CIGAR",
    templateId = "cigars",
    applicableLimits = List("L-CIGAR")
  )

  private val currency: Currency = Currency(
    code = "EUR",
    displayName = "title.euro_eur",
    valueForConversion = Some("EUR"),
    currencySynonyms = List("Europe", "European")
  )

  private val country: Country = Country(
    code = "FR",
    countryName = "title.france",
    alphaTwoCode = "FR",
    isEu = true,
    isCountry = true,
    countrySynonyms = Nil
  )

  private val calculation: Calculation = Calculation(
    excise = "0.00",
    customs = "0.00",
    vat = "0.00",
    allTax = "0.00"
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

  val countries: List[Country] = List(country)

  val currencies: List[Currency] = List(currency)

  val namesAndTokens: List[(String, String)] = List(
    ("label.other-goods.electronic-devices.televisions", "televisions"),
    ("label.other-goods.electronic-devices.other", "other")
  )

  val portsOfArrival: List[PortsOfArrival] = List(
    PortsOfArrival(
      code = "NCL",
      displayName = "title.newcastle_airport",
      isGB = true,
      portSynonyms = List("Newcastle International Airport", "NCL")
    )
  )

  val userInformation: UserInformation = UserInformation(
    firstName = "Blake",
    lastName = "Tyler",
    identificationType = "passport",
    identificationNumber = "SX12345",
    emailAddress = "blaketyler@gmail.com",
    selectPlaceOfArrival = "",
    enterPlaceOfArrival = "Newcastle Airport",
    dateOfArrival = parseLocalDate("2023-05-06"),
    timeOfArrival = LocalTime.parse("12:20 pm", DateTimeFormatter.ofPattern("hh:mm a",Locale.UK))
  )

  val purchasedItems: List[PurchasedItem] = List(
    PurchasedItem(
      purchasedProductInstance = PurchasedProductInstance(productPath, "iid0"),
      productTreeLeaf = productTreeLeaf,
      currency = currency,
      gbpCost = 200.00,
      exchangeRate = exchangeRate
    )
  )

  val calculatorResponse: CalculatorResponse = CalculatorResponse(
    alcohol = Some(alcohol),
    tobacco = None,
    otherGoods = None,
    calculation = calculation,
    withinFreeAllowance = true,
    limits = Map("L-WINE" -> "0.011"),
    isAnyItemOverAllowance = false
  )

  val calculatorResponseDto: CalculatorResponseDto = CalculatorResponseDto(
    items = items,
    calculation = calculation,
    allItemsUseGBP = true
  )
}
