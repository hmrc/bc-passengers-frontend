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

package views.other_goods

import config.AppConfig
import controllers.OtherGoodsInputController
import models._
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import util.BaseSpec
import views.ViewSpec
import views.html.other_goods.other_goods_input

class OtherGoodsInputViewSpec extends BaseSpec with ViewSpec {

  private val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private val appConfig: AppConfig                     = injected[AppConfig]
  private val messagesApi: MessagesApi                 = injected[MessagesApi]
  private val messages: Messages                       = messagesApi.preferred(request)

  private val productPath: ProductPath = ProductPath(path = "other-goods/antiques")

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

  private val otherGoodsSearchItems: List[OtherGoodsSearchItem] = List(
    OtherGoodsSearchItem(
      name = "label.other-goods.antiques",
      path = productPath
    )
  )

  private val validForm: Form[OtherGoodsDto] =
    form(searchTerm = "label.other-goods.antiques", country = "FR", currency = "EUR", cost = "4,444.00")

  private def form(
    searchTerm: String = "invalid",
    country: String = "",
    currency: String = "",
    cost: String = ""
  ): Form[OtherGoodsDto] = injected[OtherGoodsInputController].addCostForm
    .bind(
      Map(
        "searchTerm" -> searchTerm,
        "country"    -> country,
        "currency"   -> currency,
        "cost"       -> cost
      )
    )

  private trait ViewFixture {
    def viewViaApply(otherItemMode: String, form: Form[OtherGoodsDto] = validForm): HtmlFormat.Appendable =
      injected[other_goods_input].apply(
        form = form,
        iid = Some("iid0"),
        countries = nonEuropeanCountries,
        countriesEU = europeanCountries,
        currencies = currencies,
        journeyStart = None,
        otherGoodsSearchItems = otherGoodsSearchItems,
        otherItemMode = otherItemMode,
        path = productPath,
        backLink = None,
        customBackLink = false
      )(request, messages, appConfig)

    def viewViaRender(otherItemMode: String, form: Form[OtherGoodsDto] = validForm): HtmlFormat.Appendable =
      injected[other_goods_input].render(
        form = form,
        iid = Some("iid0"),
        countries = nonEuropeanCountries,
        countriesEU = europeanCountries,
        currencies = currencies,
        journeyStart = None,
        otherGoodsSearchItems = otherGoodsSearchItems,
        otherItemMode = otherItemMode,
        path = productPath,
        request = request,
        messages = messages,
        appConfig = appConfig,
        backLink = None,
        customBackLink = false
      )

    def viewViaF(otherItemMode: String, form: Form[OtherGoodsDto] = validForm): HtmlFormat.Appendable =
      injected[other_goods_input].f(
        form,
        Some("iid0"),
        nonEuropeanCountries,
        europeanCountries,
        currencies,
        None,
        otherGoodsSearchItems,
        otherItemMode,
        productPath,
        None,
        false
      )(request, messages, appConfig)
  }

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

  private val errorInputs: Seq[(String, String, String, Form[OtherGoodsDto])] = Seq(
    (
      "create",
      "#country",
      "error.country.invalid",
      form(searchTerm = "label.other-goods.antiques", currency = "EUR", cost = "4,444.00")
    ),
    (
      "create",
      "#currency",
      "error.currency.invalid",
      form(searchTerm = "label.other-goods.antiques", country = "FR", cost = "4,444.00")
    ),
    (
      "create",
      "#cost",
      "error.required.other-goods.price",
      form(searchTerm = "label.other-goods.antiques", country = "FR", currency = "EUR")
    ),
    (
      "edit",
      "#country",
      "error.country.invalid",
      form(searchTerm = "label.other-goods.antiques", currency = "EUR", cost = "4,444.00")
    ),
    (
      "edit",
      "#currency",
      "error.currency.invalid",
      form(searchTerm = "label.other-goods.antiques", country = "FR", cost = "4,444.00")
    ),
    (
      "edit",
      "#cost",
      "error.required.other-goods.price",
      form(searchTerm = "label.other-goods.antiques", country = "FR", currency = "EUR")
    ),
    (
      "display",
      "#country",
      "error.country.invalid",
      form(searchTerm = "label.other-goods.antiques", currency = "EUR", cost = "4,444.00")
    ),
    (
      "display",
      "#currency",
      "error.currency.invalid",
      form(searchTerm = "label.other-goods.antiques", country = "FR", cost = "4,444.00")
    ),
    (
      "display",
      "#cost",
      "error.required.other-goods.price",
      form(searchTerm = "label.other-goods.antiques", country = "FR", currency = "EUR")
    )
  )

  "OtherGoodsInputView" when {
    ".apply" should {
      "display the correct title" when {
        def test(otherItemMode: String, title: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaApply(otherItemMode = otherItemMode)
            ).title shouldBe title
          }

        titleInput.foreach(args => (test _).tupled(args))
      }

      "display the correct heading" when {
        def test(otherItemMode: String, heading: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaApply(otherItemMode = otherItemMode)
            ).select("h1").text shouldBe heading
          }

        headingInput.foreach(args => (test _).tupled(args))
      }

      "have all info in error summary" when {
        def test(otherItemMode: String, id: String, errorKey: String, form: Form[OtherGoodsDto]): Unit =
          s"have correct error for mode $otherItemMode and id $id and key $errorKey" in new ViewFixture {
            val doc: Document = document(viewViaApply(otherItemMode = otherItemMode, form = form))
            doc.title()                            should startWith(messages("label.error"))
            messages("label.there_is_a_problem") shouldBe getErrorTitle(doc)
            List(id -> messages(errorKey)) shouldBe getErrorsInSummary(doc)
          }

        errorInputs.foreach(args => (test _).tupled(args))
      }

    }

    ".render" should {
      "display the correct title" when {
        def test(otherItemMode: String, title: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaRender(otherItemMode = otherItemMode)
            ).title shouldBe title
          }

        titleInput.foreach(args => (test _).tupled(args))
      }

      "display the correct heading" when {
        def test(otherItemMode: String, heading: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaRender(otherItemMode = otherItemMode)
            ).select("h1").text shouldBe heading
          }

        headingInput.foreach(args => (test _).tupled(args))
      }
    }

    ".f" should {
      "display the correct title" when {
        def test(otherItemMode: String, title: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaF(otherItemMode = otherItemMode)
            ).title shouldBe title
          }

        titleInput.foreach(args => (test _).tupled(args))
      }

      "display the correct heading" when {
        def test(otherItemMode: String, heading: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaF(otherItemMode = otherItemMode)
            ).select("h1").text shouldBe heading
          }

        headingInput.foreach(args => (test _).tupled(args))
      }
    }
  }
}
