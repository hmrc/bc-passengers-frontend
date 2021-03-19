/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DeclarationNotFoundAction, DeclarationRetrievalAction}
import models.{DeclarationRetrievalDto, PreviousDeclarationRequest}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationRetrievalController @Inject()(
   val cache: Cache,
   declarationRetrievalAction: DeclarationRetrievalAction,
   declarationNotFoundAction: DeclarationNotFoundAction,
   val error_template: views.html.error_template,
   val declarationRetrievalPage: views.html.amendments.declaration_retrieval,
   val declarationNotFoundPage: views.html.amendments.declaration_not_found,
   override val controllerComponents: MessagesControllerComponents,
   implicit val appConfig: AppConfig,
   val backLinkModel: BackLinkModel,
   val declaration_Not_Found: views.html.amendments.declaration_not_found,
   val travelDetailsService: services.TravelDetailsService,
   val previousDeclarationService: services.PreviousDeclarationService,
   implicit val ec: ExecutionContext
   ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadDeclarationRetrievalPage: Action[AnyContent] = declarationRetrievalAction { implicit context =>
    Future.successful {
      context.getJourneyData.previousDeclarationRequest match {
        case Some(previousDec) =>
          Ok(declarationRetrievalPage(DeclarationRetrievalDto.form().fill(DeclarationRetrievalDto.fromPreviousDeclarationDetails(previousDec)), backLinkModel.backLink))
        case _ =>
          Ok(declarationRetrievalPage(DeclarationRetrievalDto.form(), backLinkModel.backLink))
      }
    }
  }


  def postDeclarationRetrievalPage(): Action[AnyContent] = declarationRetrievalAction { implicit context =>
    DeclarationRetrievalDto.form().bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(declarationRetrievalPage(formWithErrors, backLinkModel.backLink))
          )
      },
      success = {
        previousDeclarationDetails =>
          previousDeclarationService.storePrevDeclarationDetails(context.journeyData)(PreviousDeclarationRequest.build(previousDeclarationDetails)).map(journeyData => journeyData.get.declarationResponse.isDefined match{
            case true => Redirect(routes.DashboardController.showDashboard())
            case false => Redirect(routes.DeclarationRetrievalController.declarationNotFound())}
          )
      })

  }


  val declarationNotFound: Action[AnyContent] = declarationNotFoundAction { implicit context =>
  Future.successful(Ok(declaration_Not_Found(backLinkModel.backLink)))}

}
