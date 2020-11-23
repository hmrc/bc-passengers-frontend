/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.ZeroDeclarationAction
import javax.inject.Inject
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculatorService, PortsOfArrivalService, ProductTreeService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class ZeroDeclarationController @Inject()(
   val cache: Cache,
   val portsOfArrivalService: PortsOfArrivalService,
   val calculatorService: CalculatorService,
   val productTreeService: ProductTreeService,
   zeroDeclarationAction: ZeroDeclarationAction,
   val error_template: views.html.error_template,
   val isZeroDeclarationPage: views.html.declaration.zero_declaration,
   override val controllerComponents: MessagesControllerComponents,
   implicit val appConfig: AppConfig,
   implicit val ec: ExecutionContext
 ) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers{

  def loadDeclarationPage(): Action[AnyContent] = zeroDeclarationAction { implicit context =>

    val placeOfArrivalValue = portsOfArrivalService.getDisplayNameByCode(context.getJourneyData.userInformation.get.selectPlaceOfArrival).getOrElse(context.getJourneyData.userInformation.get.enterPlaceOfArrival)

    requireCalculatorResponse { calculatorResponse =>
      Future.successful {
        Ok(isZeroDeclarationPage(context.getJourneyData.userInformation, calculatorResponse, calculatorResponse.asDto(applySorting = false), context.getJourneyData.chargeReference.getOrElse(""), placeOfArrivalValue))

      }
    }
  }

}

