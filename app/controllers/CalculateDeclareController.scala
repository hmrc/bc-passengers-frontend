/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import java.util.UUID

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, PublicAction, DeclareAction}
import javax.inject.{Inject, Singleton}
import models._
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculateDeclareController @Inject()(
  val cache: Cache,
  val calculatorService: CalculatorService,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService,
  val countriesService: CountriesService,
  val backLinkModel: BackLinkModel,
  val travelDetailsService: TravelDetailsService,
  val userInformationService: UserInformationService,
  val payApiService: PayApiService,
  val declarationService: DeclarationService,
  val dateTimeProviderService: DateTimeProviderService,

  publicAction: PublicAction,
  dashboardAction: DashboardAction,
  declareAction: DeclareAction,

  val you_need_to_declare: views.html.declaration.declare_your_goods,
  val enter_your_details: views.html.declaration.enter_your_details,
  val error_template: views.html.error_template,
  val irish_border : views.html.travel_details.irish_border,
  val purchase_price_out_of_bounds: views.html.errors.purchase_price_out_of_bounds,
  val nothing_to_declare: views.html.purchased_products.nothing_to_declare,
  val done: views.html.purchased_products.done,
  val over_ninety_seven_thousand_pounds: views.html.purchased_products.over_ninety_seven_thousand_pounds,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
                                          
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def receiptDateTime: DateTime = dateTimeProviderService.now

  def declareYourGoods: Action[AnyContent] = declareAction { implicit context =>
    Future.successful(Ok(you_need_to_declare()))
  }

  def enterYourDetails: Action[AnyContent] = declareAction { implicit context =>
    Future.successful(Ok(enter_your_details(EnterYourDetailsDto.form(receiptDateTime))))
  }

  def processEnterYourDetails: Action[AnyContent] = dashboardAction { implicit context =>

    EnterYourDetailsDto.form(receiptDateTime).bindFromRequest.fold(

      formWithErrors => {
        Future.successful(BadRequest(enter_your_details(formWithErrors)))
      },
      enterYourDetailsDto => {

        val userInformation = UserInformation.build(enterYourDetailsDto)

        val correlationId = UUID.randomUUID.toString

        userInformationService.storeUserInformation(context.getJourneyData, userInformation) flatMap { _ =>

          requireCalculatorResponse { calculatorResponse =>

            declarationService.submitDeclaration(userInformation, calculatorResponse, context.getJourneyData.isVatResClaimed.getOrElse(false), context.getJourneyData.isBringingDutyFree.getOrElse(false), receiptDateTime, correlationId) flatMap {

              case DeclarationServiceFailureResponse =>
                Future.successful(InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem.")))

              case DeclarationServiceSuccessResponse(cr) =>

                payApiService.requestPaymentUrl(cr, userInformation, calculatorResponse, (BigDecimal(calculatorResponse.calculation.allTax)*100).toInt, receiptDateTime) map {

                  case PayApiServiceFailureResponse =>
                    InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem."))

                  case PayApiServiceSuccessResponse(url) =>
                    Redirect(url)
                }
            }
          }
        }
      }
    )
  }

  def irishBorder: Action[AnyContent] = publicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _,_,_, _, _, _, Some(irishBorder), _, _, _, _, _, _,_)) =>
          Ok(irish_border(IrishBorderDto.form.bind(Map("irishBorder" -> irishBorder.toString)), backLinkModel.backLink))
        case _ =>
          Ok(irish_border(IrishBorderDto.form, backLinkModel.backLink))
      }
    }
  }

  def irishBorderPost: Action[AnyContent] = publicAction { implicit context =>
    IrishBorderDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(irish_border(formWithErrors, backLinkModel.backLink)))
      },
      irishBorderDto => {

        travelDetailsService.storeIrishBorder(context.journeyData)(irishBorderDto.irishBorder) flatMap { _ =>

          val updatedJourneyData = context.getJourneyData.copy(irishBorder = Some(irishBorderDto.irishBorder))

          doCalculateAction(updatedJourneyData)
        }
      }
    )
  }

  def calculate: Action[AnyContent] = dashboardAction { implicit context =>

    doCalculateAction(context.getJourneyData)
  }

  private def doCalculateAction(journeyData: JourneyData)(implicit context: LocalContext): Future[Result] = {

    calculatorService.calculate(journeyData) flatMap {

      case CalculatorServiceSuccessResponse(calculatorResponse) =>

        calculatorService.storeCalculatorResponse(journeyData, calculatorResponse) map { _ =>
          Redirect(routes.CalculateDeclareController.showCalculation())
        }

      case CalculatorServicePurchasePriceOutOfBoundsFailureResponse =>

        Future.successful {
          BadRequest(purchase_price_out_of_bounds())
        }

      case _ =>
        Future.successful {
          InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem."))
        }
    }
  }

  def showCalculation: Action[AnyContent] = dashboardAction { implicit context =>
    requireCalculatorResponse { calculatorResponse =>

      Future.successful {
        BigDecimal(calculatorResponse.calculation.allTax) match {
          case allTax if allTax == 0 && calculatorResponse.withinFreeAllowance =>
            Ok( nothing_to_declare(calculatorResponse.asDto(applySorting = false), calculatorResponse.allItemsUseGBP, false, backLinkModel.backLink))

          case allTax if allTax > 0 && allTax < appConfig.minPaymentAmount || allTax == 0 && !calculatorResponse.withinFreeAllowance =>
            Ok( nothing_to_declare(calculatorResponse.asDto(applySorting = false), calculatorResponse.allItemsUseGBP, true, backLinkModel.backLink))

          case allTax if allTax > appConfig.paymentLimit  =>
            Ok( over_ninety_seven_thousand_pounds(calculatorResponse.asDto(applySorting = true), calculatorResponse.allItemsUseGBP, backLinkModel.backLink))

          case _ => Ok( done(calculatorResponse.asDto(applySorting = true), calculatorResponse.allItemsUseGBP, backLinkModel.backLink) )
        }
      }
    }
  }
}

