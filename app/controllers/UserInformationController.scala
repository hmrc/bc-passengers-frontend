package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models._
import org.joda.time.DateTime
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
  val payApiService: PayApiService,
  val declarationService: DeclarationService,
  val dateTimeProviderService: DateTimeProviderService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def receiptDateTime: DateTime = dateTimeProviderService.now

  def enterYourDetails: Action[AnyContent] = DashboardAction { implicit context =>
    Future.successful(Ok(views.html.declaration.enter_your_details(EnterYourDetailsDto.form(receiptDateTime))))
  }

  def processEnterYourDetails: Action[AnyContent] = DashboardAction { implicit context =>

    EnterYourDetailsDto.form(receiptDateTime).bindFromRequest.fold(

      formWithErrors => {
        Future.successful(BadRequest(views.html.declaration.enter_your_details(formWithErrors)))
      },
      enterYourDetailsDto => {

        val userInformation = UserInformation.build(enterYourDetailsDto)

        val correlationId = context.sessionId

        userInformationService.storeUserInformation(context.getJourneyData, userInformation) flatMap { _ =>

          requireCalculatorResponse { calculatorResponse =>

            declarationService.submitDeclaration(userInformation, calculatorResponse, receiptDateTime, correlationId) flatMap {

              case DeclarationServiceFailureResponse =>
                Future.successful(InternalServerError(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem.")))

              case DeclarationServiceSuccessResponse(cr) =>

                payApiService.requestPaymentUrl(cr, userInformation, calculatorResponse, (BigDecimal(calculatorResponse.calculation.allTax)*100).toInt, receiptDateTime) map {

                  case PayApiServiceFailureResponse =>
                    InternalServerError(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem."))

                  case PayApiServiceSuccessResponse(url) =>
                    Redirect(url)
                }
            }
          }
        }
      }
    )
  }
}

