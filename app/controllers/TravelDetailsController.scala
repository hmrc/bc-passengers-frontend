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
  val check_declare_goods_start_page: views.html.travel_details.check_declare_goods_start_page,
  val eu_country_check: views.html.travel_details.eu_country_check,
  val no_need_to_use_service: views.html.travel_details.no_need_to_use_service,
  val goods_bought_inside_and_outside_eu: views.html.travel_details.goods_bought_inside_and_outside_eu,
  val goods_bought_outside_eu: views.html.travel_details.goods_bought_outside_eu,
  val goods_bought_inside_eu: views.html.travel_details.goods_bought_inside_eu,
  val confirm_age: views.html.travel_details.confirm_age,
  val confirm_private_craft: views.html.travel_details.confirm_private_craft,
  val error_template: views.html.error_template,
  val vat_res: views.html.travel_details.vat_res,
  val duty_free: views.html.travel_details.duty_free,
  val duty_free_interrupt: views.html.travel_details.duty_free_interrupt,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  val newSession: Action[AnyContent] = Action.async { implicit request =>

    Future.successful {
      Redirect(routes.TravelDetailsController.euCountryCheck()).addingToSession(SessionKeys.sessionId -> UUID.randomUUID.toString)
    }
  }

  val checkDeclareGoodsStartPage: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(check_declare_goods_start_page())
    )
  }

  val euCountryCheck: Action[AnyContent] = PublicAction { implicit request => {
    cache.fetch map {
      case Some(JourneyData(Some(countryCheck), _, _, _, _, _, _, _, _, _)) =>
        Ok(eu_country_check(EuCountryCheckDto.form.fill(EuCountryCheckDto(countryCheck))))
      case _ =>
        Ok(eu_country_check(EuCountryCheckDto.form))
      }
    }
  }

  def euCountryCheckPost: Action[AnyContent] = PublicAction { implicit request =>

    EuCountryCheckDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(eu_country_check(formWithErrors)))
      },
      euCountryCheckDto => {
        travelDetailsService.storeEuCountryCheck(euCountryCheckDto.euCountryCheck) flatMap { _ =>
          cache.fetch map {
            case Some(JourneyData(Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _)) =>
              Redirect(routes.DashboardController.showDashboard())
            case _ =>
              if (appConfig.usingVatResJourney) {
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

  def didYouClaimTaxBack: Action[AnyContent] = PublicAction { implicit request =>
    cache.fetch map {
      case Some(JourneyData(_, Some(claimedVatRes), _, _, _, _, _, _, _, _)) =>
        Ok(vat_res(ClaimedVatResDto.form.fill(ClaimedVatResDto(claimedVatRes))))
      case _ =>
        Ok(vat_res(ClaimedVatResDto.form))
    }
  }

  def didYouClaimTaxBackPost: Action[AnyContent] = PublicAction { implicit request =>
    ClaimedVatResDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(vat_res(formWithErrors)))
      },
      didYouClaimTaxBackDto => {
        travelDetailsService.storeVatResCheck(didYouClaimTaxBackDto.claimedVatRes) map { _ =>
          if (didYouClaimTaxBackDto.claimedVatRes) {
            Redirect(routes.TravelDetailsController.privateCraft())
          } else {
            Redirect(routes.TravelDetailsController.dutyFree())
          }
        }
      }
    )
  }

  def dutyFree: Action[AnyContent] = PublicAction { implicit request =>
    cache.fetch map {
      case Some(JourneyData(_, _, Some(bringingDutyFree), _, _, _, _, _, _, _)) =>
        Ok(duty_free(BringingDutyFreeDto.form.fill(BringingDutyFreeDto(bringingDutyFree))))
      case _ =>
        Ok(duty_free(BringingDutyFreeDto.form))
    }
  }

  def dutyFreeEu: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(duty_free_interrupt(BringingOverAllowanceDto.form, mixEuRow = false)))
  }

  def dutyFreeMix: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(duty_free_interrupt(BringingOverAllowanceDto.form, mixEuRow = true)))
  }

  def dutyFreeInterruptPost: Action[AnyContent] = PublicAction { implicit request =>
    BringingOverAllowanceDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(goods_bought_outside_eu(formWithErrors)))
      },
      overAllowanceDto => {
        if (overAllowanceDto.bringingOverAllowance) {
          Future.successful(Redirect(routes.TravelDetailsController.privateCraft()))
        } else {
          Future.successful(Redirect(routes.TravelDetailsController.noNeedToUseService()))
        }
      }
    )
  }

  def dutyFreePost: Action[AnyContent] = PublicAction { implicit request =>
    BringingDutyFreeDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(duty_free(formWithErrors)))
      },
      bringingDutyFreeDto => {
        travelDetailsService.storeDutyFreeCheck(bringingDutyFreeDto.bringingDutyFree) flatMap { _ =>
          if (!bringingDutyFreeDto.bringingDutyFree) {
            cache.fetch map {
              case Some(jd) if jd.euCountryCheck.contains("euOnly") =>
                Redirect(routes.TravelDetailsController.goodsBoughtInsideEu())
              case Some(jd) if jd.euCountryCheck.contains("both") =>
                Redirect(routes.TravelDetailsController.goodsBoughtInsideAndOutsideEu())
              case _ =>
                Redirect(routes.TravelDetailsController.privateCraft())
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

  val goodsBoughtInsideEu: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(goods_bought_inside_eu()))
  }

  val goodsBoughtOutsideEu: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(goods_bought_outside_eu(BringingOverAllowanceDto.form)))
  }

  def goodsBoughtOutsideEuPost: Action[AnyContent] = PublicAction { implicit request =>
    BringingOverAllowanceDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(goods_bought_outside_eu(formWithErrors)))
      },
      overAllowanceDto => {
        if (overAllowanceDto.bringingOverAllowance) {
          Future.successful(Redirect(routes.TravelDetailsController.privateCraft()))
        } else {
          Future.successful(Redirect(routes.TravelDetailsController.noNeedToUseService()))
        }
      }
    )
  }

  val goodsBoughtInsideAndOutsideEu: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(goods_bought_inside_and_outside_eu(BringingOverAllowanceDto.form)))
  }

  def goodsBoughtInsideAndOutsideEuPost: Action[AnyContent] = PublicAction { implicit request =>
    BringingOverAllowanceDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(goods_bought_inside_and_outside_eu(formWithErrors)))
      },
      overAllowanceDto => {
        if (overAllowanceDto.bringingOverAllowance) {
          Future.successful(Redirect(routes.TravelDetailsController.privateCraft()))
        } else {
          Future.successful(Redirect(routes.TravelDetailsController.noNeedToUseService()))
        }
      }
    )
  }

  val noNeedToUseService: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(no_need_to_use_service()))
  }


  def confirmAge: Action[AnyContent] = PublicAction { implicit request =>
    cache.fetch map {
      case Some(JourneyData(_, _, _, _, Some(ageOver17), _, _, _, _, _)) =>
        Ok(confirm_age(AgeOver17Dto.form.bind(Map("ageOver17" -> ageOver17.toString))))
      case _ =>
        Ok(confirm_age(AgeOver17Dto.form))
    }
  }

  def confirmAgePost: Action[AnyContent] = PublicAction { implicit request =>

    AgeOver17Dto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(confirm_age(formWithErrors)))
      },
      ageOver17Dto => {
        travelDetailsService.storeAgeOver17(ageOver17Dto.ageOver17) map { _ =>
          Redirect(routes.DashboardController.showDashboard())
        }
      }
    )
  }

  val privateCraft: Action[AnyContent] = PublicAction { implicit request =>
    cache.fetch map {
      case Some(JourneyData(_, _, _, Some(pc), _, _, _, _, _, _)) =>
        Ok(confirm_private_craft(form.bind(Map("privateCraft" -> pc.toString))))
      case _ =>
        Ok(confirm_private_craft(form))
    }
  }

  val privateCraftPost: Action[AnyContent] = PublicAction { implicit request =>
    form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(confirm_private_craft(formWithErrors)))
      },
      privateCraftDto => {
        travelDetailsService.storePrivateCraft( privateCraftDto.privateCraft ) map { _ =>
          Redirect(routes.TravelDetailsController.confirmAge())
        }
      }
    )
  }
}
