/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.enforce

import config.AppConfig
import connectors.Cache
import controllers.{LocalContext, routes}
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyEnforcer {
  private val logger = Logger(this.getClass)

  def logAndRedirect(logMessage: String, redirectLocation: Call): Future[Result] = {
    logger.debug(logMessage)
    Future.successful(Redirect(redirectLocation))
  }

  def apply(q: JourneyStep*)(block: => Future[Result])(implicit context: LocalContext): Future[Result] =
    if (q.exists(_.meetsAllPrerequisites)) block else redirect

  private def redirect(implicit context: LocalContext): Future[Result] =
    logAndRedirect(
      s"Enforcer prerequisites not met for ${context.request.path}, redirecting",
      routes.TravelDetailsController.newSession
    )
}

@Singleton
class PublicAction @Inject() (cache: Cache, actionBuilder: DefaultActionBuilder, implicit val ec: ExecutionContext) {

  private def trimmingFormUrlEncodedData(
    block: Request[AnyContent] => Future[Result]
  )(implicit request: Request[AnyContent]): Future[Result] =
    block {
      request.map {
        case AnyContentAsFormUrlEncoded(data) =>
          AnyContentAsFormUrlEncoded(data.map { case (key, vals) =>
            (key, vals.map(_.trim))
          })
        case b                                => b
      }
    }

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    actionBuilder.async { implicit request =>
      trimmingFormUrlEncodedData { implicit request =>
        request.session.get(SessionKeys.sessionId) match {
          case Some(s) =>
            val headerCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
            cache.fetch(headerCarrier) flatMap { journeyData =>
              block(LocalContext(request, s, journeyData))
            }
          case None    =>
            Future.successful(Redirect(routes.TravelDetailsController.newSession))
        }

      }
    }
}

@Singleton
class DashboardAction @Inject() (journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step: JourneyStep = if (appConfig.isVatResJourneyEnabled) vatres.DashboardStep else nonvatres.DashboardStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
}

@Singleton
class WhereGoodsBoughtAction @Inject() (
  journeyEnforcer: JourneyEnforcer,
  appConfig: AppConfig,
  publicAction: PublicAction
) {

  val step: JourneyStep = if (appConfig.isVatResJourneyEnabled) {
    if (appConfig.isAmendmentsEnabled) vatres.WhereGoodsBoughtAmendmentStep else vatres.WhereGoodsBoughtStep
  } else {
    nonvatres.WhereGoodsBoughtStep
  }

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
}

@Singleton
class GoodsBoughtIntoGBAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.GoodsBoughtIntoGBStep) {
        block(context)
      }
    }
}

@Singleton
class GoodsBoughtIntoNIAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.GoodsBoughtIntoNIStep) {
        block(context)
      }
    }
}

@Singleton
class GoodsBoughtInsideEuAction @Inject() (
  journeyEnforcer: JourneyEnforcer,
  appConfig: AppConfig,
  publicAction: PublicAction
) {

  val step: JourneyStep =
    if (appConfig.isVatResJourneyEnabled) vatres.GoodsBoughtInsideEuStep else nonvatres.GoodsBoughtInsideEuStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
}

@Singleton
class NoNeedToUseServiceAction @Inject() (
  journeyEnforcer: JourneyEnforcer,
  appConfig: AppConfig,
  publicAction: PublicAction
) {

  val step: JourneyStep = if (appConfig.isVatResJourneyEnabled) vatres.NoNeedToUseStep else nonvatres.NoNeedToUseStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
}

@Singleton
class PrivateCraftAction @Inject() (
  journeyEnforcer: JourneyEnforcer,
  appConfig: AppConfig,
  publicAction: PublicAction
) {

  val step: JourneyStep = if (appConfig.isVatResJourneyEnabled) vatres.PrivateCraftStep else nonvatres.PrivateCraftStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
}

@Singleton
class Is17OrOverAction @Inject() (journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {

  val step: JourneyStep = if (appConfig.isVatResJourneyEnabled) vatres.Is17OrOverStep else nonvatres.Is17OrOverStep

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
}

@Singleton
class DidYouClaimTaxBackAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.DidYouClaimTaxBackEuOnlyStep, vatres.DidYouClaimTaxBackBothStep) {
        block(context)
      }
    }
}

@Singleton
class BringingDutyFreeAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.BringingDutyFreeEuStep, vatres.BringingDutyFreeBothStep) {
        block(context)
      }
    }
}

@Singleton
class DeclareDutyFreeMixAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.DeclareDutyFreeMixStep) {
        block(context)
      }
    }
}

@Singleton
class DeclareDutyFreeEuAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.DeclareDutyFreeEuStep) {
        block(context)
      }
    }
}

@Singleton
class DeclareDutyFreeAnyAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.DeclareDutyFreeEuStep, vatres.DeclareDutyFreeMixStep) {
        block(context)
      }
    }
}

@Singleton
class DeclareAction @Inject() (appConfig: AppConfig, publicAction: PublicAction, implicit val ec: ExecutionContext) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      if (declarationJourney(context)) {
        block(context)
      } else if (amendmentJourney(context)) {
        block(context)
      } else if (appConfig.isAmendmentsEnabled) {
        Future(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
      } else { Future(Redirect(routes.TravelDetailsController.whereGoodsBought)) }
    }

  private def declarationJourney(context: LocalContext): Boolean = context.journeyData.isDefined &&
    (context.getJourneyData.calculatorResponse.exists(x =>
      BigDecimal(x.calculation.allTax) > 0 && BigDecimal(x.calculation.allTax) <= appConfig.paymentLimit
    ) ||
      (context.getJourneyData.euCountryCheck.contains("greatBritain") && context.getJourneyData.calculatorResponse
        .exists(x => BigDecimal(x.calculation.allTax) == 0 && x.isAnyItemOverAllowance)))

  private def amendmentJourney(context: LocalContext): Boolean =
    context.journeyData.isDefined && context.getJourneyData.calculatorResponse.isDefined &&
      (context.getJourneyData.deltaCalculation.fold(false)(x =>
        BigDecimal(x.allTax) > 0 && BigDecimal(x.allTax) <= appConfig.paymentLimit
      ) ||
        (context.getJourneyData.euCountryCheck.contains("greatBritain") && context.getJourneyData.deltaCalculation
          .exists(x =>
            BigDecimal(x.allTax) == 0 && context.getJourneyData.calculatorResponse.get.isAnyItemOverAllowance
          )))

}

@Singleton
class UserInfoAction @Inject() (appConfig: AppConfig, publicAction: PublicAction, implicit val ec: ExecutionContext) {

  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      if (
        context.journeyData.isDefined && !context.getJourneyData.prevDeclaration.getOrElse(false) &&
        (context.getJourneyData.calculatorResponse.fold(false)(x =>
          BigDecimal(x.calculation.allTax) > 0 && BigDecimal(x.calculation.allTax) <= appConfig.paymentLimit
        ) ||
          (context.getJourneyData.euCountryCheck.getOrElse(
            ""
          ) == "greatBritain" && context.getJourneyData.calculatorResponse
            .fold(false)(x => BigDecimal(x.calculation.allTax) == 0 && x.isAnyItemOverAllowance)))
      ) {
        block(context)
      } else {
        if (appConfig.isAmendmentsEnabled) {
          Future(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
        } else {
          Future(Redirect(routes.TravelDetailsController.whereGoodsBought))
        }
      }
    }

}

@Singleton
class ArrivingNIAction @Inject() (journeyEnforcer: JourneyEnforcer, appConfig: AppConfig, publicAction: PublicAction) {
  val step: JourneyStep                                                =
    if (appConfig.isVatResJourneyEnabled) vatres.ArrivingNIStep else nonvatres.ArrivingNIStep
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(step) {
        block(context)
      }
    }
}

@Singleton
class LimitExceedAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.DashboardStep) {
        block(context)
      }
    }
}

@Singleton
class UKVatPaidAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.UKVatPaidStep) {
        block(context)
      }
    }
}

@Singleton
class UKExcisePaidAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.UKExcisePaidStep) {
        block(context)
      }
    }
}

@Singleton
class UKExcisePaidItemAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.UKExcisePaidItemStep) {
        block(context)
      }
    }
}

@Singleton
class UKResidentAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.UKResidentStep) {
        block(context)
      }
    }
}

@Singleton
class UccReliefAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.UccReliefStep) {
        block(context)
      }
    }
}

@Singleton
class DeclarationNotFoundAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.declarationNotFoundStep) {
        block(context)
      }
    }
}

@Singleton
class PendingPaymentAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.pendingPaymentStep) {
        block(context)
      }
    }
}

@Singleton
class NoFurtherAmendmentAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.noFurtherAmendmentStep) {
        block(context)
      }
    }
}

@Singleton
class NoNeedToUseServiceGbniAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.noNeedToUseServiceGbniStep) {
        block(context)
      }
    }
}

@Singleton
class ZeroDeclarationAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.ZeroDeclarationStep) {
        block(context)
      }
    }
}

@Singleton
class PreviousDeclarationAction @Inject() (
  journeyEnforcer: JourneyEnforcer,
  appConfig: AppConfig,
  publicAction: PublicAction,
  implicit val ec: ExecutionContext
) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      if (appConfig.isAmendmentsEnabled) {
        journeyEnforcer(vatres.PreviousDeclarationStep) {
          block(context)
        }
      } else {
        Future(Redirect(routes.TravelDetailsController.whereGoodsBought))
      }
    }
}

@Singleton
class DeclarationRetrievalAction @Inject() (
  journeyEnforcer: JourneyEnforcer,
  appConfig: AppConfig,
  publicAction: PublicAction,
  implicit val ec: ExecutionContext
) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      if (appConfig.isAmendmentsEnabled) {
        journeyEnforcer(vatres.DeclarationRetrievalStep) {
          block(context)
        }
      } else {
        Future(Redirect(routes.TravelDetailsController.whereGoodsBought))
      }
    }
}

@Singleton
class EUEvidenceItemAction @Inject() (journeyEnforcer: JourneyEnforcer, publicAction: PublicAction) {
  def apply(block: LocalContext => Future[Result]): Action[AnyContent] =
    publicAction { implicit context =>
      journeyEnforcer(vatres.EUEvidenceItemStep) {
        block(context)
      }
    }
}
