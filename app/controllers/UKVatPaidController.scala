/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.UKVatPaidAction
import forms.UKVatPaidForm
import javax.inject.Inject
import models.JourneyData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController


import scala.concurrent.{ExecutionContext, Future}

class UKVatPaidController @Inject()(
                                      val cache: Cache,
                                      uKVatPaidAction: UKVatPaidAction,
                                      val error_template: views.html.error_template,
                                      val isUKVatPaidPage: views.html.travel_details.ukvat_paid,
                                      override val controllerComponents: MessagesControllerComponents,
                                      implicit val appConfig: AppConfig,
                                      val backLinkModel: BackLinkModel,
                                      val travelDetailsService: services.TravelDetailsService,
                                      implicit val ec: ExecutionContext
                                    ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadUKVatPaidPage: Action[AnyContent] = uKVatPaidAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _,_,Some(isUKVatPaid),_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _,_)) =>
          Ok(isUKVatPaidPage(UKVatPaidForm.form.fill(isUKVatPaid), backLinkModel.backLink))
        case _ =>
          Ok(isUKVatPaidPage(UKVatPaidForm.form, backLinkModel.backLink))
      }
    }
  }

  def postUKVatPaidPage(): Action[AnyContent] = uKVatPaidAction { implicit context =>
    UKVatPaidForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(isUKVatPaidPage(formWithErrors, backLinkModel.backLink))
          )
      },
      success = {
        isUKVatPaid =>
          travelDetailsService.storeUKVatPaid(context.journeyData)(isUKVatPaid).map(_ =>
            Redirect(routes.UKExcisePaidController.loadUKExcisePaidPage())
          )
      })
  }

}

