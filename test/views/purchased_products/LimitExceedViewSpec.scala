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

package views.purchased_products

import play.twirl.api.HtmlFormat
import views.html.purchased_products.limit_exceed
import views.{BaseSelectors, BaseViewSpec}

class LimitExceedViewSpec extends BaseViewSpec {

  val viewViaApply: HtmlFormat.Appendable =
    injected[limit_exceed].apply(
      "110",
      "cigars",
      "label.tobacco.cigars"
    )(
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val viewViaRender: HtmlFormat.Appendable =
    injected[limit_exceed].render(
      "110.2",
      "cigars",
      "label.tobacco.cigars",
      request = request,
      messages = messages,
      appConfig = appConfig
    )

  val viewViaF: HtmlFormat.Appendable =
    injected[limit_exceed].f("110.2", "cigars", "label.tobacco.cigars")(request, messages, appConfig)

  object Selectors extends BaseSelectors

  def viewApply(amount: String, item: String, productName: String): HtmlFormat.Appendable =
    injected[limit_exceed]
      .apply(amount, item, productName)(request = request, messages = messages, appConfig = appConfig)

  "LimitExceedView" when {

    renderViewTest(
      title = "There is a problem - Check tax on goods you bring into the UK - GOV.UK",
      heading = "There is a problem"
    )

    "Alcohol" should {

      "display correct content for view" when {

        "the user enters too much beer" should {

          val view = viewApply("110.5", "beer", "label.alcohol.beer")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered 110.5 litres of beer.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 110 litres of beer.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> "Warning If you do not declare alcohol over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your alcohol may be seized.",
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much non-sparkling cider" should {

          val view = viewApply("20.01", "non-sparkling-cider", "label.alcohol.non-sparkling-cider")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered 20.01 litres of cider.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 20 litres of cider.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> "Warning If you do not declare alcohol over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your alcohol may be seized.",
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much sparkling-cider" should {

          val view = viewApply("20.01", "sparkling-cider", "label.alcohol.sparkling-cider")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered 20.01 litres of cider.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 20 litres of cider.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> "Warning If you do not declare alcohol over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your alcohol may be seized.",
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much sparkling-cider-up" should {

          val view = viewApply("20.01", "sparkling-cider-up", "label.alcohol.sparkling-cider-up")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered 20.01 litres of cider.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 20 litres of cider.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> "Warning If you do not declare alcohol over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your alcohol may be seized.",
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much spirits" should {

          val view = viewApply("10.01", "spirits", "label.alcohol.spirits")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered 10.01 litres of spirits.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 10 litres of spirits.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> "Warning If you do not declare alcohol over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your alcohol may be seized.",
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much wine" should {

          val view = viewApply("90.01", "wine", "label.alcohol.wine")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered 90.01 litres of wine.",
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
              Selectors.warning -> "Warning If you do not declare alcohol over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your alcohol may be seized.",
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }

        "the user enters too much other alcohol" should {

          val view = viewApply("20.01", "other", "label.alcohol.other")

          val expectedContent =
            Seq(
              Selectors.p(1)    -> "You have entered 20.01 litres of other alcohol.",
              Selectors.p(2)    -> "You cannot use this service to declare more than 20 litres of other alcohol.",
              Selectors.p(3)    -> "This item will be removed from your goods to declare.",
              Selectors.h2(1)   -> "What you must do",
              Selectors.p(4)    ->
                (
                  "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                    "They will calculate and take payment of the taxes and duties due."
                ),
              Selectors.warning -> "Warning If you do not declare alcohol over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your alcohol may be seized.",
              Selectors.h2(2)   -> "If you have other items to declare",
              Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
            )

          behave like pageWithExpectedMessages(view, expectedContent)
        }
      }
    }

    "Tobacco" should {

      "the user enters too many cigarettes" should {

        val view = viewApply("801", "cigarettes", "label.tobacco.cigarettes")

        val expectedContent =
          Seq(
            Selectors.p(1)    -> "You have entered 801 cigarettes.",
            Selectors.p(2)    -> "You cannot use this service to declare more than 800 cigarettes.",
            Selectors.p(3)    -> "This item will be removed from your goods to declare.",
            Selectors.h2(1)   -> "What you must do",
            Selectors.p(4)    ->
              (
                "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                  "They will calculate and take payment of the taxes and duties due."
              ),
            Selectors.warning -> "Warning If you do not declare tobacco over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your tobacco may be seized.",
            Selectors.h2(2)   -> "If you have other items to declare",
            Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
          )

        behave like pageWithExpectedMessages(view, expectedContent)
      }

      "the user enters too many cigarillos" should {

        val view = viewApply("401", "cigarillos", "label.tobacco.cigarillos")

        val expectedContent =
          Seq(
            Selectors.p(1)    -> "You have entered 401 cigarillos.",
            Selectors.p(2)    -> "You cannot use this service to declare more than 400 cigarillos.",
            Selectors.p(3)    -> "This item will be removed from your goods to declare.",
            Selectors.h2(1)   -> "What you must do",
            Selectors.p(4)    ->
              (
                "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                  "They will calculate and take payment of the taxes and duties due."
              ),
            Selectors.warning -> "Warning If you do not declare tobacco over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your tobacco may be seized.",
            Selectors.h2(2)   -> "If you have other items to declare",
            Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
          )

        behave like pageWithExpectedMessages(view, expectedContent)
      }

      "the user enters too many cigars" should {

        val view = viewApply("201", "cigars", "label.tobacco.cigars")

        val expectedContent =
          Seq(
            Selectors.p(1)    -> "You have entered 201 cigars.",
            Selectors.p(2)    -> "You cannot use this service to declare more than 200 cigars.",
            Selectors.p(3)    -> "This item will be removed from your goods to declare.",
            Selectors.h2(1)   -> "What you must do",
            Selectors.p(4)    ->
              (
                "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                  "They will calculate and take payment of the taxes and duties due."
              ),
            Selectors.warning -> "Warning If you do not declare tobacco over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your tobacco may be seized.",
            Selectors.h2(2)   -> "If you have other items to declare",
            Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
          )

        behave like pageWithExpectedMessages(view, expectedContent)
      }

      "the user enters much heated-tobacco" should {

        val view = viewApply("801", "heated-tobacco", "label.tobacco.heated-tobacco")

        val expectedContent =
          Seq(
            Selectors.p(1)    -> "You have entered 801 tobacco sticks.",
            Selectors.p(2)    -> "You cannot use this service to declare more than 800 tobacco sticks.",
            Selectors.p(3)    -> "This item will be removed from your goods to declare.",
            Selectors.h2(1)   -> "What you must do",
            Selectors.p(4)    ->
              (
                "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                  "They will calculate and take payment of the taxes and duties due."
              ),
            Selectors.warning -> "Warning If you do not declare tobacco over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your tobacco may be seized.",
            Selectors.h2(2)   -> "If you have other items to declare",
            Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
          )

        behave like pageWithExpectedMessages(view, expectedContent)
      }

      "the user enters much chewing-tobacco" should {

        val view = viewApply("1001", "chewing-tobacco", "label.tobacco.chewing-tobacco")

        val expectedContent =
          Seq(
            Selectors.p(1)    -> "You have entered 1001 grams of tobacco.",
            Selectors.p(2)    -> "You cannot use this service to declare more than 1000 grams of tobacco.",
            Selectors.p(3)    -> "This item will be removed from your goods to declare.",
            Selectors.h2(1)   -> "What you must do",
            Selectors.p(4)    ->
              (
                "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                  "They will calculate and take payment of the taxes and duties due."
              ),
            Selectors.warning -> "Warning If you do not declare tobacco over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your tobacco may be seized.",
            Selectors.h2(2)   -> "If you have other items to declare",
            Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
          )

        behave like pageWithExpectedMessages(view, expectedContent)
      }

      "the user enters much rolling-tobacco" should {

        val view = viewApply("1001", "rolling-tobacco", "label.tobacco.rolling-tobacco")

        val expectedContent =
          Seq(
            Selectors.p(1)    -> "You have entered 1001 grams of tobacco.",
            Selectors.p(2)    -> "You cannot use this service to declare more than 1000 grams of tobacco.",
            Selectors.p(3)    -> "This item will be removed from your goods to declare.",
            Selectors.h2(1)   -> "What you must do",
            Selectors.p(4)    ->
              (
                "You must use the red channel to declare this item in person to Border Force when you arrive in the UK. " +
                  "They will calculate and take payment of the taxes and duties due."
              ),
            Selectors.warning -> "Warning If you do not declare tobacco over the service limit in person, or if you make a false declaration, you may have to pay a penalty and your tobacco may be seized.",
            Selectors.h2(2)   -> "If you have other items to declare",
            Selectors.p(5)    -> "You can continue to use this service to declare other alcohol, tobacco and goods."
          )

        behave like pageWithExpectedMessages(view, expectedContent)
      }
    }
  }
}
