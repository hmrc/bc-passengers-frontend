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

package views.purchased_products

import models._
import play.api.i18n.Lang
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.purchased_products.{dashboard, previous_goods}

class PreviousGoodsViewSpec extends BaseViewSpec {

  private val (weightOrVolume, noOfSticks): (BigDecimal, Int) = (BigDecimal(50), 10)

  private val alcoholProductPath: ProductPath    = ProductPath(path = "alcohol/wine")
  private val tobaccoProductPath: ProductPath    = ProductPath(path = "tobacco/cigars")
  private val otherGoodsProductPath: ProductPath = ProductPath(path = "other-goods/furniture")

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

  private val alcoholProductTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "wine",
    name = "label.alcohol.wine",
    rateID = "ALC/A3/WINE",
    templateId = "alcohol",
    applicableLimits = List("L-WINE")
  )

  private val tobaccoProductTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "cigars",
    name = "label.tobacco.cigars",
    rateID = "TOB/A1/CIGAR",
    templateId = "cigars",
    applicableLimits = List("L-CIGAR")
  )

  private val otherGoodsProductTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "furniture",
    name = "label.other-goods.furniture",
    rateID = "OGD/FURN",
    templateId = "other-goods",
    applicableLimits = Nil
  )

  private val alcoholPurchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
    path = alcoholProductPath,
    iid = "iid0",
    weightOrVolume = Some(weightOrVolume),
    noOfSticks = None,
    country = Some(country),
    currency = Some("GBP"),
    cost = Some(100.00)
  )

  private val tobaccoPurchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
    path = tobaccoProductPath,
    iid = "iid1",
    weightOrVolume = Some(weightOrVolume),
    noOfSticks = Some(noOfSticks),
    country = Some(country),
    currency = Some("GBP"),
    cost = Some(100.00)
  )

  private val otherGoodsPurchasedProductInstance: PurchasedProductInstance = PurchasedProductInstance(
    path = otherGoodsProductPath,
    iid = "iid2",
    weightOrVolume = None,
    noOfSticks = None,
    country = Some(country),
    currency = Some("GBP"),
    cost = Some(100.00)
  )

  private val alcoholPurchasedItemList: List[PurchasedItem] = List(
    PurchasedItem(
      purchasedProductInstance = alcoholPurchasedProductInstance,
      productTreeLeaf = alcoholProductTreeLeaf,
      currency = currency,
      gbpCost = 200.00,
      exchangeRate = exchangeRate
    )
  )

  private val tobaccoPurchasedItemList: List[PurchasedItem] = List(
    PurchasedItem(
      purchasedProductInstance = tobaccoPurchasedProductInstance,
      productTreeLeaf = tobaccoProductTreeLeaf,
      currency = currency,
      gbpCost = 300.00,
      exchangeRate = exchangeRate
    )
  )

  private def otherGoodsPurchasedItemList(fillNumber: Int = 1): List[PurchasedItem] = List.fill(fillNumber)(
    PurchasedItem(
      purchasedProductInstance = otherGoodsPurchasedProductInstance,
      productTreeLeaf = otherGoodsProductTreeLeaf,
      currency = currency,
      gbpCost = 400.00,
      exchangeRate = exchangeRate
    )
  )

  val viewViaApply: HtmlFormat.Appendable = injected[previous_goods].apply(
    journeyData = JourneyData(),
    previousAlcoholPurchasedItemList = alcoholPurchasedItemList,
    previousTobaccoPurchasedItemList = tobaccoPurchasedItemList,
    previousOtherGoodsPurchasedItemList = otherGoodsPurchasedItemList(),
    backLink = None,
    isIrishBorderQuestionEnabled = true,
    isGbNi = true,
    isEU = false,
    isUkResident = true
  )(
    request = request,
    messages = messages,
    lang = Lang("en"),
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[previous_goods].render(
    journeyData = JourneyData(),
    previousAlcoholPurchasedItemList = alcoholPurchasedItemList,
    previousTobaccoPurchasedItemList = tobaccoPurchasedItemList,
    previousOtherGoodsPurchasedItemList = otherGoodsPurchasedItemList(),
    backLink = None,
    isIrishBorderQuestionEnabled = true,
    isGbNi = true,
    isEU = false,
    isUkResident = true,
    request = request,
    messages = messages,
    lang = Lang("en"),
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[previous_goods].f(
    JourneyData(),
    alcoholPurchasedItemList,
    tobaccoPurchasedItemList,
    otherGoodsPurchasedItemList(),
    None,
    true,
    true,
    false,
    true
  )(request, messages, Lang("en"), appConfig)

  "PreviousGoodsView" when {
    renderViewTest(
      title = "Your previously declared goods - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Your previously declared goods"
    )
  }
}
