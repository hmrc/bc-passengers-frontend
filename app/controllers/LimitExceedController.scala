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
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AlcoholAndTobaccoCalculationService, CalculatorService, ProductTreeService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{FormatsAndConversions, InstanceDecider, ProductDetector}
import views.html.purchased_products.{limit_exceed_add, limit_exceed_edit}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

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
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with ControllerHelpers
    with InstanceDecider
    with ProductDetector
    with FormatsAndConversions {

  private val logger: Logger = Logger(this.getClass)

  private def showAlcoholGroupMessage(journeyData: JourneyData, productToken: String): Boolean = productToken match {
    case token if token == "wine"           => checkProductExists(journeyData, "alcohol/sparkling-wine")
    case token if token == "sparkling-wine" => checkProductExists(journeyData, "alcohol/wine")
    case token if token == "other"          => checkProductExists(journeyData, "cider")
    case token if token.contains("cider")   => checkProductExists(journeyData, "other")
    case _                                  => false
  }

  private def showLooseTobaccoGroupMessage(journeyData: JourneyData, productToken: String): Boolean =
    if (productToken == "chewing-tobacco") {
      checkProductExists(journeyData, "rolling-tobacco")
    } else {
      checkProductExists(journeyData, "chewing-tobacco")
    }

  def onPageLoadAddJourneyAlcoholVolume(path: ProductPath): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val userInput: Option[String]       = context.request.session.data.get(s"user-amount-input-${product.token}")
        val userInputBigDecimal: BigDecimal = userInput.map(s => BigDecimal(s)).getOrElseZero
        val userInputBigDecimalFormatted    = userInputBigDecimal.formatDecimalPlaces(3)

        val totalAccPreviouslyAddedVolume =
          alcoholAndTobaccoCalculationService.alcoholAddHelper(
            context.getJourneyData,
            BigDecimal(0),
            product.token
          )

        val totalAccNoOfVolume: BigDecimal =
          (totalAccPreviouslyAddedVolume + userInputBigDecimal).formatDecimalPlaces(3)

        val showPanelIndent: Boolean = checkAlcoholProductExists(
          productToken = product.token,
          wineOrSparklingExists = checkProductExists(context.getJourneyData, "wine"),
          ciderOrOtherAlcoholExists =
            checkProductExists(context.getJourneyData, "cider") || checkProductExists(context.getJourneyData, "other"),
          beerOrSpiritExists = checkProductExists(context.getJourneyData, path.toString)
        )

        val showGroupMessage: Boolean = showAlcoholGroupMessage(context.getJourneyData, product.token)

        userInput match {
          case Some(_) =>
            Future(
              Ok(
                limitExceedViewAdd(
                  totalAccNoOfVolume.stripTrailingZerosToString,
                  userInputBigDecimalFormatted.stripTrailingZerosToString,
                  product.token,
                  product.name,
                  showPanelIndent,
                  showGroupMessage
                )
              )
            )
          case _       =>
            logger.error("[LimitExceedController][onPageLoadAddJourneyAlcoholVolume] no user input found in session")
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadAddJourneyTobaccoWeight(path: ProductPath): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val userInput: Option[String]       = context.request.session.data.get(s"user-amount-input-${product.token}")
        val userInputBigDecimal: BigDecimal = userInput.map(s => BigDecimal(s)).getOrElseZero
        val userInputBigDecimalFormatted    = (userInputBigDecimal * 1000).formatDecimalPlaces(2)

        val totalAccWeightForTobaccoProduct =
          alcoholAndTobaccoCalculationService.looseTobaccoAddHelper(
            context.getJourneyData,
            None
          )

        val totalAccWeight =
          ((totalAccWeightForTobaccoProduct + userInputBigDecimal) * 1000).formatDecimalPlaces(2)

        val showPanelIndent: Boolean = checkProductExists(context.getJourneyData, "chewing-tobacco") ||
          checkProductExists(context.getJourneyData, "rolling-tobacco")

        val showGroupMessage: Boolean = showLooseTobaccoGroupMessage(context.getJourneyData, product.token)

        userInput match {
          case Some(_) =>
            Future(
              Ok(
                limitExceedViewAdd(
                  totalAccWeight.stripTrailingZerosToString,
                  userInputBigDecimalFormatted.stripTrailingZerosToString,
                  product.token,
                  product.name,
                  showPanelIndent,
                  showGroupMessage
                )
              )
            )
          case _       =>
            logger.error("[LimitExceedController][onPageLoadAddJourneyTobaccoWeight] no user input found in session")
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

        val totalAccPreviouslyAddedNoOfSticks =
          alcoholAndTobaccoCalculationService.noOfSticksTobaccoAddHelper(
            context.getJourneyData,
            None,
            product.token
          )

        val totalAccNoOfSticks: Int = totalAccPreviouslyAddedNoOfSticks + userInputInt

        userInput match {
          case Some(_) =>
            Future(
              Ok(
                limitExceedViewAdd(
                  totalAccNoOfSticks.toString,
                  userInputInt.toString,
                  product.token,
                  product.name,
                  showPanelIndent
                )
              )
            )
          case _       =>
            logger.error("[LimitExceedController][onPageLoadAddJourneyNoOfSticks] no user input found in session")
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadEditAlcoholVolume(path: ProductPath, iid: String): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val originalAmountEntered: BigDecimal = originalAmountEnteredWeightOrVolume(context.getJourneyData, iid)

        val originalAmountFormatted = originalAmountEntered.formatDecimalPlaces(3)

        val userInput: Option[String] = context.request.session.data.get(s"user-amount-input-${product.token}")

        val userInputBigDecimal: BigDecimal = userInput.map(s => BigDecimal(s)).getOrElseZero

        val totalAccWeightForAlcoholProduct =
          alcoholAndTobaccoCalculationService.alcoholEditHelper(
            context.getJourneyData,
            userInputBigDecimal,
            product.token,
            iid
          )

        val userInputBigDecimalFormatted = userInputBigDecimal.formatDecimalPlaces(3)

        val totaledAmount: BigDecimal = totalAccWeightForAlcoholProduct

        val totaledAmountFormatted: BigDecimal = totaledAmount.formatDecimalPlaces(3)

        val showGroupMessage: Boolean = showAlcoholGroupMessage(context.getJourneyData, product.token)

        userInput match {
          case Some(_) =>
            Future(
              Ok(
                limitExceedViewEdit(
                  totalEnteredAmount = totaledAmountFormatted.stripTrailingZerosToString,
                  originalAmountEntered = originalAmountFormatted.stripTrailingZerosToString,
                  userInput = userInputBigDecimalFormatted.stripTrailingZerosToString,
                  token = product.token,
                  productName = product.name,
                  showGroupMessage = showGroupMessage
                )
              )
            )
          case _       =>
            logger.error("[LimitExceedController][onPageLoadEditAlcoholVolume] no user input found in session")
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadEditTobaccoWeight(path: ProductPath, iid: String): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val originalAmountEntered: BigDecimal = originalAmountEnteredWeightOrVolume(context.getJourneyData, iid)

        val originalAmountFormatted = (originalAmountEntered * 1000).formatDecimalPlaces(2)

        val userInput: Option[String] = context.request.session.data.get(s"user-amount-input-${product.token}")

        val userInputBigDecimal: BigDecimal = userInput.map(s => BigDecimal(s)).getOrElse(0)

        val totalAccWeightForLooseTobacco =
          alcoholAndTobaccoCalculationService.looseTobaccoEditHelper(
            context.getJourneyData,
            Some(userInputBigDecimal),
            iid
          )

        val userInputBigDecimalFormatted = (userInputBigDecimal * 1000).formatDecimalPlaces(2)

        val totaledAmount: BigDecimal = totalAccWeightForLooseTobacco

        val totaledAmountFormatted: BigDecimal = (totaledAmount * 1000).formatDecimalPlaces(2)

        val showGroupMessage: Boolean = showLooseTobaccoGroupMessage(context.getJourneyData, product.token)

        userInput match {
          case Some(_) =>
            Future(
              Ok(
                limitExceedViewEdit(
                  totaledAmountFormatted.stripTrailingZerosToString,
                  originalAmountFormatted.stripTrailingZerosToString,
                  userInputBigDecimalFormatted.stripTrailingZerosToString,
                  product.token,
                  product.name,
                  showGroupMessage
                )
              )
            )
          case _       =>
            logger.error("[LimitExceedController][onPageLoadEditTobaccoWeight] no user input found in session")
            Future(InternalServerError(errorTemplate()))
        }
      }
    }

  def onPageLoadEditNoOfSticks(path: ProductPath, iid: String): Action[AnyContent] =
    limitExceedAction { implicit context =>
      requireProduct(path) { product =>
        val originalAmountEntered: Int = originalAmountEnteredNoOfSticks(context.getJourneyData, iid)

        val userInput: Option[String] = context.request.session.data.get(s"user-amount-input-${product.token}")

        val userInputInt: Int = userInput.map(_.toInt).getOrElse(0)

        val totalAccNoOfSticks: Int =
          alcoholAndTobaccoCalculationService.noOfSticksTobaccoEditHelper(
            context.getJourneyData,
            Some(userInputInt),
            product.token,
            iid
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
            logger.error("[LimitExceedController][onPageLoadEditNoOfSticks] no user input found in session")
            Future(InternalServerError(errorTemplate()))
        }
      }
    }
}
