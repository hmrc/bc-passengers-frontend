package controllers.enforce

import config.AppConfig
import connectors.Cache
import controllers.{LocalContext, routes}
import javax.inject.{Inject, Singleton}
import models.JourneyData
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc.{Action, AnyContent, AnyContentAsFormUrlEncoded, Call, DefaultActionBuilder, Request, RequestHeader, Result}
import controllers.enforce._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class JourneyEnforcer {
  def logAndRedirect(logMessage: String, redirectLocation: Call)(implicit context: LocalContext): Future[Result] = {
    Logger.debug(logMessage)
    Future.successful(Redirect(redirectLocation))
  }

  def apply(q: JourneyStep*)(block: => Future[Result])(implicit context: LocalContext): Future[Result] = if(q.exists(_.meetsAllPrerequisites)) block else redirect

  def redirect(implicit context: LocalContext): Future[Result] =
    logAndRedirect(s"Enforcer prerequisites not met for ${context.request.path}, redirecting", routes.TravelDetailsController.whereGoodsBought())
}

@Singleton
class WhereGoodsBoughtEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    val step = if(appConfig.isVatResJourneyEnabled) vatres.WhereGoodsBoughtStep else nonvatres.WhereGoodsBoughtStep
    journeyEnforcer(step)(block)
  }
}

@Singleton
class GoodsBoughtInAndOutEuEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    val step = if(appConfig.isVatResJourneyEnabled) vatres.GoodsBoughtInAndOutEuStep else nonvatres.GoodsBoughtInAndOutEuStep
    journeyEnforcer(step)(block)
  }
}

@Singleton
class GoodsBoughtOutsideEuEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    val step = if(appConfig.isVatResJourneyEnabled) vatres.GoodsBoughtOutsideEuStep else nonvatres.GoodsBoughtOutsideEuStep
    journeyEnforcer(step)(block)
  }
}

@Singleton
class GoodsBoughtInsideEuEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    val step = if(appConfig.isVatResJourneyEnabled) vatres.GoodsBoughtInsideEuStep else nonvatres.GoodsBoughtInsideEuStep
    journeyEnforcer(step)(block)
  }
}

@Singleton
class NoNeedToUseServiceEnforcer @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    val step = if(appConfig.isVatResJourneyEnabled) vatres.NoNeedToUseStep else nonvatres.NoNeedToUseStep
    journeyEnforcer(step)(block)
  }
}

@Singleton
class PrivateCraftEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    val step = if(appConfig.isVatResJourneyEnabled) vatres.PrivateCraftStep else nonvatres.PrivateCraftStep
    journeyEnforcer(step)(block)
  }
}

@Singleton
class Is17OrOverEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    val step = if(appConfig.isVatResJourneyEnabled) vatres.Is17OrOverStep else nonvatres.Is17OrOverStep
    journeyEnforcer(step)(block)
  }
}

@Singleton
class DashboardEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    val step = if(appConfig.isVatResJourneyEnabled) vatres.DashboardStep else nonvatres.DashboardStep
    journeyEnforcer(step)(block)
  }
}

@Singleton
class DidYouClaimTaxBackEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    journeyEnforcer( vatres.DidYouClaimTaxBackEuOnlyStep, vatres.DidYouClaimTaxBackBothStep )(block)
  }
}

@Singleton
class BringingDutyFreeEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    journeyEnforcer( vatres.BringingDutyFreeEuStep, vatres.BringingDutyFreeBothStep )(block)
  }
}

@Singleton
class DeclareDutyFreeMixEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    journeyEnforcer( vatres.DeclareDutyFreeMixStep )(block)
  }
}

@Singleton
class DeclareDutyFreeEuEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    journeyEnforcer( vatres.DeclareDutyFreeEuStep )(block)
  }
}

@Singleton
class DeclareDutyFreeAnyEnforcer @Inject() ( journeyEnforcer: JourneyEnforcer, appConfig: AppConfig ) {

  def apply(block: => Future[Result])(implicit context: LocalContext): Future[Result] = {
    journeyEnforcer( vatres.DeclareDutyFreeEuStep, vatres.DeclareDutyFreeMixStep )(block)
  }
}

@Singleton
class PublicAction @Inject() (cache: Cache, actionBuilder: DefaultActionBuilder) {

  private def trimmingFormUrlEncodedData(block: Request[AnyContent] => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    block {
      request.map {
        case AnyContentAsFormUrlEncoded(data) =>
          AnyContentAsFormUrlEncoded(data.map {
            case (key, vals) => (key, vals.map(_.trim))
          })
        case b => b
      }
    }
  }

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {

    actionBuilder.async { implicit request =>

      trimmingFormUrlEncodedData { implicit request =>

        request.session.get(SessionKeys.sessionId) match {
          case Some(s) =>
            val headerCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))
            cache.fetch(headerCarrier) flatMap { journeyData =>
              block(LocalContext(request, s, journeyData))
            }
          case None =>
            Future.successful(Redirect(routes.TravelDetailsController.newSession()))
        }

      }
    }
  }
}

@Singleton
class DashboardAction @Inject() (publicAction: PublicAction, dashboardEnforcer: DashboardEnforcer) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      dashboardEnforcer {
        block(context)
      }
    }
  }
}