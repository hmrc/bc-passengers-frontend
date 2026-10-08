/*
 * Copyright 2026 HM Revenue & Customs
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

package views.other_ni_goods

import config.AppConfig
import controllers.OtherGoodsInputNIController
import forms.OtherGoodsInputNIForm
import models.*
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import util.BaseSpec
import views.ViewSpec
import views.html.other_goods_ni.other_goods_input_ni

class OtherGoodsInputNIViewSpec extends BaseSpec with ViewSpec {

  private val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private val appConfig: AppConfig                     = injected[AppConfig]
  private val messagesApi: MessagesApi                 = injected[MessagesApi]
  private val messages: Messages                       = messagesApi.preferred(request)

  private val productPath: ProductPath         = ProductPath(path = "other-ni-goods/antiques")
  private val productTreeLeaf: ProductTreeLeaf = ProductTreeLeaf(
    token = "antiques",
    name = "label.other-ni-goods.antiques",
    rateID = "OGD/ART",
    templateId = "other-ni-goods",
    applicableLimits = Nil
  )

  private val currencies: List[Currency] = List(
    Currency(
      code = "EUR",
      displayName = "title.euro_eur",
      valueForConversion = Some("EUR"),
      currencySynonyms = List("Europe", "European")
    )
  )

  private val europeanCountries: List[Country] = List(
    Country(
      code = "FR",
      countryName = "title.france",
      alphaTwoCode = "FR",
      isEu = true,
      isCountry = true,
      countrySynonyms = Nil
    )
  )

  private val nonEuropeanCountries: List[Country] = List(
    Country(
      code = "EG",
      countryName = "title.egypt",
      alphaTwoCode = "EG",
      isEu = false,
      isCountry = true,
      countrySynonyms = Nil
    )
  )

  private val validForm: Form[OtherGoodsNIDto] =
    form(country = "FR", currency = "EUR", cost = "4,444.00")

  private def form(
    country: String = "",
    currency: String = "",
    cost: String = ""
  ): Form[OtherGoodsNIDto] = injected[OtherGoodsInputNIForm]
    .otherGoodsNIForm(productPath)
    .bind(
      Map(
        "country"  -> country,
        "currency" -> currency,
        "cost"     -> cost
      )
    )

  def viewViaApply(otherItemMode: String, form: Form[OtherGoodsNIDto] = validForm): HtmlFormat.Appendable =
    injected[other_goods_input_ni].apply(
      form = validForm,
      backLink = None,
      customBackLink = false,
      product = productTreeLeaf,
      path = productPath,
      iid = Some("iid0"),
      countries = nonEuropeanCountries,
      countriesEU = europeanCountries,
      currencies = currencies,
      journeyStart = None
    )(request, messages, appConfig)

  def viewViaRender(otherItemMode: String, form: Form[OtherGoodsNIDto] = validForm): HtmlFormat.Appendable =
    injected[other_goods_input_ni].render(
      form = validForm,
      backLink = None,
      customBackLink = false,
      product = productTreeLeaf,
      path = productPath,
      iid = Some("iid0"),
      countries = nonEuropeanCountries,
      countriesEU = europeanCountries,
      currencies = currencies,
      journeyStart = None,
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  def viewViaF(otherItemMode: String, form: Form[OtherGoodsNIDto] = validForm): HtmlFormat.Appendable =
    injected[other_goods_input_ni].ref.f(
      validForm,
      None,
      false,
      productTreeLeaf,
      productPath,
      Some("iid0"),
      nonEuropeanCountries,
      europeanCountries,
      currencies,
      None
    )(request, messages, appConfig)

  def euOnlyView(otherItemMode: String, form: Form[OtherGoodsNIDto] = validForm): HtmlFormat.Appendable =
    injected[other_goods_input_ni].apply(
      form = validForm,
      backLink = None,
      customBackLink = false,
      product = productTreeLeaf,
      path = productPath,
      iid = Some("iid0"),
      countries = nonEuropeanCountries,
      countriesEU = europeanCountries,
      currencies = currencies,
      journeyStart = Some("euOnly")
    )(request, messages, appConfig)

  private val titleInput: Seq[(String, String)] = Seq(
    ("create", "Tell us about the Item of other goods - Check tax on goods you bring into the UK - GOV.UK"),
    (
      "edit",
      "Tell us about the Antique, collector’s item or artwork - Check tax on goods you bring into the UK - GOV.UK"
    ),
    ("display", "Tell us about the Item of other goods - Check tax on goods you bring into the UK - GOV.UK")
  )

  private val headingInput: Seq[(String, String)] = Seq(
    ("create", "Tell us about the Item of other goods"),
    (
      "edit",
      "Tell us about the Antique, collector’s item or artwork"
    ),
    ("display", "Tell us about the Item of other goods")
  )

  private val errorInputs: Seq[(String, String, String, Form[OtherGoodsNIDto])] = Seq(
    (
      "create",
      "#country",
      "error.country.invalid",
      form(currency = "EUR", cost = "4,444.00")
    ),
    (
      "create",
      "#currency",
      "error.currency.invalid",
      form(country = "FR", cost = "4,444.00")
    ),
    (
      "create",
      "#cost",
      "error.required.other-goods.price",
      form(country = "FR", currency = "EUR")
    ),
    (
      "edit",
      "#country",
      "error.country.invalid",
      form(currency = "EUR", cost = "4,444.00")
    ),
    (
      "edit",
      "#currency",
      "error.currency.invalid",
      form(country = "FR", cost = "4,444.00")
    ),
    (
      "edit",
      "#cost",
      "error.required.other-goods.price",
      form(country = "FR", currency = "EUR")
    ),
    (
      "display",
      "#country",
      "error.country.invalid",
      form(currency = "EUR", cost = "4,444.00")
    ),
    (
      "display",
      "#currency",
      "error.currency.invalid",
      form(country = "FR", cost = "4,444.00")
    ),
    (
      "display",
      "#cost",
      "error.required.other-goods.price",
      form(country = "FR", currency = "EUR")
    )
  )
}
