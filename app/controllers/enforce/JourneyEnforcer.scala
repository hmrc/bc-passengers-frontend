package controllers.enforce

import config.AppConfig
import connectors.Cache
import controllers.{LocalContext, routes}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc.{Action, AnyContent, AnyContentAsFormUrlEncoded, Call, DefaultActionBuilder, Request, RequestHeader, Result}
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
class DashboardAction @Inject() (journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step = if(appConfig.isVatResJourneyEnabled) vatres.DashboardStep else nonvatres.DashboardStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class WhereGoodsBoughtAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step = if(appConfig.isVatResJourneyEnabled) vatres.WhereGoodsBoughtStep else nonvatres.WhereGoodsBoughtStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class GoodsBoughtInAndOutEuAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step = if(appConfig.isVatResJourneyEnabled) vatres.GoodsBoughtInAndOutEuStep else nonvatres.GoodsBoughtInAndOutEuStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class GoodsBoughtOutsideEuAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step = if(appConfig.isVatResJourneyEnabled) vatres.GoodsBoughtOutsideEuStep else nonvatres.GoodsBoughtOutsideEuStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class GoodsBoughtInsideEuAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step = if(appConfig.isVatResJourneyEnabled) vatres.GoodsBoughtInsideEuStep else nonvatres.GoodsBoughtInsideEuStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class NoNeedToUseServiceAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step = if(appConfig.isVatResJourneyEnabled) vatres.NoNeedToUseStep else nonvatres.NoNeedToUseStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class PrivateCraftAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step = if(appConfig.isVatResJourneyEnabled) vatres.PrivateCraftStep else nonvatres.PrivateCraftStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class Is17OrOverAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step = if(appConfig.isVatResJourneyEnabled) vatres.Is17OrOverStep else nonvatres.Is17OrOverStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class DidYouClaimTaxBackAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.DidYouClaimTaxBackEuOnlyStep, vatres.DidYouClaimTaxBackBothStep ) {
        block(context)
      }
    }
  }
}

@Singleton
class BringingDutyFreeAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.BringingDutyFreeEuStep, vatres.BringingDutyFreeBothStep ) {
        block(context)
      }
    }
  }
}

@Singleton
class DeclareDutyFreeMixAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.DeclareDutyFreeMixStep ) {
        block(context)
      }
    }
  }
}

@Singleton
class DeclareDutyFreeEuAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.DeclareDutyFreeEuStep ) {
        block(context)
      }
    }
  }
}

@Singleton
class DeclareDutyFreeAnyAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.DeclareDutyFreeEuStep, vatres.DeclareDutyFreeMixStep ) {
        block(context)
      }
    }
  }
}


@Singleton
class DeclareAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {


  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>

      if (context.getJourneyData.calculatorResponse.fold(false)(x => BigDecimal(x.calculation.allTax) >= appConfig.minPaymentAmount && BigDecimal(x.calculation.allTax) <=  appConfig.paymentLimit)){
        block(context)
      } else {
       Future(Redirect(routes.TravelDetailsController.whereGoodsBought()))
     }
    }
  }
}
