package controllers

import java.util.UUID
import javax.inject.{Inject, Singleton}

import config.AppConfig
import models.{SelectedCountryDto, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Action
import services.{CountriesService, TravelDetailsService}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future


@Singleton
class TravelDetailsController @Inject() (
  val countriesService: CountriesService,
  val travelDetailsService: TravelDetailsService,
  val messagesApi: MessagesApi,
  implicit val appConfig: AppConfig
) extends FrontendController with I18nSupport with PublicActions {

  val start = Action.async { implicit request =>
    Future.successful {
      Redirect(routes.TravelDetailsController.selectCountry()).addingToSession(SessionKeys.sessionId -> UUID.randomUUID.toString)
    }
  }


  val selectCountry = PublicAction { implicit request =>
    travelDetailsService.getUserInputData map {
      case Some(JourneyData(Some(country), _, _)) =>
        Ok(views.html.passengers.country_of_purchase(SelectedCountryDto.form.bind(Map("country" -> country)), countriesService.getAllCountries))
      case _ =>
        Ok(views.html.passengers.country_of_purchase(SelectedCountryDto.form, countriesService.getAllCountries))
    }
  }

  val selectCountryPost = PublicAction { implicit request =>

    SelectedCountryDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.passengers.country_of_purchase(formWithErrors, countriesService.getAllCountries)))
      },
      selectedCountryDto => {
        val euCountry = countriesService.isInEu(selectedCountryDto.country)

        if (euCountry) {
          Future.successful(Redirect(routes.TravelDetailsController.euDone()))
        } else {
          travelDetailsService.storeCountry( selectedCountryDto.country )
          Future.successful(Redirect(routes.TravelDetailsController.confirmAge()))
        }
      }
    )
  }




  val confirmAge = PublicAction { implicit request =>
    travelDetailsService.getUserInputData map {
      case Some(JourneyData(_, Some(ageOver17), _)) =>
        Ok(views.html.passengers.confirm_age(AgeOver17Dto.form.bind(Map("ageOver17" -> ageOver17.toString))))
      case _ =>
        Ok(views.html.passengers.confirm_age(AgeOver17Dto.form))
    }
  }

  val confirmAgePost = PublicAction { implicit request =>

    AgeOver17Dto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.passengers.confirm_age(formWithErrors)))
      },
      ageOver17Dto => {
        travelDetailsService.storeAgeOver17( ageOver17Dto.ageOver17 )
        Future.successful(Redirect(routes.TravelDetailsController.privateCraft()))
      }
    )
  }




  val privateCraft = PublicAction { implicit request =>

    travelDetailsService.getUserInputData map {
      case Some(JourneyData(_, _, Some(privateCraft))) =>
        Ok(views.html.passengers.confirm_private_craft(PrivateCraftDto.form.bind(Map("privateCraft" -> privateCraft.toString))))
      case _ =>
        Ok(views.html.passengers.confirm_private_craft(PrivateCraftDto.form))
    }
  }

  val privateCraftPost = PublicAction { implicit request =>
    PrivateCraftDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.passengers.confirm_private_craft(formWithErrors)))
      },
      privateCraftDto => {
        travelDetailsService.storePrivateCraft( privateCraftDto.privateCraft )
        Future.successful(Redirect(routes.TravelDetailsController.productDashboard()))
      }
    )
  }




  val productDashboard = PublicAction { implicit request =>

    travelDetailsService.getUserInputData map {
      case Some(journeyData) =>
        Ok(views.html.passengers.dashboard(journeyData))
      case None =>
        Ok(views.html.passengers.dashboard(JourneyData()))
    }


  }




  val euDone = PublicAction { implicit request =>
    Future.successful(Ok(views.html.passengers.eu_done()))
  }

}
