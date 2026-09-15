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

package views.travel_details

import play.twirl.api.HtmlFormat
import util.WineStillOrSparklingFeature
import views.BaseViewSpec
import views.html.travel_details.goods_bought_inside_eu

class GoodsBoughtInsideEUViewSpec extends BaseViewSpec with WineStillOrSparklingFeature {

  private def viewWithToggle(enabled: Boolean): HtmlFormat.Appendable =
    injected[goods_bought_inside_eu].apply(backLink = None)(request, messages, appConfigToggle(enabled))

  val viewViaApply: HtmlFormat.Appendable = injected[goods_bought_inside_eu].apply(backLink = None)(
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaRender: HtmlFormat.Appendable = injected[goods_bought_inside_eu].render(
    backLink = None,
    request = request,
    messages = messages,
    appConfig = appConfig
  )

  val viewViaF: HtmlFormat.Appendable = injected[goods_bought_inside_eu].ref.f(None)(request, messages, appConfig)

  "GoodsBoughtInsideEUView" when {
    renderViewTest(
      title = "You do not need to tell us about your goods - Check tax on goods you bring into the UK - GOV.UK",
      heading = "You do not need to tell us about your goods"
    )
  }

  "GoodsBoughtInsideEUView with the wine-still-or-sparkling toggle ON" should {

    lazy val body = viewWithToggle(true).body

    "show the 'bringing in goods to sell' customs intro instead of the table intro" in {
      body should include(messages("text.tell_us.customs_sell.still-or-sparkling"))
      body should not include messages("text.customs_officers_are_more_likely_")
    }

    "show the merged Alcohol section (beer or cider, wine still or sparkling, spirits, fortified)" in {
      body should include(messages("label.tell_us.alcohol.beer_cider.still-or-sparkling"))
      body should include(messages("label.tell_us.alcohol.wine.still-or-sparkling"))
      body should include(messages("label.tell_us.alcohol.spirits.still-or-sparkling"))
      body should include(messages("label.tell_us.alcohol.fortified.still-or-sparkling"))
    }

    "show the Tobacco section" in {
      body should include(messages("label.tell_us.tobacco.cigarettes.still-or-sparkling"))
      body should include(messages("label.tell_us.tobacco.heated.still-or-sparkling"))
    }
  }

  "GoodsBoughtInsideEUView with the wine-still-or-sparkling toggle OFF" should {

    lazy val body = viewWithToggle(false).body

    "show the original customs intro and the goods table" in {
      body should include(messages("text.customs_officers_are_more_likely_"))
      body should include(messages("label.type_of_goods"))
      body should include(messages("label.110_litres"))
    }

    "not show the restructured Alcohol/Tobacco content" in {
      body should not include messages("text.tell_us.customs_sell.still-or-sparkling")
      body should not include messages("label.tell_us.alcohol.beer_cider.still-or-sparkling")
    }
  }
}
