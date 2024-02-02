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

package services

import models.ProductTreeLeaf
import play.api.i18n.{Lang, Langs, MessagesApi}
import play.twirl.api.{Html, HtmlFormat}

import javax.inject.Inject

class LimitExceededContentHelperService @Inject() (
  p: views.html.components.p,
  messages: MessagesApi
)(implicit val langs: Langs) {

  def selectProduct[A](productName: String)(alcohol: A, stickTobacco: A, looseTobacco: A): A =
    productName match {
      case name if name.contains("alcohol")    => alcohol
      case name if name.contains("cigarettes") => stickTobacco
      case name if name.contains("cigars")     => stickTobacco
      case name if name.contains("cigarillos") => stickTobacco
      case name if name.contains("chewing")    => looseTobacco
      case name if name.contains("rolling")    => looseTobacco
      case _                                   => throw new RuntimeException("Help me") // handle an exception here
    }

  def editViewContent(
    productTreeLeaf: ProductTreeLeaf,
    originalAmountFormatted: String,
    userInput: String,
    totalAmount: String
  )(implicit lang: Lang) = {
    val p1Content =
      selectProduct(productTreeLeaf.name)(
        p(
          Html(
            messages(
              "limitExceeded.p1.edit.alcohol",
              originalAmountFormatted,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}"),
              userInput
            )
          )
        ),
        p(
          Html(
            messages(
              "limitExceeded.p1.edit.tobacco",
              originalAmountFormatted,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}"),
              userInput
            )
          )
        ),
        p(
          Html(
            messages(
              "limitExceeded.p1.edit.loose.tobacco",
              originalAmountFormatted,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}"),
              userInput
            )
          )
        )
      )

    val p2Content =
      selectProduct(productTreeLeaf.name)(
        p(
          Html(
            messages(
              "limitExceeded.p2.edit.alcohol",
              totalAmount,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}")
            )
          )
        ),
        p(
          Html(
            messages(
              "limitExceeded.p2.edit.tobacco",
              totalAmount,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}")
            )
          )
        ),
        p(
          Html(
            messages(
              "limitExceeded.p2.edit.loose.tobacco",
              totalAmount,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}")
            )
          )
        )
      )

    val p3Content =
      selectProduct(productTreeLeaf.name)(
        p(
          Html(messages("limitExceeded.p3.edit.alcohol", messages(s"limitExceeded.max.limit.${productTreeLeaf.token}")))
        ),
        p(
          Html(messages("limitExceeded.p3.edit.tobacco", messages(s"limitExceeded.max.limit.${productTreeLeaf.token}")))
        ),
        p(
          Html(
            messages(
              "limitExceeded.p3.edit.loose.tobacco",
              messages(s"limitExceeded.max.limit.${productTreeLeaf.token}")
            )
          )
        )
      )

    val p4Content: HtmlFormat.Appendable =
      selectProduct(productTreeLeaf.name)(
        p(
          Html(
            messages(
              "limitExceeded.p4.edit.alcohol",
              originalAmountFormatted,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}")
            )
          )
        ),
        p(
          Html(
            messages(
              "limitExceeded.p4.edit.tobacco",
              originalAmountFormatted,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}")
            )
          )
        ),
        p(
          Html(
            messages(
              "limitExceeded.p4.edit.loose.tobacco",
              originalAmountFormatted,
              messages(s"limitExceeded.unit.${productTreeLeaf.token}")
            )
          )
        )
      )

    val section1Content: Html =
      HtmlFormat.fill(
        Seq(
          p1Content,
          p2Content,
          p3Content,
          p4Content
        )
      )

    section1Content
  }
}
