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

import play.twirl.api.HtmlFormat
import views.{BaseSelectors, BaseViewSpec}
import views.html.purchased_products.limit_exceed_add

class LimitExceedAddViewSpec extends BaseViewSpec {

  val viewViaApply: HtmlFormat.Appendable =
    injected[limit_exceed_add].apply(
      totalAccAmount = "110",
      userInput = "0.01",
      token = "cigars",
      productName = "label.tobacco.cigars",
      showPanelIndent = false
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val viewViaRender: HtmlFormat.Appendable =
    injected[limit_exceed_add].render(
      totalAccAmount = "110.2",
      userInput = "0.02",
      token = "cigars",
      productName = "label.tobacco.cigars",
      showPanelIndent = false,
      showGroupMessage = false,
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val viewViaF: HtmlFormat.Appendable =
    injected[limit_exceed_add]
      .f("110.2", "0.02", "cigars", "label.tobacco.cigars", false, false)(request, messages, appConfig)

  object Selectors extends BaseSelectors {
    val panelIndent = "#main-content > div > div > div > div.govuk-inset-text"
  }

  def viewApply(
    amount: String,
    userInput: String,
    item: String,
    productName: String,
    showPanel: Boolean = false,
    showGroupMessage: Boolean = false
  ): HtmlFormat.Appendable =
    injected[limit_exceed_add]
      .apply(amount, userInput, item, productName, showPanel, showGroupMessage)(
        request = request,
        messages = messages,
        appConfig = appConfig
      )

  "LimitExceedView" when {

    renderViewTest(
      title = "There is a problem - Check tax on goods you bring into the UK - GOV.UK",
      heading = "There is a problem"
    )

    "Alcohol" should {

      "display correct content for view" when {

        "the user enters too much beer" should {

          val view = viewApply("110.500", "0.05", "beer", "label.alcohol.beer")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 110.500 litres of beer.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 110 litres of beer.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much non-sparkling cider" should {

          val view = viewApply("20.01", "0.01", "non-sparkling-cider", "label.alcohol.non-sparkling-cider")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 20.01 litres of cider.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 20 litres of cider.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much sparkling-cider" should {

          val view = viewApply("20.01", "0.01", "sparkling-cider", "label.alcohol.sparkling-cider")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 20.01 litres of cider.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 20 litres of cider.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much sparkling-cider-up" should {

          val view = viewApply("20.01", "0.01", "sparkling-cider-up", "label.alcohol.sparkling-cider-up")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 20.01 litres of cider.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 20 litres of cider.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much spirits" should {

          val view = viewApply("10.01", "0.01", "spirits", "label.alcohol.spirits")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 10.01 litres of spirits.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 10 litres of spirits.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much sparkling wine" should {

          val view = viewApply("90.01", "0.01", "sparkling-wine", "label.alcohol.sparkling-wine")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 90.01 litres of sparkling wine.",
              Selectors.p(
                2
              )                 -> "You cannot use this service to declare more than 60 litres of sparkling wine.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much wine" should {

          val view = viewApply("90.01", "0.01", "wine", "label.alcohol.wine")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 90.01 litres of wine.",
              Selectors.p(
                2
              )                 -> "You cannot use this service to declare more than 90 litres of wine.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much wine when sparkling wine has been previously added" should {

          val view = viewApply("100.01", "0.01", "wine", "label.alcohol.wine", showGroupMessage = true)

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 100.01 litres of wine (this includes sparkling wine).",
              Selectors.p(
                2
              )                 -> "You cannot use this service to declare more than 90 litres of wine (this includes up to 60 litres of sparkling wine).",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much other alcohol" should {

          val view = viewApply("20.01", "0.01", "other", "label.alcohol.other")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 20.01 litres of other alcohol.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 20 litres of other alcohol.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much other alcohol when cider has been previously added" should {

          val view = viewApply("30.01", "0.01", "other", "label.alcohol.other", showGroupMessage = true)

          val expectedContent =
            Seq(
              Selectors.p(1)    -> ("You have entered a total of 30.01 litres of all other alcoholic drinks " +
                "(including cider, port, sherry and alcohol up to 22%)."),
              Selectors.p(2)    -> ("You cannot use this service to declare more than 20 litres of all " +
                "other alcoholic drinks (including cider, port, sherry and alcohol up to 22%)."),
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare alcohol over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your alcohol may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }
      }

      def panelIndentTests(
        item: String,
        productKey: String,
        panelMessage: String
      ): Unit =
        s"display the correct content when the showing the panel indent for: $item" when {

          s"the user enters too much $item" should {

            val view = viewApply("110.500", "2.000", item, productKey, showPanel = true)

            val expectedContent =
              Seq(
                Selectors.panelIndent -> s"2.000 litres of $panelMessage"
              )

            behave like pageWithExpectedMessages(view, expectedContent)
          }

        }

      Seq(
        ("beer", "label.alcohol.beer", "beer"),
        ("non-sparkling-cider", "label.alcohol.non-sparkling-cider", "cider"),
        ("sparkling-cider", "label.alcohol.sparkling-cider", "cider"),
        ("sparkling-cider-up", "label.alcohol.sparkling-cider-up", "cider"),
        ("wine", "label.alcohol.wine", "wine"),
        ("spirits", "label.alcohol.spirits", "spirits"),
        ("other", "label.alcohol.other-alcohol", "other alcohol")
      ).foreach { case (item, productKey, panelMessage) =>
        panelIndentTests(item, productKey, panelMessage)
      }
    }

    "Tobacco" should {

      "display correct content for view" when {

        "the user enters too many cigarettes" should {

          val view = viewApply("801", "1", "cigarettes", "label.tobacco.cigarettes")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 801 cigarettes.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 800 cigarettes.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare tobacco over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your tobacco may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too many cigarillos" should {

          val view = viewApply("401", "1", "cigarillos", "label.tobacco.cigarillos")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 401 cigarillos.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 400 cigarillos.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare tobacco over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your tobacco may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too many cigars" should {

          val view = viewApply("201", "1", "cigars", "label.tobacco.cigars")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 201 cigars.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 200 cigars.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare tobacco over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your tobacco may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too many heated-tobacco" should {

          val view = viewApply("801", "1", "heated-tobacco", "label.tobacco.heated-tobacco")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 801 tobacco sticks.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 800 tobacco sticks.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare tobacco over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your tobacco may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much chewing-tobacco" should {

          val view = viewApply("1001", "1", "chewing-tobacco", "label.tobacco.chewing-tobacco")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 1001g of pipe or chewing tobacco.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 1000g of pipe or chewing tobacco.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare tobacco over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your tobacco may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much rolling-tobacco" should {

          val view = viewApply("1001", "1", "rolling-tobacco", "label.tobacco.rolling-tobacco")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 1001g of rolling tobacco.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 1000g of rolling tobacco.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare tobacco over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your tobacco may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much rolling-tobacco when chewing-tobacco has been previously added" should {

          val view = viewApply("1101", "1", "rolling-tobacco", "label.tobacco.rolling-tobacco", showGroupMessage = true)

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered a total of 1101g of loose tobacco.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 1000g of loose tobacco.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> ("Warning If you do not declare tobacco over the service limit in person, or " +
                "if you make a false declaration, you may have to pay a penalty and your tobacco may be seized."),
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        def panelIndentTests[A](amount: A, userInput: A, item: String, productKey: String, panelMessage: String): Unit =
          s"display the correct content when the showing the panel indent for: $item" when {

            s"the user enters too much $item" should {

              val view = viewApply(amount.toString, userInput.toString, item, productKey, showPanel = true)

              val expectedContent =
                Seq(
                  Selectors.panelIndent -> s"$userInput$panelMessage"
                )

              behave like pageWithExpectedMessages(view, expectedContent)
            }

          }

        Seq(
          (800, 1, "cigarettes", "label.tobacco.cigarettes", " cigarette"),
          (400, 1, "cigarillos", "label.tobacco.cigarillos", " cigarillo"),
          (200, 1, "cigars", "label.tobacco.cigars", " cigar"),
          (800, 1, "heated-tobacco", "label.tobacco.heated-tobacco", " tobacco stick")
        ).foreach { case (amount, userInput, item, productKey, panelMessage) =>
          panelIndentTests[Int](amount, userInput, item, productKey, panelMessage)
        }

        Seq(
          (1000.01, 300.57, "chewing-tobacco", "label.tobacco.chewing-tobacco", "g of pipe or chewing tobacco"),
          (1000.01, 300.57, "rolling-tobacco", "label.tobacco.rolling-tobacco", "g of rolling tobacco")
        ).foreach { case (amount, userInput, item, productKey, panelMessage) =>
          panelIndentTests[BigDecimal](amount, userInput, item, productKey, panelMessage)
        }
      }
    }
  }
}
