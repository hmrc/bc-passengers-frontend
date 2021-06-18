/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{NoFurtherAmendmentAction, PendingPaymentAction}
import forms.PendingPaymentForm
import javax.inject.Inject
import models.JourneyData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculatorService, CalculatorServiceSuccessResponse, ProductTreeService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class PendingPaymentController @Inject()(
   val cache: Cache,
   val calculatorService: CalculatorService,
   val productTreeService: ProductTreeService,
   val calculateDeclareController: controllers.CalculateDeclareController,
   noFurtherAmendmentAction: NoFurtherAmendmentAction,
   pendingPaymentAction: PendingPaymentAction,
   val error_template: views.html.error_template,
   val noFurtherAmendmentPage: views.html.amendments.no_further_amendment,
   val pendingPaymentPage: views.html.amendments.pending_payment,

   override val controllerComponents: MessagesControllerComponents,
   implicit val appConfig: AppConfig,
   val backLinkModel: BackLinkModel,
   implicit val ec: ExecutionContext
   ) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers  {

  val loadPendingPaymentPage: Action[AnyContent] = pendingPaymentAction { implicit context =>
    calculatorService.calculate(context.getJourneyData) flatMap {
      case CalculatorServiceSuccessResponse(calculatorResponse) =>
        val previousPaidCalculation = calculatorService.getPreviousPaidCalculation(context.getJourneyData.deltaCalculation.get, calculatorResponse.calculation)
        calculatorService.storeCalculatorResponse(context.getJourneyData, calculatorResponse, context.getJourneyData.deltaCalculation) map { _ =>
            context.journeyData match {
              case Some(JourneyData(_, _, _, _, _,_,_, _, _, _, _, _, _, _, _, _, _, _, _ ,_, _,_,_,_,_,_, Some(pendingPayment),_)) =>
                Ok(pendingPaymentPage(PendingPaymentForm.form.fill(pendingPayment), calculatorResponse.asDto(applySorting = false), context.getJourneyData.deltaCalculation, previousPaidCalculation.allTax, backLinkModel.backLink))
              case _ =>
                Ok(pendingPaymentPage(PendingPaymentForm.form, calculatorResponse.asDto(applySorting = false), context.getJourneyData.deltaCalculation, previousPaidCalculation.allTax, backLinkModel.backLink))
          }
        }
      case _ =>
        Future.successful {
          InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem."))
        }
    }
  }

  val postPendingPaymentPage: Action[AnyContent] = pendingPaymentAction { implicit context =>
    requireCalculatorResponse { calculatorResponse =>
      PendingPaymentForm.form.bindFromRequest().fold(
        hasErrors = {
          formWithErrors =>
            Future.successful(
              BadRequest(pendingPaymentPage(formWithErrors, calculatorResponse.asDto(applySorting = true), context.getJourneyData.deltaCalculation, calculatorService.getPreviousPaidCalculation(context.getJourneyData.deltaCalculation.get, calculatorResponse.calculation).allTax, backLinkModel.backLink))
            )
        },
        success = {
          pendingPayment =>
            cache.storeJourneyData(context.getJourneyData.copy(pendingPayment = Some(pendingPayment))).map(_ =>
              pendingPayment match {
                case true => Redirect(routes.CalculateDeclareController.processAmendment())
                case false => Redirect(routes.PendingPaymentController.noFurtherAmendment())
              }
            )
        })
    }
  }

  val noFurtherAmendment: Action[AnyContent] = noFurtherAmendmentAction { implicit context =>
    Future.successful(Ok(noFurtherAmendmentPage(backLinkModel.backLink)))}

}
