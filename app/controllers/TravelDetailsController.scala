package controllers

import java.util.UUID

import config.AppConfig
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
  val productsService: ProductTreeService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService,
  val interrupt_page: views.html.travel_details.interrupt_page,
  val check_declare_goods_start_page: views.html.travel_details.check_declare_goods_start_page,
  val eu_country_check: views.html.travel_details.eu_country_check,
  val confirm_age: views.html.travel_details.confirm_age,
  val goods_bought_inside_eu: views.html.travel_details.goods_bought_inside_eu,
  val confirm_private_craft: views.html.travel_details.confirm_private_craft,
  val error_template: views.html.error_template,
  val vat_res: views.html.travel_details.vat_res,
  val duty_free: views.html.travel_details.duty_free,
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
    travelDetailsService.getJourneyData map {
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
          travelDetailsService.getJourneyData map {
            case Some(JourneyData(Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _)) =>
              Redirect(routes.DashboardController.showDashboard())
            case _ =>
              if (appConfig.usingVatResJourney) {
                euCountryCheckDto.euCountryCheck match {
                  case "euOnly" => Redirect(routes.TravelDetailsController.didYouClaimTaxBack())
                  case "nonEuOnly" => Redirect(routes.TravelDetailsController.nonEuInterrupt())
                  case "both" => Redirect(routes.TravelDetailsController.didYouClaimTaxBack())
                }
              } else {
                euCountryCheckDto.euCountryCheck match {
                  case "euOnly" => Redirect(routes.TravelDetailsController.goodsBoughtInsideEu())
                  case "nonEuOnly" => Redirect(routes.TravelDetailsController.nonEuInterrupt())
                  case "both" =>Redirect(routes.TravelDetailsController.bothInterrupt())
                }
              }
          }
        }
      }
    )
  }

  def didYouClaimTaxBack: Action[AnyContent] = PublicAction { implicit request =>
    travelDetailsService.getJourneyData map {
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
    travelDetailsService.getJourneyData map {
      case Some(JourneyData(_, _, Some(bringingDutyFree), _, _, _, _, _, _, _)) =>
        Ok(duty_free(BringingDutyFreeDto.form.fill(BringingDutyFreeDto(bringingDutyFree))))
      case _ =>
        Ok(duty_free(BringingDutyFreeDto.form))
    }
  }

  def dutyFreePost: Action[AnyContent] = PublicAction { implicit request =>
    BringingDutyFreeDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(duty_free(formWithErrors)))
      },
      bringingDutyFreeDto => {
        travelDetailsService.storeDutyFreeCheck(bringingDutyFreeDto.bringingDutyFree) flatMap { _ =>
          if (!bringingDutyFreeDto.bringingDutyFree) {
            travelDetailsService.getJourneyData map {
              case Some(jd) if jd.euCountryCheck.contains("euOnly") =>
                Redirect(routes.TravelDetailsController.goodsBoughtInsideEu())
              case Some(jd) if jd.euCountryCheck.contains("both") =>
                Redirect(routes.TravelDetailsController.bothInterrupt())
              case _ =>
                Redirect(routes.TravelDetailsController.privateCraft())
            }
          } else {
            Future.successful(Redirect(routes.TravelDetailsController.privateCraft()))
          }
        }
      }
    )
  }

  val nonEuInterrupt: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(interrupt_page(mixEuRow = false)))
  }

  val bothInterrupt: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(interrupt_page(mixEuRow = true)))
  }

  def confirmAge: Action[AnyContent] = PublicAction { implicit request =>
    travelDetailsService.getJourneyData map {
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

    travelDetailsService.getJourneyData map {
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
        travelDetailsService.storePrivateCraft( privateCraftDto.privateCraft ) flatMap { _ =>
          travelDetailsService.getJourneyData map {
            case Some(JourneyData(_, _, _, Some(_), Some(_), _, _, _, _, _)) =>
              Redirect(routes.DashboardController.showDashboard())
            case _ =>
              Redirect(routes.TravelDetailsController.confirmAge())
          }
        }
      }
    )
  }

  val goodsBoughtInsideEu: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(goods_bought_inside_eu()))
  }

}
