package controllers

import java.util.UUID

import config.AppConfig
import connectors.Cache
import javax.inject.{Inject, Singleton}
import models.PrivateCraftDto._
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TravelDetailsController @Inject() (

  val countriesService: CountriesService,
  val calculatorService: CalculatorService,
  val travelDetailsService: TravelDetailsService,
  val cache: Cache,
  val productsService: ProductTreeService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService,
  val backLinkModel: BackLinkModel,
  val check_declare_goods_start_page: views.html.travel_details.check_declare_goods_start_page,
  val eu_country_check: views.html.travel_details.eu_country_check,
  val no_need_to_use_service: views.html.travel_details.no_need_to_use_service,

  val goods_bought_inside_and_outside_eu: views.html.travel_details.goods_bought_inside_and_outside_eu,
  val goods_bought_outside_eu: views.html.travel_details.goods_bought_outside_eu,
  val goods_bought_inside_eu: views.html.travel_details.goods_bought_inside_eu,
  val confirm_age: views.html.travel_details.confirm_age,
  val private_travel: views.html.travel_details.private_travel,
  val error_template: views.html.error_template,
  val did_you_claim_tax_back: views.html.travel_details.did_you_claim_tax_back,
  val duty_free: views.html.travel_details.duty_free,
  val duty_free_interrupt: views.html.travel_details.duty_free_interrupt,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  val newSession: Action[AnyContent] = Action.async { implicit request =>

    Future.successful {
      Redirect(routes.TravelDetailsController.whereGoodsBought()).addingToSession(SessionKeys.sessionId -> UUID.randomUUID.toString)
    }
  }

  val checkDeclareGoodsStartPage: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(check_declare_goods_start_page())
    )
  }

  val whereGoodsBought: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(Some(countryCheck), _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(eu_country_check(EuCountryCheckDto.form.fill(EuCountryCheckDto(countryCheck))))
        case _ =>
          Ok(eu_country_check(EuCountryCheckDto.form))
      }
    }
  }

  def whereGoodsBoughtPost: Action[AnyContent] = PublicAction { implicit context =>

    EuCountryCheckDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(eu_country_check(formWithErrors)))
      },
      euCountryCheckDto => {
        travelDetailsService.storeEuCountryCheck(context.journeyData)(euCountryCheckDto.euCountryCheck) flatMap { _ =>
          cache.fetch map { _ =>
            if (appConfig.isVatResJourneyEnabled) {
              euCountryCheckDto.euCountryCheck match {
                case "euOnly" => Redirect(routes.TravelDetailsController.didYouClaimTaxBack())
                case "nonEuOnly" => Redirect(routes.TravelDetailsController.goodsBoughtOutsideEu())
                case "both" => Redirect(routes.TravelDetailsController.didYouClaimTaxBack())
              }
            } else {
              euCountryCheckDto.euCountryCheck match {
                case "euOnly" => Redirect(routes.TravelDetailsController.goodsBoughtInsideEu())
                case "nonEuOnly" => Redirect(routes.TravelDetailsController.goodsBoughtOutsideEu())
                case "both" => Redirect(routes.TravelDetailsController.goodsBoughtInsideAndOutsideEu())
              }
            }
          }
        }
      }
    )
  }

  def didYouClaimTaxBack: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, Some(claimedVatRes), _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(did_you_claim_tax_back(ClaimedVatResDto.form.fill(ClaimedVatResDto(claimedVatRes)), backLinkModel.backLink))
        case _ =>
          Ok(did_you_claim_tax_back(ClaimedVatResDto.form, backLinkModel.backLink))
      }
    }
  }
  
  def didYouClaimTaxBackPost: Action[AnyContent] = PublicAction { implicit context =>
    ClaimedVatResDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(did_you_claim_tax_back(formWithErrors, backLinkModel.backLink)))
      },
      didYouClaimTaxBackDto => {
        travelDetailsService.storeVatResCheck(context.journeyData)(didYouClaimTaxBackDto.claimedVatRes) map { _ =>
          if (didYouClaimTaxBackDto.claimedVatRes) {
            Redirect(routes.TravelDetailsController.privateTravel())
          } else {
            Redirect(routes.TravelDetailsController.dutyFree())
          }
        }
      }
    )
  }

  def dutyFree: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, Some(isBringingDutyFree), _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(duty_free(BringingDutyFreeDto.form.fill(BringingDutyFreeDto(isBringingDutyFree)), backLinkModel.backLink))
        case _ =>
          Ok(duty_free(BringingDutyFreeDto.form, backLinkModel.backLink))
      }
    }
  }

  def dutyFreeEu: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _, Some(bringingOverAllowance), _, _, _, _, _, _, _, _, _, _)) =>
          Ok(duty_free_interrupt(BringingOverAllowanceDto.form.bind(Map("bringingOverAllowance" -> bringingOverAllowance.toString)), mixEuRow = false, backLinkModel.backLink))
        case _ =>
          Ok(duty_free_interrupt(BringingOverAllowanceDto.form, mixEuRow = false, backLinkModel.backLink))
      }
    }
  }

  def dutyFreeMix: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _, Some(bringingOverAllowance), _, _, _, _, _, _, _, _, _, _)) =>
          Ok(duty_free_interrupt(BringingOverAllowanceDto.form.bind(Map("bringingOverAllowance" -> bringingOverAllowance.toString)), mixEuRow = true, backLinkModel.backLink))
        case _ =>
          Ok(duty_free_interrupt(BringingOverAllowanceDto.form, mixEuRow = true, backLinkModel.backLink))
      }
    }
  }

  def dutyFreeInterruptPost: Action[AnyContent] = PublicAction { implicit context =>
    BringingOverAllowanceDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(goods_bought_outside_eu(formWithErrors, backLinkModel.backLink)))
      },
      overAllowanceDto => {
        travelDetailsService.storeBringingOverAllowance(context.journeyData)( overAllowanceDto.bringingOverAllowance ) map { _ =>
          if (overAllowanceDto.bringingOverAllowance) {
            Redirect(routes.TravelDetailsController.privateTravel())
          } else {
            Redirect(routes.TravelDetailsController.noNeedToUseService())
          }
        }
      }
    )
  }

  def dutyFreePost: Action[AnyContent] = PublicAction { implicit context =>
    BringingDutyFreeDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(duty_free(formWithErrors, backLinkModel.backLink)))
      },
      isBringingDutyFreeDto => {
        travelDetailsService.storeBringingDutyFree(context.journeyData)(isBringingDutyFreeDto.isBringingDutyFree) flatMap { _ =>
          if (!isBringingDutyFreeDto.isBringingDutyFree) {
            cache.fetch map {
              case Some(jd) if jd.euCountryCheck.contains("euOnly") =>
                Redirect(routes.TravelDetailsController.goodsBoughtInsideEu())
              case Some(jd) if jd.euCountryCheck.contains("both") =>
                Redirect(routes.TravelDetailsController.goodsBoughtInsideAndOutsideEu())
              case _ =>
                Redirect(routes.TravelDetailsController.privateTravel())
            }
          } else {
            cache.fetch map {
              case Some(jd) if jd.euCountryCheck.contains("euOnly") =>
                Redirect(routes.TravelDetailsController.dutyFreeEu())
              case Some(jd) if jd.euCountryCheck.contains("both") =>
                Redirect(routes.TravelDetailsController.dutyFreeMix())
            }
          }
        }
      }
    )
  }

  val goodsBoughtInsideEu: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful(Ok(goods_bought_inside_eu(backLinkModel.backLink)))
  }

  val goodsBoughtOutsideEu: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _, Some(bringingOverAllowance), _, _, _, _, _, _, _, _, _, _)) =>
          Ok(goods_bought_outside_eu(BringingOverAllowanceDto.form.bind(Map("bringingOverAllowance" -> bringingOverAllowance.toString)), backLinkModel.backLink))
        case _ =>
          Ok(goods_bought_outside_eu(BringingOverAllowanceDto.form, backLinkModel.backLink))
      }
    }
  }

  def goodsBoughtOutsideEuPost: Action[AnyContent] = PublicAction { implicit context =>
    BringingOverAllowanceDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(goods_bought_outside_eu(formWithErrors, backLinkModel.backLink)))
      },
      overAllowanceDto => {
        travelDetailsService.storeBringingOverAllowance(context.journeyData)( overAllowanceDto.bringingOverAllowance ) map { _ =>
          if (overAllowanceDto.bringingOverAllowance) {
            Redirect(routes.TravelDetailsController.privateTravel())
          } else {
            Redirect(routes.TravelDetailsController.noNeedToUseService())
          }
        }
      }
    )
  }

  val goodsBoughtInsideAndOutsideEu: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _, Some(bringingOverAllowance), _, _, _, _, _, _, _, _, _, _)) =>
          Ok(goods_bought_inside_and_outside_eu(BringingOverAllowanceDto.form.bind(Map("bringingOverAllowance" -> bringingOverAllowance.toString)), backLinkModel.backLink))
        case _ =>
          Ok(goods_bought_inside_and_outside_eu(BringingOverAllowanceDto.form, backLinkModel.backLink))
      }
    }
  }

  def goodsBoughtInsideAndOutsideEuPost: Action[AnyContent] = PublicAction { implicit context =>
    BringingOverAllowanceDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(goods_bought_inside_and_outside_eu(formWithErrors, backLinkModel.backLink)))
      },
      overAllowanceDto => {
        travelDetailsService.storeBringingOverAllowance(context.journeyData)( overAllowanceDto.bringingOverAllowance ) map { _ =>
          if (overAllowanceDto.bringingOverAllowance) {
            Redirect(routes.TravelDetailsController.privateTravel())
          } else {
            Redirect(routes.TravelDetailsController.noNeedToUseService())
          }
        }
      }
    )
  }

  val noNeedToUseService: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful(Ok(no_need_to_use_service(backLinkModel.backLink)))
  }


  def confirmAge: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _, _, _, Some(ageOver17), _, _, _, _, _, _, _, _)) =>
          Ok(confirm_age(AgeOver17Dto.form.bind(Map("ageOver17" -> ageOver17.toString)), backLinkModel.backLink))
        case _ =>
          Ok(confirm_age(AgeOver17Dto.form, backLinkModel.backLink))
      }
    }
  }

  def confirmAgePost: Action[AnyContent] = PublicAction { implicit context =>

    AgeOver17Dto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(confirm_age(formWithErrors, backLinkModel.backLink)))
      },
      ageOver17Dto => {
        travelDetailsService.storeAgeOver17(context.journeyData)(ageOver17Dto.ageOver17) map { _ =>
          Redirect(routes.DashboardController.showDashboard())
        }
      }
    )
  }

  val privateTravel: Action[AnyContent] = PublicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _, _, Some(pc), _, _, _, _, _, _, _, _, _)) =>
          Ok(private_travel(form.bind(Map("privateCraft" -> pc.toString)), backLinkModel.backLink))
        case _ =>
          Ok(private_travel(form, backLinkModel.backLink))
      }
    }
  }

  val privateTravelPost: Action[AnyContent] = PublicAction { implicit context =>
    form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(private_travel(formWithErrors, backLinkModel.backLink)))
      },
      privateCraftDto => {
        travelDetailsService.storePrivateCraft(context.journeyData)( privateCraftDto.privateCraft ) map { _ =>
          Redirect(routes.TravelDetailsController.confirmAge())
        }
      }
    )
  }

  val keepAlive: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok("Ok")
    )
  }

}
