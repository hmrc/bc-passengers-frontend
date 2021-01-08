/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.DeclarationRetrievalAction
import models.{DeclarationRetrievalDto, PreviousDeclarationDetails}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationRetrievalController @Inject()(
   val cache: Cache,
   declarationRetrievalAction: DeclarationRetrievalAction,
   val error_template: views.html.error_template,
   val declarationRetrievalPage: views.html.amendments.declaration_retrieval,
   override val controllerComponents: MessagesControllerComponents,
   implicit val appConfig: AppConfig,
   val backLinkModel: BackLinkModel,
   val travelDetailsService: services.TravelDetailsService,
   implicit val ec: ExecutionContext
   ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadDeclarationRetrievalPage: Action[AnyContent] = declarationRetrievalAction { implicit context =>
    Future.successful {
      context.getJourneyData.previousDeclarationDetails match {
        case Some(previousDec) =>
          Ok(declarationRetrievalPage(DeclarationRetrievalDto.form().fill(DeclarationRetrievalDto.fromPreviousDeclarationDetails(previousDec))))
        case _ =>
          Ok(declarationRetrievalPage(DeclarationRetrievalDto.form()))
      }
    }
  }

  def postDeclarationRetrievalPage(): Action[AnyContent] = declarationRetrievalAction { implicit context =>
    DeclarationRetrievalDto.form().bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(declarationRetrievalPage(formWithErrors))
          )
      },
      success = {
        previousDeclarationDetails =>
          travelDetailsService.storePrevDeclarationDetails(context.journeyData)(PreviousDeclarationDetails.build(previousDeclarationDetails)).map(_ =>
            Redirect(routes.TravelDetailsController.whereGoodsBought())
          )
      })
  }

}
