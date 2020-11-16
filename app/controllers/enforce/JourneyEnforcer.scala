/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.enforce

import config.AppConfig
import connectors.Cache
import controllers.actions.IdentifierAction
import controllers.{LocalContext, routes}
import javax.inject.{Inject, Singleton}
import models.IdentifierRequest
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class JourneyEnforcer {
  def logAndRedirect(logMessage: String, redirectLocation: Call)(implicit context: LocalContext): Future[Result] = {
    Logger.debug(logMessage)
    Future.successful(Redirect(redirectLocation))
  }

  def apply(q: JourneyStep*)(block: => Future[Result])(implicit context: LocalContext): Future[Result] = if(q.exists(_.meetsAllPrerequisites)) block else redirect

  def redirect(implicit context: LocalContext): Future[Result] =
    logAndRedirect(s"Enforcer prerequisites not met for ${context.request.path}, redirecting", routes.TravelDetailsController.newSession())
}

@Singleton
class PublicAction @Inject() (cache: Cache, actionBuilder: DefaultActionBuilder, identify: IdentifierAction) {

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

    identify.async { implicit request =>
      val provideId = request.credId

      trimmingFormUrlEncodedData { implicit request =>

        request.session.get(SessionKeys.sessionId) match {
          case Some(s) =>
            val headerCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))
            cache.fetch(headerCarrier) flatMap { journeyData =>
              block(LocalContext(IdentifierRequest(request, provideId), s, journeyData))
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

  val step: JourneyStep = if(appConfig.isVatResJourneyEnabled) vatres.DashboardStep else nonvatres.DashboardStep

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

  val step: JourneyStep = if(appConfig.isVatResJourneyEnabled) {
    if(appConfig.isAmendmentsEnabled) vatres.WhereGoodsBoughtAmendmentStep else vatres.WhereGoodsBoughtStep
  } else {
    nonvatres.WhereGoodsBoughtStep
  }

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class GoodsBoughtIntoGBAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(vatres.GoodsBoughtIntoGBStep) {
        block(context)
      }
    }
  }
}

@Singleton
class GoodsBoughtIntoNIAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(vatres.GoodsBoughtIntoNIStep) {
        block(context)
      }
    }
  }
}

@Singleton
class GoodsBoughtInsideEuAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step: JourneyStep = if(appConfig.isVatResJourneyEnabled) vatres.GoodsBoughtInsideEuStep else nonvatres.GoodsBoughtInsideEuStep

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

  val step: JourneyStep = if(appConfig.isVatResJourneyEnabled) vatres.NoNeedToUseStep else nonvatres.NoNeedToUseStep

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

  val step: JourneyStep = if(appConfig.isVatResJourneyEnabled) vatres.PrivateCraftStep else nonvatres.PrivateCraftStep

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

  val step: JourneyStep = if(appConfig.isVatResJourneyEnabled) vatres.Is17OrOverStep else nonvatres.Is17OrOverStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class DidYouClaimTaxBackAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.DidYouClaimTaxBackEuOnlyStep, vatres.DidYouClaimTaxBackBothStep ) {
        block(context)
      }
    }
  }
}

@Singleton
class BringingDutyFreeAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.BringingDutyFreeEuStep, vatres.BringingDutyFreeBothStep ) {
        block(context)
      }
    }
  }
}

@Singleton
class DeclareDutyFreeMixAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.DeclareDutyFreeMixStep ) {
        block(context)
      }
    }
  }
}

@Singleton
class DeclareDutyFreeEuAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.DeclareDutyFreeEuStep ) {
        block(context)
      }
    }
  }
}

@Singleton
class DeclareDutyFreeAnyAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer( vatres.DeclareDutyFreeEuStep, vatres.DeclareDutyFreeMixStep ) {
        block(context)
      }
    }
  }
}


@Singleton
class DeclareAction @Inject()(appConfig: AppConfig, publicAction: PublicAction) {


  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>

      if (context.journeyData.isDefined &&
        (context.getJourneyData.calculatorResponse.fold(false)(x => BigDecimal(x.calculation.allTax) > 0 && BigDecimal(x.calculation.allTax) <=  appConfig.paymentLimit) ||
        (context.getJourneyData.euCountryCheck.getOrElse("") == "greatBritain" && context.getJourneyData.calculatorResponse.fold(false)(x => BigDecimal(x.calculation.allTax) == 0 && x.isAnyItemOverAllowance)))
      ){
        block(context)
      }

      else {
       Future(Redirect(routes.TravelDetailsController.whereGoodsBought()))
     }
    }
  }

}

@Singleton
class ArrivingNIAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {
  val step: JourneyStep = if(appConfig.isVatResJourneyEnabled) vatres.ArrivingNIStep else nonvatres.ArrivingNIStep
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
  }
}

@Singleton
class UKVatPaidAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(vatres.UKVatPaidStep) {
        block(context)
      }
    }
  }
}

@Singleton
class UKExcisePaidAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(vatres.UKExcisePaidStep) {
        block(context)
      }
    }
  }
}

@Singleton
class UKResidentAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(vatres.UKResidentStep) {
        block(context)
      }
    }
  }
}

@Singleton
class UccReliefAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(vatres.UccReliefStep) {
        block(context)
      }
    }
  }
}

@Singleton
class NoNeedToUseServiceGbniAction @Inject()(journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(vatres.noNeedToUseServiceGbniStep) {
        block(context)
      }
    }
  }
}

@Singleton
class ZeroDeclarationAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      journeyEnforcer(vatres.ZeroDeclarationStep) {
        block(context)
      }
    }
  }
}

@Singleton
class PreviousDeclarationAction @Inject()(journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] = {
    publicAction { implicit context =>
      if (appConfig.isAmendmentsEnabled) {
        journeyEnforcer(vatres.PreviousDeclarationStep) {
          block(context)
        }
      }
      else {
        Future(Redirect(routes.TravelDetailsController.whereGoodsBought()))
      }
    }
  }
}
