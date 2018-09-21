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

import scala.concurrent.Future


@Singleton
class TravelDetailsController @Inject() (
  val countriesService: CountriesService,
  val travelDetailsService: TravelDetailsService,
  val productsService: ProductTreeService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  val newSession: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.TravelDetailsController.selectCountry()).withSession(SessionKeys.sessionId -> UUID.randomUUID.toString))
  }

  val checkDeclareGoodsStartPage: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(views.html.travel_details.check_declare_goods_start_page())
    )
  }


  val selectCountry: Action[AnyContent] = PublicAction { implicit request =>
    travelDetailsService.getJourneyData map {
      case Some(JourneyData(Some(country), _, _, _, _, _)) =>
        Ok(views.html.travel_details.country_of_purchase(SelectedCountryDto.form.bind(Map("country" -> country)), countriesService.getAllCountries))
      case _ =>
        Ok(views.html.travel_details.country_of_purchase(SelectedCountryDto.form, countriesService.getAllCountries))
    }
  }

  val selectCountryPost: Action[AnyContent] = PublicAction { implicit request =>

    SelectedCountryDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.travel_details.country_of_purchase(formWithErrors, countriesService.getAllCountries)))
      },
      selectedCountryDto => {
        val euCountry = countriesService.isInEu(selectedCountryDto.country)

        travelDetailsService.storeCountry( selectedCountryDto.country ) map { _ =>

          if (euCountry) {
            Redirect(routes.TravelDetailsController.euDone())
          } else {
            Redirect(routes.TravelDetailsController.confirmAge())
          }
        }
      }
    )
  }

  def confirmAge: Action[AnyContent] = PublicAction { implicit request =>
    travelDetailsService.getJourneyData map {
      case Some(JourneyData(_, Some(ageOver17), _, _, _, _)) =>
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
        travelDetailsService.storeAgeOver17(ageOver17Dto.ageOver17) flatMap { _ =>
          travelDetailsService.getJourneyData map {
            case Some(JourneyData(_, Some(_), Some(_), _, _, _)) =>
              Redirect(routes.DashboardController.showDashboard())
            case _ =>
              Redirect(routes.TravelDetailsController.privateCraft())
          }
        }
      }
    )
  }


  val privateCraft: Action[AnyContent] = PublicAction { implicit request =>

    travelDetailsService.getJourneyData map {
      case Some(JourneyData(_, _, Some(privateCraft), _, _, _)) =>
        Ok(views.html.travel_details.confirm_private_craft(form.bind(Map("privateCraft" -> privateCraft.toString))))
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
        travelDetailsService.storePrivateCraft( privateCraftDto.privateCraft ) map { _ =>
          Redirect(routes.DashboardController.showDashboard())
        }
      }
    )
  }


  val euDone: Action[AnyContent] = PublicAction { implicit request =>
    Future.successful(Ok(views.html.travel_details.eu_done()))
  }
}
