/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

/*
 * Copyright 2020 HM Revenue & Customs
 *
 */
package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.UccReliefAction
import forms.UccReliefForm
import javax.inject.Inject
import models.JourneyData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UccReliefController @Inject()(
                                     val cache: Cache,
                                     uccReliefAction: UccReliefAction,
                                     val error_template: views.html.error_template,
                                     val isUccReliefPage: views.html.travel_details.ucc_relief,
                                     override val controllerComponents: MessagesControllerComponents,
                                     implicit val appConfig: AppConfig,
                                     val backLinkModel: BackLinkModel,
                                     val travelDetailsService: services.TravelDetailsService,
                                     implicit val ec: ExecutionContext
                                   ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadUccReliefPage: Action[AnyContent] = uccReliefAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _,_,_,_,_,Some(isUccRelief), _, _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(isUccReliefPage(UccReliefForm.form.fill(isUccRelief), backLinkModel.backLink))
        case _ =>
          Ok(isUccReliefPage(UccReliefForm.form, backLinkModel.backLink))
      }
    }
  }

  def postUccReliefPage(): Action[AnyContent] = uccReliefAction { implicit context =>
    UccReliefForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(isUccReliefPage(formWithErrors, backLinkModel.backLink))
          )
      },
      success = {
        isUccRelief =>
          travelDetailsService.storeUccRelief(context.journeyData)(isUccRelief).map(_ =>
            Redirect(routes.TravelDetailsController.goodsBoughtIntoNI())
          )
      })
  }

}

