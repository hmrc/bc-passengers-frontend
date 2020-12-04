/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.PreviousDeclarationAction
import forms.PrevDeclarationForm
import javax.inject.Inject
import models.JourneyData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class PreviousDeclarationController @Inject()(
   val cache: Cache,
   previousDeclarationAction: PreviousDeclarationAction,
   val error_template: views.html.error_template,
   val previousDeclarationPage: views.html.amendments.previous_declaration,
   override val controllerComponents: MessagesControllerComponents,
   implicit val appConfig: AppConfig,
   val backLinkModel: BackLinkModel,
   val travelDetailsService: services.TravelDetailsService,
   implicit val ec: ExecutionContext
   ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadPreviousDeclarationPage: Action[AnyContent] = previousDeclarationAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData( Some(prevDeclaration), _, _, _, _,_,_, _, _, _, _, _, _, _, _, _, _, _, _ ,_, _)) =>
          Ok(previousDeclarationPage(PrevDeclarationForm.validateForm().fill(prevDeclaration), backLinkModel.backLink))
        case _ =>
          Ok(previousDeclarationPage(PrevDeclarationForm.validateForm(), backLinkModel.backLink))
      }
    }
  }

  def postPreviousDeclarationPage(): Action[AnyContent] = previousDeclarationAction { implicit context =>
    PrevDeclarationForm.validateForm().bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(previousDeclarationPage(formWithErrors, backLinkModel.backLink))
          )
      },
      success = {
        prevDeclaration =>
          travelDetailsService.storePrevDeclaration(context.journeyData)(prevDeclaration).map(_ =>
            Redirect(routes.TravelDetailsController.whereGoodsBought()))
      })
  }

}
