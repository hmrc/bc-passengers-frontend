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

package views.other_goods

import config.AppConfig
import controllers.OtherGoodsInputController
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.{Html, HtmlFormat}
import util.BaseSpec
import views.html.other_goods.other_goods_input

class OtherGoodsInputViewSpec extends BaseSpec with Injecting {

  private val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private val appConfig: AppConfig                     = injected[AppConfig]
  private val messagesApi: MessagesApi                 = injected[MessagesApi]
  private val messages: Messages                       = messagesApi.preferred(request)

  private val productPath: ProductPath = ProductPath(path = "other-goods/antiques")

  private val currencies: List[Currency] = List(
    Currency(
      code = "USD",
      displayName = "title.usa_dollars_usd",
      valueForConversion = Some("USD"),
      currencySynonyms = List("USD", "USA", "US", "United States of America", "American")
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
    OtherGoodsSearchItem(name = "label.other-goods.antiques", path = productPath)
  )

  private val validForm: Form[OtherGoodsDto] = injected[OtherGoodsInputController].addCostForm
    .bind(
      Map(
        "searchTerm" -> "label.other-goods.antiques",
        "country"    -> "FR",
        "currency"   -> "EUR",
        "cost"       -> "4,444.00"
      )
    )

  private def document(html: Html): Document = Jsoup.parse(html.toString())

  private trait ViewFixture {
    def viewViaApply(otherItemMode: String): HtmlFormat.Appendable = inject[other_goods_input].apply(
      form = validForm,
      iid = Some("iid0"),
      countries = nonEuropeanCountries,
      countriesEU = europeanCountries,
      currencies = currencies,
      journeyStart = None,
      otherGoodsSearchItems = otherGoodsSearchItems,
      otherItemMode = otherItemMode,
      path = productPath
    )(request, messages, appConfig)

    def viewViaRender(otherItemMode: String): HtmlFormat.Appendable = inject[other_goods_input].render(
      form = validForm,
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
      appConfig = appConfig
    )

    def viewViaF(otherItemMode: String): HtmlFormat.Appendable =
      inject[other_goods_input].f(
        validForm,
        Some("iid0"),
        nonEuropeanCountries,
        europeanCountries,
        currencies,
        None,
        otherGoodsSearchItems,
        otherItemMode,
        productPath
      )(request, messages, appConfig)
  }

  "OtherGoodsInputView" when {
    ".apply" should {
      "display the correct title" when {
        def test(otherItemMode: String, title: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaApply(otherItemMode = otherItemMode)
            ).title shouldBe title
          }

        val input = Seq(
          ("create", "Tell us about the Item of other goods - Check tax on goods you bring into the UK - GOV.UK"),
          (
            "edit",
            "Tell us about the Antique, collector’s item or artwork - Check tax on goods you bring into the UK - GOV.UK"
          ),
          ("display", "Tell us about the Item of other goods - Check tax on goods you bring into the UK - GOV.UK")
        )

        input.foreach(args => (test _).tupled(args))
      }

      "display the correct heading" when {
        def test(otherItemMode: String, heading: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaApply(otherItemMode = otherItemMode)
            ).select("h1").text shouldBe heading
          }

        val input = Seq(
          ("create", "Tell us about the Item of other goods"),
          ("edit", "Tell us about the Antique, collector’s item or artwork"),
          ("display", "Tell us about the Item of other goods")
        )

        input.foreach(args => (test _).tupled(args))
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

        val input = Seq(
          ("create", "Tell us about the Item of other goods - Check tax on goods you bring into the UK - GOV.UK"),
          (
            "edit",
            "Tell us about the Antique, collector’s item or artwork - Check tax on goods you bring into the UK - GOV.UK"
          ),
          ("display", "Tell us about the Item of other goods - Check tax on goods you bring into the UK - GOV.UK")
        )

        input.foreach(args => (test _).tupled(args))
      }

      "display the correct heading" when {
        def test(otherItemMode: String, heading: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaRender(otherItemMode = otherItemMode)
            ).select("h1").text shouldBe heading
          }

        val input = Seq(
          ("create", "Tell us about the Item of other goods"),
          ("edit", "Tell us about the Antique, collector’s item or artwork"),
          ("display", "Tell us about the Item of other goods")
        )

        input.foreach(args => (test _).tupled(args))
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

        val input = Seq(
          ("create", "Tell us about the Item of other goods - Check tax on goods you bring into the UK - GOV.UK"),
          (
            "edit",
            "Tell us about the Antique, collector’s item or artwork - Check tax on goods you bring into the UK - GOV.UK"
          ),
          ("display", "Tell us about the Item of other goods - Check tax on goods you bring into the UK - GOV.UK")
        )

        input.foreach(args => (test _).tupled(args))
      }

      "display the correct heading" when {
        def test(otherItemMode: String, heading: String): Unit =
          s"otherItemMode is $otherItemMode" in new ViewFixture {
            document(
              viewViaF(otherItemMode = otherItemMode)
            ).select("h1").text shouldBe heading
          }

        val input = Seq(
          ("create", "Tell us about the Item of other goods"),
          ("edit", "Tell us about the Antique, collector’s item or artwork"),
          ("display", "Tell us about the Item of other goods")
        )

        input.foreach(args => (test _).tupled(args))
      }
    }
  }
}
