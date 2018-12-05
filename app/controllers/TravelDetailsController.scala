package controllers

import java.util.UUID

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.PrivateCraftDto._
import models.{SelectedCountryDto, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CountriesService, CurrencyService, ProductTreeService, TravelDetailsService}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class TravelDetailsController @Inject() (
  val countriesService: CountriesService,
  val travelDetailsService: TravelDetailsService,
  val productsService: ProductTreeService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi, val ec: ExecutionContext) extends FrontendController with I18nSupport with ControllerHelpers {

  val newSession: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.TravelDetailsController.euCountryCheck()).withSession(SessionKeys.sessionId -> UUID.randomUUID.toString))
  }

  val checkDeclareGoodsStartPage: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(views.html.travel_details.check_declare_goods_start_page())
    )
  }

  val euCountryCheck: Action[AnyContent] = PublicAction { implicit request => {
    travelDetailsService.getJourneyData map {
      case Some(JourneyData(Some(countryCheck), _, _, _, _, _, _, _)) =>
        Ok(views.html.travel_details.eu_country_check(EuCountryCheckDto.form.fill(EuCountryCheckDto(countryCheck))))
      case _ =>
        Ok(views.html.travel_details.eu_country_check(EuCountryCheckDto.form))
      }
    }
  }

  def euCountryCheckPost: Action[AnyContent] = PublicAction { implicit request =>

    EuCountryCheckDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.travel_details.eu_country_check(formWithErrors)))
      },
      euCountryCheckDto => {
        travelDetailsService.storeEuCountryCheck(euCountryCheckDto.euCountryCheck) flatMap { _ =>
          travelDetailsService.getJourneyData map {
            case Some(JourneyData(Some(_), Some(_), Some(_), _, _, _, _, _)) =>
              Redirect(routes.DashboardController.showDashboard())
            case _ =>
              euCountryCheckDto.euCountryCheck match {
                case "euOnly" => Redirect(routes.TravelDetailsController.euDone())
                case "nonEuOnly" => Redirect(routes.TravelDetailsController.nonEuInterrupt())
                case "both" =>Redirect(routes.TravelDetailsController.bothInterrupt())
              }
          }
        }
      }
    )
  }

  val nonEuInterrupt: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(views.html.travel_details.interrupt_page(mixEuRow = false)))
  }

  def interruptPost: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Redirect(routes.TravelDetailsController.privateCraft()))
  }

  val bothInterrupt: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(views.html.travel_details.interrupt_page(mixEuRow = true)))
  }

  def confirmAge: Action[AnyContent] = PublicAction { implicit request =>
    travelDetailsService.getJourneyData map {
      case Some(JourneyData(_, _, Some(ageOver17), _, _, _, _, _)) =>
        Ok(views.html.travel_details.confirm_age(AgeOver17Dto.form.bind(Map("ageOver17" -> ageOver17.toString))))
      case _ =>
        Ok(views.html.travel_details.confirm_age(AgeOver17Dto.form))
    }
  }

  def confirmAgePost: Action[AnyContent] = PublicAction { implicit request =>

    AgeOver17Dto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.travel_details.confirm_age(formWithErrors)))
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
      case Some(JourneyData(_, Some(pc), _, _, _, _, _, _)) =>
        Ok(views.html.travel_details.confirm_private_craft(form.bind(Map("privateCraft" -> pc.toString))))
      case _ =>
        Ok(views.html.travel_details.confirm_private_craft(form))
    }
  }

  val privateCraftPost: Action[AnyContent] = PublicAction { implicit request =>
    form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.travel_details.confirm_private_craft(formWithErrors)))
      },
      privateCraftDto => {
        travelDetailsService.storePrivateCraft( privateCraftDto.privateCraft ) flatMap { _ =>
          travelDetailsService.getJourneyData map {
            case Some(JourneyData(_, Some(_), Some(_), _, _, _, _, _)) =>
              Redirect(routes.DashboardController.showDashboard())
            case _ =>
              Redirect(routes.TravelDetailsController.confirmAge())
          }
        }
      }
    )
  }


  val euDone: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(views.html.travel_details.eu_done()))
  }
}
