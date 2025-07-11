/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.*
import controllers.ControllerHelpers
import models.PrivateCraftDto.*
import models.*
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import services.*
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TravelDetailsController @Inject() (
  val countriesService: CountriesService,
  val calculatorService: CalculatorService,
  val travelDetailsService: TravelDetailsService,
  val cache: Cache,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService,
  val backLinkModel: BackLinkModel,
  val eu_country_check: views.html.travel_details.eu_country_check,
  val no_need_to_use_service: views.html.travel_details.no_need_to_use_service,
  val goods_brought_into_gb: views.html.travel_details.goods_bought_inside_and_outside_eu,
  val goods_brought_into_ni: views.html.travel_details.goods_bought_outside_eu,
  val goods_bought_inside_eu: views.html.travel_details.goods_bought_inside_eu,
  val confirm_age: views.html.travel_details.confirm_age,
  val private_travel: views.html.travel_details.private_travel,
  val errorTemplate: views.html.errorTemplate,
  val did_you_claim_tax_back: views.html.travel_details.did_you_claim_tax_back,
  val bringing_duty_free_question: views.html.travel_details.bringing_duty_free_question,
  val duty_free_allowance_question_mix: views.html.travel_details.duty_free_allowance_question_mix,
  val duty_free_allowance_question_eu: views.html.travel_details.duty_free_allowance_question_eu,
  val no_need_to_use_service_gbni: views.html.travel_details.no_need_to_use_service_gbni,
  whereGoodsBoughtAction: WhereGoodsBoughtAction,
  didYouClaimTaxBackAction: DidYouClaimTaxBackAction,
  bringingDutyFreeAction: BringingDutyFreeAction,
  goodsBoughtInsideEuAction: GoodsBoughtInsideEuAction,
  goodsBoughtIntoNIAction: GoodsBoughtIntoNIAction,
  goodsBoughtIntoGBAction: GoodsBoughtIntoGBAction,
  noNeedToUseServiceAction: NoNeedToUseServiceAction,
  declareDutyFreeAnyAction: DeclareDutyFreeAnyAction,
  declareDutyFreeEuAction: DeclareDutyFreeEuAction,
  declareDutyFreeMixAction: DeclareDutyFreeMixAction,
  privateCraftAction: PrivateCraftAction,
  is17OrOverAction: Is17OrOverAction,
  noNeedToUseServiceGbniAction: NoNeedToUseServiceGbniAction,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  private val logger = Logger(this.getClass)

  val newSession: Action[AnyContent] = Action.async { implicit request =>
    Future.successful {
      if (appConfig.isAmendmentsEnabled) {
        Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage)
          .addingToSession(SessionKeys.sessionId -> UUID.randomUUID.toString)
      } else {
        Redirect(routes.TravelDetailsController.whereGoodsBought)
          .addingToSession(SessionKeys.sessionId -> UUID.randomUUID.toString)
      }
    }
  }

  val whereGoodsBought: Action[AnyContent] = whereGoodsBoughtAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                _,
                Some(countryCheck),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(eu_country_check(EuCountryCheckDto.form.fill(EuCountryCheckDto(countryCheck)), backLinkModel.backLink))
        case _ =>
          Ok(eu_country_check(EuCountryCheckDto.form, backLinkModel.backLink))
      }
    }
  }

  val whereGoodsBoughtPost: Action[AnyContent] = whereGoodsBoughtAction { implicit context =>
    EuCountryCheckDto.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(eu_country_check(formWithErrors, backLinkModel.backLink))),
        euCountryCheckDto =>
          travelDetailsService.storeEuCountryCheck(context.journeyData)(euCountryCheckDto.euCountryCheck).flatMap { _ =>
            cache.fetch map { _ => Redirect(routes.ArrivingNIController.loadArrivingNIPage) }
          }
      )
  }

  val didYouClaimTaxBack: Action[AnyContent] = didYouClaimTaxBackAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                Some(claimedVatRes),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(
            did_you_claim_tax_back(ClaimedVatResDto.form.fill(ClaimedVatResDto(claimedVatRes)), backLinkModel.backLink)
          )
        case _ =>
          Ok(did_you_claim_tax_back(ClaimedVatResDto.form, backLinkModel.backLink))
      }
    }
  }

  val didYouClaimTaxBackPost: Action[AnyContent] = didYouClaimTaxBackAction { implicit context =>
    ClaimedVatResDto.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(did_you_claim_tax_back(formWithErrors, backLinkModel.backLink))),
        didYouClaimTaxBackDto =>
          travelDetailsService.storeVatResCheck(context.journeyData)(didYouClaimTaxBackDto.claimedVatRes) map { _ =>
            if (didYouClaimTaxBackDto.claimedVatRes) {
              Redirect(routes.TravelDetailsController.privateTravel)
            } else {
              Redirect(routes.TravelDetailsController.dutyFree)
            }
          }
      )
  }

  val dutyFree: Action[AnyContent] = bringingDutyFreeAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                Some(isBringingDutyFree),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(
            bringing_duty_free_question(
              BringingDutyFreeDto.form.fill(BringingDutyFreeDto(isBringingDutyFree)),
              backLinkModel.backLink
            )
          )
        case _ =>
          Ok(bringing_duty_free_question(BringingDutyFreeDto.form, backLinkModel.backLink))
      }
    }
  }

  val dutyFreePost: Action[AnyContent] = bringingDutyFreeAction { implicit context =>
    BringingDutyFreeDto.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(bringing_duty_free_question(formWithErrors, backLinkModel.backLink))),
        isBringingDutyFreeDto =>
          travelDetailsService
            .storeBringingDutyFree(context.journeyData)(
              isBringingDutyFreeDto.isBringingDutyFree
            )
            .flatMap { _ =>
              if (!isBringingDutyFreeDto.isBringingDutyFree) {
                cache.fetch map {
                  case Some(jd) if jd.euCountryCheck.contains("euOnly") =>
                    Redirect(routes.TravelDetailsController.goodsBoughtInsideEu)
                  case Some(jd) if jd.euCountryCheck.contains("both")   =>
                    Redirect(routes.TravelDetailsController.goodsBoughtIntoGB)
                  case _                                                =>
                    Redirect(routes.TravelDetailsController.privateTravel)
                }
              } else {
                cache.fetch map {
                  case Some(jd) if jd.euCountryCheck.contains("euOnly") =>
                    Redirect(routes.TravelDetailsController.bringingDutyFreeQuestionEu)
                  case Some(jd) if jd.euCountryCheck.contains("both")   =>
                    Redirect(routes.TravelDetailsController.bringingDutyFreeQuestionMix)
                  case Some(_)                                          =>
                    throw new RuntimeException(
                      "[TravelDetailsController][dutyFreePost] Unable to determine the next step based on the euCountryCheck value"
                    )
                  case _                                                =>
                    throw new RuntimeException(
                      "[TravelDetailsController][dutyFreePost] No user answers found in the cache to determine the next step"
                    )
                }
              }
            }
      )
  }

  val goodsBoughtInsideEu: Action[AnyContent] = goodsBoughtInsideEuAction { implicit context =>
    Future.successful(Ok(goods_bought_inside_eu(backLinkModel.backLink)))
  }

  val goodsBoughtIntoNI: Action[AnyContent] = goodsBoughtIntoNIAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                Some(bringingOverAllowance),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(
            goods_brought_into_ni(
              BringingOverAllowanceDto.form.bind(Map("bringingOverAllowance" -> bringingOverAllowance.toString)),
              backLinkModel.backLink
            )
          )
        case _ =>
          Ok(goods_brought_into_ni(BringingOverAllowanceDto.form, backLinkModel.backLink))
      }
    }
  }

  val goodsBoughtIntoNIPost: Action[AnyContent] = goodsBoughtIntoNIAction { implicit context =>
    BringingOverAllowanceDto.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(goods_brought_into_ni(formWithErrors, backLinkModel.backLink))),
        overAllowanceDto =>
          travelDetailsService.storeBringingOverAllowance(context.journeyData)(
            overAllowanceDto.bringingOverAllowance
          ) map { _ =>
            if (overAllowanceDto.bringingOverAllowance) {
              Redirect(routes.TravelDetailsController.privateTravel)
            } else {
              Redirect(routes.TravelDetailsController.noNeedToUseService)
            }
          }
      )
  }

  val goodsBoughtIntoGB: Action[AnyContent] = goodsBoughtIntoGBAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                Some(bringingOverAllowance),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(
            goods_brought_into_gb(
              BringingOverAllowanceDto.form.bind(Map("bringingOverAllowance" -> bringingOverAllowance.toString)),
              backLinkModel.backLink
            )
          )
        case _ =>
          Ok(goods_brought_into_gb(BringingOverAllowanceDto.form, backLinkModel.backLink))
      }
    }
  }

  val goodsBoughtIntoGBPost: Action[AnyContent] = goodsBoughtIntoGBAction { implicit context =>
    BringingOverAllowanceDto.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(goods_brought_into_gb(formWithErrors, backLinkModel.backLink))),
        overAllowanceDto =>
          travelDetailsService.storeBringingOverAllowance(context.journeyData)(
            overAllowanceDto.bringingOverAllowance
          ) map { _ =>
            if (overAllowanceDto.bringingOverAllowance) {
              Redirect(routes.TravelDetailsController.privateTravel)
            } else {
              Redirect(routes.TravelDetailsController.noNeedToUseService)
            }
          }
      )
  }

  val noNeedToUseService: Action[AnyContent] = noNeedToUseServiceAction { implicit context =>
    Future.successful(Ok(no_need_to_use_service(backLinkModel.backLink)))
  }

  val bringingDutyFreeQuestionEu: Action[AnyContent] = declareDutyFreeEuAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                Some(bringingOverAllowance),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(
            duty_free_allowance_question_eu(
              BringingOverAllowanceDto.form.bind(Map("bringingOverAllowance" -> bringingOverAllowance.toString)),
              mixEuRow = false,
              backLinkModel.backLink
            )
          )
        case _ =>
          Ok(duty_free_allowance_question_eu(BringingOverAllowanceDto.form, mixEuRow = false, backLinkModel.backLink))
      }
    }
  }

  val bringingDutyFreeQuestionMix: Action[AnyContent] = declareDutyFreeMixAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                Some(bringingOverAllowance),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(
            duty_free_allowance_question_mix(
              BringingOverAllowanceDto.form.bind(Map("bringingOverAllowance" -> bringingOverAllowance.toString)),
              mixEuRow = true,
              backLinkModel.backLink
            )
          )
        case _ =>
          Ok(duty_free_allowance_question_mix(BringingOverAllowanceDto.form, mixEuRow = true, backLinkModel.backLink))
      }
    }
  }

  val dutyFreeAllowanceQuestionMixPost: Action[AnyContent] = declareDutyFreeAnyAction { implicit context =>
    BringingOverAllowanceDto.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(duty_free_allowance_question_mix(formWithErrors, mixEuRow = true, backLinkModel.backLink))
          ),
        overAllowanceDto =>
          travelDetailsService.storeBringingOverAllowance(context.journeyData)(
            overAllowanceDto.bringingOverAllowance
          ) map { _ =>
            if (overAllowanceDto.bringingOverAllowance) {
              Redirect(routes.TravelDetailsController.privateTravel)
            } else {
              Redirect(routes.TravelDetailsController.noNeedToUseService)
            }
          }
      )
  }

  val dutyFreeAllowanceQuestionEuPost: Action[AnyContent] = declareDutyFreeAnyAction { implicit context =>
    BringingOverAllowanceDto.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(duty_free_allowance_question_eu(formWithErrors, mixEuRow = false, backLinkModel.backLink))
          ),
        overAllowanceDto =>
          travelDetailsService.storeBringingOverAllowance(context.journeyData)(
            overAllowanceDto.bringingOverAllowance
          ) map { _ =>
            if (overAllowanceDto.bringingOverAllowance) {
              Redirect(routes.TravelDetailsController.privateTravel)
            } else {
              Redirect(routes.TravelDetailsController.noNeedToUseService)
            }
          }
      )
  }

  val privateTravel: Action[AnyContent] = privateCraftAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(_, _, _, _, _, _, _, _, _, _, Some(pc), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)
            ) =>
          Ok(private_travel(form.bind(Map("privateCraft" -> pc.toString)), backLinkModel.backLink))
        case _ =>
          Ok(private_travel(form, backLinkModel.backLink))
      }
    }
  }

  val privateTravelPost: Action[AnyContent] = privateCraftAction { implicit context =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(private_travel(formWithErrors, backLinkModel.backLink))),
        privateCraftDto =>
          travelDetailsService.storePrivateCraft(context.journeyData)(privateCraftDto.privateCraft) map { _ =>
            Redirect(routes.TravelDetailsController.confirmAge)
          }
      )
  }

  def confirmAge: Action[AnyContent] = is17OrOverAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                Some(ageOver17),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(confirm_age(AgeOver17Dto.form.bind(Map("ageOver17" -> ageOver17.toString)), backLinkModel.backLink))
        case _ =>
          Ok(confirm_age(AgeOver17Dto.form, backLinkModel.backLink))
      }
    }
  }

  def confirmAgePost: Action[AnyContent] = is17OrOverAction { implicit context =>
    AgeOver17Dto.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(confirm_age(formWithErrors, backLinkModel.backLink))),
        ageOver17Dto =>
          travelDetailsService.storeAgeOver17(context.journeyData)(ageOver17Dto.ageOver17) map { _ =>
            Redirect(routes.DashboardController.showDashboard)
          }
      )
  }

  val keepAlive: Action[AnyContent] = Action.async { implicit request =>
    cache.updateUpdatedAtTimestamp.map(_ => Ok("Ok")).recover { case e =>
      logger.error(s"[TravelDetailsController][keepAlive] failed to keep session alive because ${e.getMessage}")
      InternalServerError(e.getMessage)
    }
  }

  val noNeedToUseServiceGbni: Action[AnyContent] = noNeedToUseServiceGbniAction { implicit context =>
    Future.successful(Ok(no_need_to_use_service_gbni(backLinkModel.backLink)))
  }

}
