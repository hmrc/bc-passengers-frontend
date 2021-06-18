/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import java.util.UUID
import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, DeclareAction, PublicAction, UserInfoAction}
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
  val portsOfArrivalService: PortsOfArrivalService,
  val backLinkModel: BackLinkModel,
  val travelDetailsService: TravelDetailsService,
  val userInformationService: UserInformationService,
  val payApiService: PayApiService,
  val declarationService: DeclarationService,
  val dateTimeProviderService: DateTimeProviderService,

  publicAction: PublicAction,
  dashboardAction: DashboardAction,
  declareAction: DeclareAction,
  userInfoAction: UserInfoAction,

  val you_need_to_declare: views.html.declaration.declare_your_goods,
  val zero_to_declare_your_goods: views.html.declaration.zero_to_declare_your_goods,
  val enter_your_details: views.html.declaration.enter_your_details,
  val error_template: views.html.error_template,
  val irish_border : views.html.travel_details.irish_border,
  val purchase_price_out_of_bounds: views.html.errors.purchase_price_out_of_bounds,
  val nothing_to_declare: views.html.purchased_products.nothing_to_declare,
  val zero_to_declare: views.html.purchased_products.zero_to_declare,
  val done: views.html.purchased_products.done,
  val over_ninety_seven_thousand_pounds: views.html.purchased_products.over_ninety_seven_thousand_pounds,
  val zero_declaration: views.html.declaration.zero_declaration,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext

) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def receiptDateTime: DateTime = dateTimeProviderService.now

  def declareYourGoods: Action[AnyContent] = declareAction { implicit context =>

    def checkZeroPoundCondition(calculatorResponse: CalculatorResponse): Boolean = {
      val calcTax = BigDecimal(calculatorResponse.calculation.allTax)
      calculatorResponse.isAnyItemOverAllowance && context.getJourneyData.euCountryCheck.contains("greatBritain") && calcTax == 0
    }
    def checkZeroPoundConditionForAmendment(calculatorResponse:CalculatorResponse, deltaAlltax:String):Boolean = {
      calculatorResponse.isAnyItemOverAllowance && context.getJourneyData.euCountryCheck.contains("greatBritain") && deltaAlltax == "0.00"
    }

    requireCalculatorResponse { calculatorResponse =>
      val isAmendment = context.getJourneyData.declarationResponse.isDefined
      Future.successful {
        if (checkZeroPoundCondition(calculatorResponse) ||
          (context.getJourneyData.deltaCalculation.isDefined &&
            checkZeroPoundConditionForAmendment(calculatorResponse,context.getJourneyData.deltaCalculation.get.allTax)))
          Ok(zero_to_declare_your_goods(calculatorResponse.asDto(applySorting = false), calculatorResponse.allItemsUseGBP, isAmendment, backLinkModel.backLink))
        else {
          Ok(you_need_to_declare(isAmendment, backLinkModel.backLink))
        }
      }
    }
  }

  def enterYourDetails: Action[AnyContent] = userInfoAction { implicit context =>
    context.getJourneyData.userInformation match {

      case Some(userInformation) =>
        context.getJourneyData.euCountryCheck match {
          case Some("greatBritain") => Future.successful(Ok(enter_your_details(EnterYourDetailsDto.form(receiptDateTime).fill(EnterYourDetailsDto.fromUserInformation(userInformation)), portsOfArrivalService.getAllPortsNI, context.getJourneyData.euCountryCheck, backLinkModel.backLink) ) )
          case _ => Future.successful(Ok(enter_your_details(EnterYourDetailsDto.form(receiptDateTime).fill(EnterYourDetailsDto.fromUserInformation(userInformation)), portsOfArrivalService.getAllPorts, context.getJourneyData.euCountryCheck, backLinkModel.backLink)))
        }
      case _ =>
        context.getJourneyData.euCountryCheck match {
          case Some("greatBritain") => Future.successful(Ok(enter_your_details(EnterYourDetailsDto.form(receiptDateTime), portsOfArrivalService.getAllPortsNI, context.getJourneyData.euCountryCheck, backLinkModel.backLink)))
          case _ => Future.successful(Ok(enter_your_details(EnterYourDetailsDto.form(receiptDateTime), portsOfArrivalService.getAllPorts, context.getJourneyData.euCountryCheck, backLinkModel.backLink)))
        }
    }
  }

  def processEnterYourDetails: Action[AnyContent] = dashboardAction { implicit context =>

    EnterYourDetailsDto.form(receiptDateTime).bindFromRequest.fold(

      formWithErrors => {
        context.getJourneyData.euCountryCheck match {
          case Some("greatBritain") => Future.successful(BadRequest(enter_your_details(formWithErrors, portsOfArrivalService.getAllPortsNI, context.getJourneyData.euCountryCheck, backLinkModel.backLink)))
          case _ => Future.successful(BadRequest(enter_your_details(formWithErrors, portsOfArrivalService.getAllPorts, context.getJourneyData.euCountryCheck, backLinkModel.backLink)))
        }
      },
      enterYourDetailsDto => {

        val userInformation = UserInformation.build(enterYourDetailsDto)

        val correlationId = UUID.randomUUID.toString

        userInformationService.storeUserInformation(context.getJourneyData, userInformation) flatMap { _ =>

          requireCalculatorResponse { calculatorResponse =>

            declarationService.submitDeclaration(userInformation, calculatorResponse, context.getJourneyData, receiptDateTime, correlationId) flatMap {

              case DeclarationServiceFailureResponse =>
                Future.successful(InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem.")))

              case DeclarationServiceSuccessResponse(cr) =>

                BigDecimal(calculatorResponse.calculation.allTax) match {
                  case allTax if allTax == 0  && context.getJourneyData.euCountryCheck.contains("greatBritain") && calculatorResponse.isAnyItemOverAllowance =>
                    declarationService.storeChargeReference(context.getJourneyData, userInformation, cr.value) flatMap { _ =>
                      Future.successful(Redirect(routes.ZeroDeclarationController.loadDeclarationPage()))
                    }
                  case _ => payApiService.requestPaymentUrl(cr, userInformation, calculatorResponse, (BigDecimal(calculatorResponse.calculation.allTax) * 100).toInt, false, None) map {

                    case PayApiServiceFailureResponse =>
                      InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem."))

                    case PayApiServiceSuccessResponse(url) =>
                      Redirect(url)
                  }
                }
            }
          }
        }
      }
    )
  }

  def processAmendment: Action[AnyContent] = dashboardAction { implicit context =>

    val correlationId = UUID.randomUUID.toString
    val userInformation = context.getJourneyData.userInformation.getOrElse(throw new RuntimeException("no user Information"))
    val amendState = context.getJourneyData.amendState.getOrElse("")
    requireCalculatorResponse { calculatorResponse =>
      val amountPaidPreviously = if(amendState.equals("pending-payment")) calculatorService.getPreviousPaidCalculation(context.getJourneyData.deltaCalculation.get, calculatorResponse.calculation).allTax else context.getJourneyData.declarationResponse.get.calculation.allTax
      declarationService.submitAmendment(userInformation, calculatorResponse, context.getJourneyData, receiptDateTime, correlationId) flatMap {

        case DeclarationServiceFailureResponse =>
          Future.successful(InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem.")))

        case DeclarationServiceSuccessResponse(cr) =>

          BigDecimal(context.getJourneyData.deltaCalculation.get.allTax) match {
            case deltaAllTax if deltaAllTax == 0  && context.getJourneyData.euCountryCheck.contains("greatBritain") && calculatorResponse.isAnyItemOverAllowance =>
              declarationService.storeChargeReference(context.getJourneyData, userInformation, cr.value) flatMap { _ =>
                Future.successful(Redirect(routes.ZeroDeclarationController.loadDeclarationPage()))
              }
            case deltaAllTax => payApiService.requestPaymentUrl(cr, userInformation, calculatorResponse, (deltaAllTax * 100).toInt, true, Some(amountPaidPreviously), Some(amendState)) map {

              case PayApiServiceFailureResponse =>
                InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem."))

              case PayApiServiceSuccessResponse(url) =>
                Redirect(url)
            }
          }
      }
    }
  }

  def irishBorder: Action[AnyContent] = publicAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _, _,_, _, _, _, _, _, _, _, Some(irishBorder), _, _, _, _, _, _,_,_,_,_,_,_,_, _, _)) =>
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

  private def doCalculateAction(journeyData: JourneyData)(implicit context: LocalContext): Future[Result] = calculatorService.calculate(journeyData) flatMap {

    case CalculatorServiceSuccessResponse(calculatorResponse) =>

      val oldCalculation: Option[Calculation] = journeyData.declarationResponse.map(_.calculation)
      val currentCalculation: Calculation = calculatorResponse.calculation

      if (!oldCalculation.isDefined) calculatorService.storeCalculatorResponse(journeyData, calculatorResponse) map { _ =>
        Redirect(routes.CalculateDeclareController.showCalculation())
      } else {
        val deltaCalculation = calculatorService.getDeltaCalculation(oldCalculation.get, currentCalculation)
        calculatorService.storeCalculatorResponse(journeyData, calculatorResponse, Some(deltaCalculation)) map { _ =>
          Redirect(routes.CalculateDeclareController.showCalculation())
        }
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

  def showCalculation: Action[AnyContent] = dashboardAction {implicit context =>

    def checkZeroPoundCondition(calculatorResponse:CalculatorResponse):Boolean = {
      val calcTax = BigDecimal(calculatorResponse.calculation.allTax)
     calculatorResponse.isAnyItemOverAllowance && context.getJourneyData.euCountryCheck.contains("greatBritain") && calcTax == 0
    }
    def checkZeroPoundConditionForAmendment(calculatorResponse:CalculatorResponse, deltaAlltax:String):Boolean = {
      calculatorResponse.isAnyItemOverAllowance && context.getJourneyData.euCountryCheck.contains("greatBritain") && deltaAlltax == "0.00"
    }
    val deltaCalc: Option[Calculation] = context.getJourneyData.deltaCalculation
    val declarationResponse = context.getJourneyData.declarationResponse
    requireCalculatorResponse { calculatorResponse =>
      Future.successful {
        if(declarationResponse.isDefined){
          val oldTax = declarationResponse.get.calculation.allTax
          BigDecimal(deltaCalc.get.allTax) match {
            case _ if checkZeroPoundConditionForAmendment(calculatorResponse,deltaCalc.get.allTax) =>
              Ok(zero_to_declare(true, calculatorResponse.asDto(applySorting = false), deltaCalc, oldTax, calculatorResponse.allItemsUseGBP, backLinkModel.backLink))

            case allTax if allTax == 0 && calculatorResponse.withinFreeAllowance =>
              Ok(nothing_to_declare(true, calculatorResponse.asDto(applySorting = false), deltaCalc, oldTax, calculatorResponse.allItemsUseGBP, underNinePounds = false, backLinkModel.backLink))

            case allTax if allTax == 0 && !calculatorResponse.withinFreeAllowance =>
              Ok(nothing_to_declare(true, calculatorResponse.asDto(applySorting = false), deltaCalc, oldTax, calculatorResponse.allItemsUseGBP, underNinePounds = true, backLinkModel.backLink))

            case allTax if allTax > appConfig.paymentLimit =>
              Ok(over_ninety_seven_thousand_pounds(true, calculatorResponse.asDto(applySorting = true), deltaCalc, oldTax, calculatorResponse.allItemsUseGBP, backLinkModel.backLink))

            case _ =>
              Ok(done(true, calculatorResponse.asDto(applySorting = true), deltaCalc, oldTax, calculatorResponse.allItemsUseGBP, backLinkModel.backLink))
          }
        }
        else {
          BigDecimal(calculatorResponse.calculation.allTax) match {
            case _ if checkZeroPoundCondition(calculatorResponse) =>
              Ok(zero_to_declare(false, calculatorResponse.asDto(applySorting = false), None, "", calculatorResponse.allItemsUseGBP, backLinkModel.backLink))

            case allTax if allTax == 0 && calculatorResponse.withinFreeAllowance =>
              Ok(nothing_to_declare(false, calculatorResponse.asDto(applySorting = false), None, "", calculatorResponse.allItemsUseGBP, underNinePounds = false, backLinkModel.backLink))

            case allTax if allTax == 0 && !calculatorResponse.withinFreeAllowance =>
              Ok(nothing_to_declare(false, calculatorResponse.asDto(applySorting = false), None, "", calculatorResponse.allItemsUseGBP, underNinePounds = true, backLinkModel.backLink))

            case allTax if allTax > appConfig.paymentLimit =>
              Ok(over_ninety_seven_thousand_pounds(false, calculatorResponse.asDto(applySorting = true), None, "", calculatorResponse.allItemsUseGBP, backLinkModel.backLink))

            case _ =>
              Ok(done(false, calculatorResponse.asDto(applySorting = true), None, "", calculatorResponse.allItemsUseGBP, backLinkModel.backLink))
          }
        }
      }
    }
  }
}
