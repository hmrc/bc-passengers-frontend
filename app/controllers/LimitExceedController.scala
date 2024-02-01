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

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.LimitExceedAction
import models._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AlcoholAndTobaccoCalculationService, CalculatorService, ProductTreeService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.purchased_products.{limit_exceed_add, limit_exceed_edit}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

class LimitExceedController @Inject() (
  val cache: Cache,
  alcoholAndTobaccoCalculationService: AlcoholAndTobaccoCalculationService,
  val productTreeService: ProductTreeService,
  val calculatorService: CalculatorService,
  limitExceedAction: LimitExceedAction,
  val errorTemplate: views.html.errorTemplate,
  limitExceedViewAdd: limit_exceed_add,
  limitExceedViewEdit: limit_exceed_edit,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  val backLinkModel: BackLinkModel,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  def onPageLoadAddJourneyAlcoholVolume(path: ProductPath): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val userInput: Option[String]       = context.request.session.data.get(s"user-amount-input-${product.token}")
        val userInputBigDecimal: BigDecimal = userInput.map(s => BigDecimal(s)).getOrElse(0)
        val userInputBigDecimalFormatted    = userInputBigDecimal.setScale(2, RoundingMode.HALF_UP)

        val showPanelIndent: Boolean = context.getJourneyData.purchasedProductInstances.exists(_.path == path)

        val totalAccPreviouslyAddedVolume =
          alcoholAndTobaccoCalculationService.alcoholAddHelper(
            context.getJourneyData,
            BigDecimal(0),
            product.token
          )

        val totalAccNoOfVolume: BigDecimal = (totalAccPreviouslyAddedVolume + userInputBigDecimal).setScale(2, RoundingMode.HALF_UP)

        userInput match {
          case Some(_) =>
            Future(Ok(limitExceedViewAdd(totalAccNoOfVolume.toString(), userInputBigDecimalFormatted.toString(), product.token, product.name, showPanelIndent)))
          case _       =>
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadAddJourneyTobaccoWeight(path: ProductPath): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val userInput: Option[String]       = context.request.session.data.get(s"user-amount-input-${product.token}")
        val userInputBigDecimal: BigDecimal = userInput.map(s => BigDecimal(s)).getOrElse(0)
        val userInputBigDecimalFormatted =  (userInputBigDecimal * 1000).setScale(2, RoundingMode.HALF_UP)

        val totalAccWeightForTobaccoProduct =
          alcoholAndTobaccoCalculationService.looseTobaccoAddHelper(
            context.getJourneyData,
            None
          )

//        val showPanelIndent: Boolean = context.getJourneyData.purchasedProductInstances.nonEmpty
        val showPanelIndent: Boolean = context.getJourneyData.purchasedProductInstances.exists(_.path == path)

        val totalAccWeight =
          ((totalAccWeightForTobaccoProduct + userInputBigDecimal) * 1000).setScale(2, RoundingMode.HALF_UP)

        userInput match {
          case Some(_) =>
            Future(Ok(limitExceedViewAdd(totalAccWeight.toString(), userInputBigDecimalFormatted.toString, product.token, product.name, showPanelIndent)))
          case _       =>
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadAddJourneyNoOfSticks(path: ProductPath): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val userInput: Option[String] = context.request.session.data.get(s"user-amount-input-${product.token}")

        val userInputInt: Int = userInput.map(_.toInt).getOrElse(0)

        val showPanelIndent: Boolean = context.getJourneyData.purchasedProductInstances.exists(_.path == path)
//        val showPanelIndent: Boolean = context.getJourneyData.purchasedProductInstances.nonEmpty

        val totalAccPreviouslyAddedNoOfSticks =
          alcoholAndTobaccoCalculationService.noOfSticksTobaccoAddHelper(
            context.getJourneyData,
            None,
            product.token
          )

        val totalAccNoOfSticks: Int = totalAccPreviouslyAddedNoOfSticks + userInputInt

        userInput match {
          case Some(_) =>
            Future(Ok(limitExceedViewAdd(totalAccNoOfSticks.toString, userInputInt.toString, product.token, product.name, showPanelIndent)))
          case _       =>
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadEditAlcoholVolume(path: ProductPath): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val originalAmountEntered: BigDecimal =
          context.getJourneyData.workingInstance.flatMap(_.weightOrVolume).getOrElse(BigDecimal(0))

        val originalAmountFormatted = originalAmountEntered.setScale(2, RoundingMode.HALF_UP)

        val userInput: Option[String] = context.request.session.data.get(s"user-amount-input-${product.token}")

        val userInputBigDecimal: BigDecimal = userInput.map(s => BigDecimal(s)).getOrElse(0)

        val totalAccWeightForAlcoholProduct =
          alcoholAndTobaccoCalculationService.alcoholEditHelper(
            context.getJourneyData,
            userInputBigDecimal,
            product.token
          )

        val userInputBigDecimalFormatted = userInputBigDecimal.setScale(2, RoundingMode.HALF_UP)

        val totaledAmount: BigDecimal = totalAccWeightForAlcoholProduct

        val totaledAmountFormatted: BigDecimal = totaledAmount.setScale(2, RoundingMode.HALF_UP)

        userInput match {
          case Some(_) =>
            Future(
              Ok(
                limitExceedViewEdit(
                  totaledAmountFormatted.toString,
                  originalAmountFormatted.toString,
                  userInputBigDecimalFormatted.toString(),
                  product.token,
                  product.name
                )
              )
            )
          case _       =>
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadEditTobaccoWeight(path: ProductPath): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val originalAmountEntered: BigDecimal =
          context.getJourneyData.workingInstance.flatMap(_.weightOrVolume).getOrElse(BigDecimal(0))

        val originalAmountFormatted = (originalAmountEntered * 1000).setScale(2, RoundingMode.HALF_UP)

        val userInput: Option[String] = context.request.session.data.get(s"user-amount-input-${product.token}")

        val userInputBigDecimal: BigDecimal = userInput.map(s => BigDecimal(s)).getOrElse(0)

        val totalAccWeightForLooseTobacco =
          alcoholAndTobaccoCalculationService.looseTobaccoEditHelper(context.getJourneyData, Some(userInputBigDecimal))

        val userInputBigDecimalFormatted = (userInputBigDecimal * 1000).setScale(2, RoundingMode.HALF_UP)

        val totaledAmount: BigDecimal = totalAccWeightForLooseTobacco

        val totaledAmountFormatted: BigDecimal = (totaledAmount * 1000).setScale(2, RoundingMode.HALF_UP)

        userInput match {
          case Some(_) =>
            Future(
              Ok(
                limitExceedViewEdit(
                  totaledAmountFormatted.toString,
                  originalAmountFormatted.toString,
                  userInputBigDecimalFormatted.toString(),
                  product.token,
                  product.name
                )
              )
            )
          case _       =>
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadEditNoOfSticks(path: ProductPath): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>

        val originalAmountEntered: Int =
          context.getJourneyData.workingInstance.flatMap(_.noOfSticks).getOrElse(0)

        val userInput: Option[String] = context.request.session.data.get(s"user-amount-input-${product.token}")

        val userInputInt: Int = userInput.map(_.toInt).getOrElse(0)

        val totalAccNoOfSticks =
          alcoholAndTobaccoCalculationService.noOfSticksTobaccoEditHelper(
            context.getJourneyData,
            Some(userInputInt),
            product.token
          )

        userInput match {
          case Some(_) =>
            Future(
              Ok(
                limitExceedViewEdit(
                  totalAccNoOfSticks.toString,
                  originalAmountEntered.toString,
                  userInputInt.toString,
                  product.token,
                  product.name
                )
              )
            )
          case _       =>
            Future(InternalServerError(errorTemplate()))
        }
      }
    }
}
