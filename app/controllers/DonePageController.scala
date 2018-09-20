package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.{AgeOver17Dto, EnterYourDetailsDto, JourneyData, UserInformation}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class DonePageController @Inject() (
  val travelDetailsService: TravelDetailsService,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService,
  val userInformationService: UserInformationService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def enterYourDetails: Action[AnyContent] = DashboardAction { implicit context =>
    Future.successful(Ok(views.html.declaration.enter_your_details(EnterYourDetailsDto.form)))
  }

  def processEnterYourDetails: Action[AnyContent] = DashboardAction { implicit context =>

    EnterYourDetailsDto.form.bindFromRequest.fold(

      formWithErrors => {
        Future.successful(BadRequest(views.html.declaration.enter_your_details(formWithErrors)))
      },
      enterYourDetailsDto => {
        userInformationService.storeUserInformation(context.getJourneyData, UserInformation.build(enterYourDetailsDto)) map { _ =>
          val output =
            s"""
              |First Name: ${enterYourDetailsDto.firstName}
              |Last Name: ${enterYourDetailsDto.lastName}
              |Passport Number: ${enterYourDetailsDto.passportNumber}
              |Place of Arrival: ${enterYourDetailsDto.placeOfArrival}
              |Date of Arrival: ${enterYourDetailsDto.dateOfArrival}
            """.stripMargin
          Ok(output)
        }
      }
    )
  }
}

