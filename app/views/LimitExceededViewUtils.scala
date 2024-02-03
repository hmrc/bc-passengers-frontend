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

package views

import jakarta.inject.Singleton
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}

import javax.inject.Inject

@Singleton
class LimitExceededViewUtils @Inject() (p: views.html.components.p) {

  def selectProduct[A](
    productName: String
  )(alcohol: Option[A], stickTobacco: Option[A], looseTobacco: Option[A]): Option[A] =
    productName match {
      case name if name.contains("alcohol")    => alcohol
      case name if name.contains("cigarettes") => stickTobacco
      case name if name.contains("cigars")     => stickTobacco
      case name if name.contains("cigarillos") => stickTobacco
      case name if name.contains("heated")     => stickTobacco
      case name if name.contains("chewin")     => looseTobacco
      case name if name.contains("rollin")     => looseTobacco
      case _                                   => None
    }

  def addViewContent(
    productName: String,
    productToken: String,
    totalAmount: String
  )(implicit messages: Messages): Html = {
    val p1 =
      selectProduct(productName)(
        Option(
          p(
            Html(messages("limitExceeded.p1.add.alcohol", totalAmount, messages(s"limitExceeded.p1.$productToken"))),
            id = Some("entered-amount")
          )
        ),
        Option(
          p(
            Html(messages("limitExceeded.p1.add.tobacco", totalAmount, messages(s"limitExceeded.p1.$productToken"))),
            id = Some("entered-amount")
          )
        ),
        Option(
          p(
            Html(
              messages("limitExceeded.p1.add.loose.tobacco", totalAmount, messages(s"limitExceeded.p1.$productToken"))
            ),
            id = Some("entered-amount")
          )
        )
      )

    val p2 = Option(
      p(
        Html(messages("limitExceeded.p2", messages(s"limitExceeded.p2.$productToken"))),
        id = Some("limit-exceeded-cannot-use-service")
      )
    )

    val p3 = Option(p(Html(messages("limitExceeded.p3")), id = Some("item-removed")))

    val section1Content: Html =
      HtmlFormat.fill(
        Seq(
          p1,
          p2,
          p3
        ).flatten
      )

    section1Content
  }

  def editViewContent(
    productName: String,
    productToken: String,
    totalAmount: String,
    originalAmountFormatted: String,
    userInput: String
  )(implicit messages: Messages): Html = {

    val p1Content =
      selectProduct(productName)(
        alcohol = Option(
          p(
            Html(
              messages(
                "limitExceeded.p1.edit.alcohol",
                originalAmountFormatted,
                messages(s"limitExceeded.unit.$productToken"),
                userInput
              )
            ),
            id = Some("entered-amount")
          )
        ),
        stickTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p1.edit.tobacco",
                originalAmountFormatted,
                messages(s"limitExceeded.unit.$productToken"),
                userInput
              )
            ),
            id = Some("entered-amount")
          )
        ),
        looseTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p1.edit.loose.tobacco",
                originalAmountFormatted,
                messages(s"limitExceeded.unit.$productToken"),
                userInput
              )
            ),
            id = Some("entered-amount")
          )
        )
      )

    val p2Content =
      selectProduct(productName)(
        Option(
          p(
            Html(
              messages(
                "limitExceeded.p2.edit.alcohol",
                totalAmount,
                messages(s"limitExceeded.unit.$productToken")
              )
            ),
            id = Some("new-total-amount")
          )
        ),
        Option(
          p(
            Html(
              messages(
                "limitExceeded.p2.edit.tobacco",
                totalAmount,
                messages(s"limitExceeded.unit.$productToken")
              )
            ),
            id = Some("new-total-amount")
          )
        ),
        Option(
          p(
            Html(
              messages(
                "limitExceeded.p2.edit.loose.tobacco",
                totalAmount,
                messages(s"limitExceeded.unit.$productToken")
              )
            ),
            id = Some("new-total-amount")
          )
        )
      )

    val p3Content =
      selectProduct(productName)(
        Option(
          p(
            Html(messages("limitExceeded.p3.edit.alcohol", messages(s"limitExceeded.max.limit.$productToken")))
          )
        ),
        Option(
          p(
            Html(messages("limitExceeded.p3.edit.tobacco", messages(s"limitExceeded.max.limit.$productToken")))
          )
        ),
        Option(
          p(
            Html(
              messages(
                "limitExceeded.p3.edit.loose.tobacco",
                messages(s"limitExceeded.max.limit.$productToken")
              )
            )
          )
        )
      )

    val p4Content =
      selectProduct(productName)(
        Option(
          p(
            Html(
              messages(
                "limitExceeded.p4.edit.alcohol",
                originalAmountFormatted,
                messages(s"limitExceeded.unit.$productToken")
              )
            )
          )
        ),
        Option(
          p(
            Html(
              messages(
                "limitExceeded.p4.edit.tobacco",
                originalAmountFormatted,
                messages(s"limitExceeded.unit.$productToken")
              )
            )
          )
        ),
        Option(
          p(
            Html(
              messages(
                "limitExceeded.p4.edit.loose.tobacco",
                originalAmountFormatted,
                messages(s"limitExceeded.unit.$productToken")
              )
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
        ).flatten
      )

    section1Content
  }

}
