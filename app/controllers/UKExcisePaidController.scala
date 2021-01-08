/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.UKExcisePaidAction
import forms.UKExcisePaidForm
import javax.inject.Inject
import models.JourneyData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UKExcisePaidController @Inject()(
                                     val cache: Cache,
                                     uKExcisePaidAction: UKExcisePaidAction,
                                     val error_template: views.html.error_template,
                                     val isUKExcisePaidPage: views.html.travel_details.ukexcise_paid,
                                     override val controllerComponents: MessagesControllerComponents,
                                     implicit val appConfig: AppConfig,
                                     val backLinkModel: BackLinkModel,
                                     val travelDetailsService: services.TravelDetailsService,
                                     implicit val ec: ExecutionContext
                                   ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadUKExcisePaidPage: Action[AnyContent] = uKExcisePaidAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_,_, _, _,Some(isUKExcisePaid),_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _,_)) =>
          Ok(isUKExcisePaidPage(UKExcisePaidForm.form.fill(isUKExcisePaid), backLinkModel.backLink))
        case _ =>
          Ok(isUKExcisePaidPage(UKExcisePaidForm.form, backLinkModel.backLink))
      }
    }
  }

  def postUKExcisePaidPage(): Action[AnyContent] = uKExcisePaidAction { implicit context =>
    UKExcisePaidForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(isUKExcisePaidPage(formWithErrors, backLinkModel.backLink))
          )
      },
      success = {
        isUKExcisePaid =>
          travelDetailsService.storeUKExcisePaid(context.journeyData)(isUKExcisePaid).map(_ =>
            Redirect(routes.UKResidentController.loadUKResidentPage())
          )
      })
  }

}
