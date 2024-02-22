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
import utils.FormatsAndConversions

import javax.inject.Inject
import scala.math.BigDecimal.RoundingMode

@Singleton
class LimitExceededViewUtils @Inject() (p: views.html.components.p, panelIndent: views.html.components.panelIndent)
    extends FormatsAndConversions {

  private[views] def selectProduct[A](
    productName: String
  )(alcohol: Option[A], stickTobacco: Option[A], looseTobacco: Option[A]): Option[A] =
    productName match {
      case name if name.contains("alcohol")    => alcohol
      case name if name.contains("cigarettes") => stickTobacco
      case name if name.contains("cigars")     => stickTobacco
      case name if name.contains("cigarillos") => stickTobacco
      case name if name.contains("heated")     => stickTobacco
      case name if name.contains("chewing")    => looseTobacco
      case name if name.contains("rolling")    => looseTobacco
      case _                                   => None
    }

  private[views] def determineSingularOrPlural[A](amount: String, singular: A, plural: A) =
    if (BigDecimal(amount).setScale(0, RoundingMode.HALF_UP) == 1) {
      singular
    } else {
      plural
    }

  def addViewContent(
    productName: String,
    productToken: String,
    totalAmount: String
  )(implicit messages: Messages): Html = {

    val p1 =
      selectProduct(productName)(
        alcohol = Option(
          determineSingularOrPlural(
            amount = totalAmount,
            singular = p(
              Html(
                messages(
                  "limitExceeded.p1.add.alcohol",
                  totalAmount,
                  messages("limitExceeded.litre"),
                  messages(s"limitExceeded.$productToken")
                )
              ),
              id = Some("entered-amount")
            ),
            plural = p(
              Html(
                messages(
                  "limitExceeded.p1.add.alcohol",
                  totalAmount,
                  messages("limitExceeded.litres"),
                  messages(s"limitExceeded.$productToken")
                )
              ),
              id = Some("entered-amount")
            )
          )
        ),
        stickTobacco = Option(
          determineSingularOrPlural(
            totalAmount,
            p(
              Html(
                messages("limitExceeded.p1.add.tobacco", totalAmount, messages(s"limitExceeded.$productToken.singular"))
              ),
              id = Some("entered-amount")
            ),
            p(
              Html(
                messages("limitExceeded.p1.add.tobacco", totalAmount, messages(s"limitExceeded.$productToken.plural"))
              ),
              id = Some("entered-amount")
            )
          )
        ),
        looseTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p1.add.loose.tobacco",
                totalAmount,
                messages(s"limitExceeded.grams.of"),
                messages(s"limitExceeded.$productToken")
              )
            ),
            id = Some("entered-amount")
          )
        )
      )

    val p2 = Option(
      p(
        Html(messages("limitExceeded.p2", messages(s"limitExceeded.max.limit.$productToken"))),
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

  def addViewPanelContent(
    productName: String,
    productToken: String,
    userInput: String
  )(implicit messages: Messages): Html = {
    val content =
      selectProduct(productName)(
        alcohol = Option(
          panelIndent(
            Html(
              messages(
                "limitExceeded.add.panelIndent",
                userInput,
                determineSingularOrPlural(
                  userInput,
                  messages("limitExceeded.litre"),
                  messages("limitExceeded.litres")
                ),
                messages(s"limitExceeded.$productToken")
              )
            )
          )
        ),
        stickTobacco = Option(
          panelIndent(
            Html(
              messages(
                "limitExceeded.add.panelIndent.tobacco",
                userInput,
                determineSingularOrPlural(
                  userInput,
                  messages(s"limitExceeded.$productToken.singular"),
                  messages(s"limitExceeded.$productToken.plural")
                )
              )
            )
          )
        ),
        looseTobacco = Option(
          panelIndent(
            Html(
              messages(
                "limitExceeded.add.panelIndent.loose.tobacco",
                userInput,
                messages(s"limitExceeded.grams.of"),
                messages(s"limitExceeded.$productToken")
              )
            )
          )
        )
      )

    HtmlFormat.fill(
      Seq(content).flatten
    )
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
                "limitExceeded.p1.edit.alcohol.a",
                originalAmountFormatted,
                determineSingularOrPlural(
                  originalAmountFormatted,
                  messages("limitExceeded.litre"),
                  messages("limitExceeded.litres")
                ),
                messages(s"limitExceeded.$productToken"),
                messages(
                  "limitExceeded.p1.edit.alcohol.b",
                  userInput,
                  determineSingularOrPlural(
                    userInput,
                    messages("limitExceeded.litre"),
                    messages("limitExceeded.litres")
                  ),
                  messages(s"limitExceeded.$productToken")
                )
              )
            ),
            id = Some("entered-amount")
          )
        ),
        stickTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p1.edit.tobacco.a",
                originalAmountFormatted,
                determineSingularOrPlural(
                  originalAmountFormatted,
                  messages(s"limitExceeded.$productToken.singular"),
                  messages(s"limitExceeded.$productToken.plural")
                ),
                messages(
                  "limitExceeded.p1.edit.tobacco.b",
                  userInput,
                  determineSingularOrPlural(
                    userInput,
                    messages(s"limitExceeded.$productToken.singular"),
                    messages(s"limitExceeded.$productToken.plural")
                  )
                )
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
                messages("limitExceeded.grams.of"),
                messages(s"limitExceeded.$productToken"),
                userInput
              )
            ),
            id = Some("entered-amount")
          )
        )
      )

    val p2Content =
      selectProduct(productName)(
        alcohol = Option(
          determineSingularOrPlural(
            totalAmount,
            p(
              Html(
                messages(
                  "limitExceeded.p2.edit.alcohol",
                  totalAmount,
                  messages(s"limitExceeded.litre"),
                  messages(s"limitExceeded.$productToken")
                )
              ),
              id = Some("new-total-amount")
            ),
            p(
              Html(
                messages(
                  "limitExceeded.p2.edit.alcohol",
                  totalAmount,
                  messages(s"limitExceeded.litres"),
                  messages(s"limitExceeded.$productToken")
                )
              ),
              id = Some("new-total-amount")
            )
          )
        ),
        stickTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p2.edit.tobacco",
                totalAmount,
                determineSingularOrPlural(
                  totalAmount,
                  messages(s"limitExceeded.$productToken.singular"),
                  messages(s"limitExceeded.$productToken.plural")
                )
              )
            ),
            id = Some("new-total-amount")
          )
        ),
        looseTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p2.edit.loose.tobacco",
                totalAmount,
                messages(s"limitExceeded.grams.of"),
                messages(s"limitExceeded.$productToken")
              )
            ),
            id = Some("new-total-amount")
          )
        )
      )

    val p3Content =
      selectProduct(productName)(
        alcohol = Option(
          p(
            Html(messages("limitExceeded.p3.edit.alcohol", messages(s"limitExceeded.max.limit.$productToken"))),
            id = Some("limit-exceeded-cannot-use-service")
          )
        ),
        stickTobacco = Option(
          p(
            Html(messages("limitExceeded.p3.edit.tobacco", messages(s"limitExceeded.max.limit.$productToken"))),
            id = Some("limit-exceeded-cannot-use-service")
          )
        ),
        looseTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p3.edit.loose.tobacco",
                messages(s"limitExceeded.max.limit.$productToken")
              )
            ),
            id = Some("limit-exceeded-cannot-use-service")
          )
        )
      )

    val p4Content =
      selectProduct(productName)(
        alcohol = Option(
          p(
            Html(
              messages(
                "limitExceeded.p4.edit.alcohol",
                originalAmountFormatted,
                determineSingularOrPlural(
                  originalAmountFormatted,
                  messages(s"limitExceeded.litre"),
                  messages(s"limitExceeded.litres")
                ),
                messages(s"limitExceeded.$productToken")
              )
            ),
            id = Some("revert-back")
          )
        ),
        stickTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p4.edit.tobacco",
                originalAmountFormatted,
                determineSingularOrPlural(
                  originalAmountFormatted,
                  messages(s"limitExceeded.$productToken.singular"),
                  messages(s"limitExceeded.$productToken.plural")
                )
              )
            ),
            id = Some("revert-back")
          )
        ),
        looseTobacco = Option(
          p(
            Html(
              messages(
                "limitExceeded.p4.edit.loose.tobacco",
                originalAmountFormatted,
                messages(s"limitExceeded.grams.of"),
                messages(s"limitExceeded.$productToken")
              )
            ),
            id = Some("revert-back")
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
