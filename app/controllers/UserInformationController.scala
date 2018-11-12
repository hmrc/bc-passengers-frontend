package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class UserInformationController @Inject()(
  val travelDetailsService: TravelDetailsService,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService,
  val countriesService: CountriesService,
  val userInformationService: UserInformationService,
  val payApiService: PayApiService
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

        userInformationService.storeUserInformation(context.getJourneyData, UserInformation.build(enterYourDetailsDto)) flatMap { _ =>

          requireCalculatorResponse { calculatorResponse =>

            payApiService.requestPaymentUrl(ChargeReference("XJPR1234567893"), (BigDecimal(calculatorResponse.calculation.allTax)*100).toInt) map {

              case PayApiServiceFailureResponse =>
                InternalServerError(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem."))

              case PayApiServiceSuccessResponse(url) =>
                Redirect(url)
            }
          }
        }
      }
    )
  }
}

