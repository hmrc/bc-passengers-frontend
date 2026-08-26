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

package views.purchased_products

import models.*
import play.twirl.api.HtmlFormat
import views.BaseViewSpec
import views.html.purchased_products.check_your_goods_answers

class GoodsCheckYourAnswersViewSpec extends BaseViewSpec {

  private val country = Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil)
  private val item = PurchasedProductInstance(
    path = ProductPath("alcohol/beer"),
    iid = "iid0",
    weightOrVolume = Some(BigDecimal(5)),
    country = Some(country),
    originCountry = Some(country),
    currency = Some("EUR"),
    cost = Some(BigDecimal(50))
  )
  private val product = ProductTreeLeaf("beer", "label.alcohol.beer", "ALC/A1/BEER", "alcohol", List("L-BEER"))
  private val currency = Currency("EUR", "title.euro_eur", None, Nil)
  private val tobaccoItem = item.copy(
    path = ProductPath("tobacco/cigars"),
    noOfSticks = Some(10),
    weightOrVolume = Some(BigDecimal(100))
  )
  private val tobaccoProduct = ProductTreeLeaf("cigars", "label.tobacco.cigars", "TOB/A1/CIGAR", "cigars", List("L-CIGAR"))

  val viewViaApply: HtmlFormat.Appendable =
    injected[check_your_goods_answers].apply(item, product, Some(currency))(request, messages, appConfig)

  val viewViaRender: HtmlFormat.Appendable =
    injected[check_your_goods_answers].render(item, product, Some(currency), request, messages, appConfig)

  val viewViaF: HtmlFormat.Appendable =
    injected[check_your_goods_answers].ref.f(item, product, Some(currency))(request, messages, appConfig)

  "GoodsCheckYourAnswersView" when {
    renderViewTest(
      title = "Check your answers - Check tax on goods you bring into the UK - GOV.UK",
      heading = "Check your answers"
    )

    "show the selected item and its answers in a summary list" in {
      val doc = document(viewViaApply)

      doc.select("h2.govuk-heading-m").text() shouldBe "Beer"
      doc.select(".govuk-summary-list__key").eachText() should contain allOf (
        "Type of goods",
        "Type of alcohol",
        "Total volume in litres",
        "Price paid"
      )
      doc.select(".govuk-summary-list").text() should include("50")
      doc.select("a.govuk-link[href*=enter-goods/alcohol/iid0/edit]").isEmpty shouldBe false
      doc.select("button.govuk-button").text() shouldBe "Save and continue"
    }

    "show the tobacco weight in grams" in {
      val doc = document(injected[check_your_goods_answers].apply(tobaccoItem, tobaccoProduct, Some(currency))(request, messages, appConfig))

      doc.select(".govuk-summary-list__key").eachText() should contain("Total weight in grams")
      doc.select(".govuk-summary-list").text() should include("100 grams")
    }
  }
}
