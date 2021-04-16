/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.ZeroDeclarationAction
import javax.inject.Inject
import models.Calculation
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class ZeroDeclarationController @Inject()(
   val cache: Cache,
   val portsOfArrivalService: PortsOfArrivalService,
   val calculatorService: CalculatorService,
   val productTreeService: ProductTreeService,
   val declarationService: DeclarationService,
   zeroDeclarationAction: ZeroDeclarationAction,
   val error_template: views.html.error_template,
   val isZeroDeclarationPage: views.html.declaration.zero_declaration,
   override val controllerComponents: MessagesControllerComponents,
   implicit val appConfig: AppConfig,
   implicit val ec: ExecutionContext
  ) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def loadDeclarationPage(): Action[AnyContent] = zeroDeclarationAction { implicit context =>

    val chargeReference = context.getJourneyData.chargeReference.getOrElse("")

    declarationService.updateDeclaration(chargeReference) flatMap {
      case DeclarationServiceFailureResponse =>
        Future.successful(InternalServerError(error_template("Sorry, we are experiencing technical difficulties - 500", "Sorry, we are experiencing technical difficulties", "Please try again in a few minutes")))

      case DeclarationServiceSuccessResponse =>
        val placeOfArrivalValue = portsOfArrivalService.getDisplayNameByCode(context.getJourneyData.userInformation.get.selectPlaceOfArrival).getOrElse(context.getJourneyData.userInformation.get.enterPlaceOfArrival)

        requireCalculatorResponse { calculatorResponse =>

          Future.successful {
            if(context.getJourneyData.declarationResponse.isDefined) {
              val deltaCalculation: Option[Calculation] = context.getJourneyData.deltaCalculation
              val oldTax = context.getJourneyData.declarationResponse.get.calculation.allTax
              Ok(isZeroDeclarationPage(true, deltaCalculation, Some(oldTax), context.getJourneyData.userInformation, calculatorResponse, calculatorResponse.asDto(applySorting = false), chargeReference, placeOfArrivalValue))
            } else {
              Ok(isZeroDeclarationPage(false, None, None, context.getJourneyData.userInformation, calculatorResponse, calculatorResponse.asDto(applySorting = false), chargeReference, placeOfArrivalValue))
            }
          }
        }
    }

  }

}

