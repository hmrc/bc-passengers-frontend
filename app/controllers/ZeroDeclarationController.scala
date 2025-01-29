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

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.ZeroDeclarationAction
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class ZeroDeclarationController @Inject() (
  val cache: Cache,
  val portsOfArrivalService: PortsOfArrivalService,
  val calculatorService: CalculatorService,
  val productTreeService: ProductTreeService,
  val declarationService: DeclarationService,
  zeroDeclarationAction: ZeroDeclarationAction,
  val errorTemplate: views.html.errorTemplate,
  val isZeroDeclarationPage: views.html.declaration.zero_declaration,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  def loadDeclarationPage(): Action[AnyContent] = zeroDeclarationAction { implicit context =>
    val chargeReference = context.getJourneyData.chargeReference.getOrElse("")
    declarationService.updateDeclaration(chargeReference).flatMap {
      case DeclarationServiceFailureResponse =>
        Future.successful(InternalServerError(errorTemplate()))
      case DeclarationServiceSuccessResponse =>
        val placeOfArrivalValue = portsOfArrivalService
          .getDisplayNameByCode(context.getJourneyData.buildUserInformation.get.selectPlaceOfArrival)
          .getOrElse(context.getJourneyData.buildUserInformation.get.enterPlaceOfArrival)

        requireCalculatorResponse { calculatorResponse =>
          val (previousDeclaration, deltaCalculation, oldTax) =
            context.getJourneyData.declarationResponse match {
              case Some(declarationResponse) =>
                (
                  true,
                  context.getJourneyData.deltaCalculation,
                  Some(declarationResponse.calculation.allTax)
                )
              case None                      => (false, None, None)
            }

          Future.successful(
            Ok(
              isZeroDeclarationPage(
                previousDeclaration,
                deltaCalculation,
                oldTax,
                context.getJourneyData.buildUserInformation,
                calculatorResponse,
                calculatorResponse.asDto(applySorting = false),
                chargeReference,
                placeOfArrivalValue
              )
            )
          )
        }
    }
  }
}
