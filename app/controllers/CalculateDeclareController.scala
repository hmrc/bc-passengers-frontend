/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, DeclareAction, PublicAction, UserInfoAction}
import controllers.ControllerHelpers
import models._
import java.time.LocalDateTime
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculateDeclareController @Inject() (
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
  val errorTemplate: views.html.errorTemplate,
  val irish_border: views.html.travel_details.irish_border,
  val purchase_price_out_of_bounds: views.html.errors.purchase_price_out_of_bounds,
  val nothing_to_declare: views.html.purchased_products.nothing_to_declare,
  val zero_to_declare: views.html.purchased_products.zero_to_declare,
  val done: views.html.purchased_products.done,
  val over_ninety_seven_thousand_pounds: views.html.purchased_products.over_ninety_seven_thousand_pounds,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  def receiptDateTime: LocalDateTime = dateTimeProviderService.now

  def declareYourGoods: Action[AnyContent] = declareAction { implicit context =>
    def checkZeroPoundCondition(calculatorResponse: CalculatorResponse): Boolean                                  = {
      val calcTax = BigDecimal(calculatorResponse.calculation.allTax)
      calculatorResponse.isAnyItemOverAllowance && context.getJourneyData.euCountryCheck.contains(
        "greatBritain"
      ) && calcTax == 0
    }
    def checkZeroPoundConditionForAmendment(calculatorResponse: CalculatorResponse, deltaAllTax: String): Boolean =
      calculatorResponse.isAnyItemOverAllowance && context.getJourneyData.euCountryCheck.contains(
        "greatBritain"
      ) && deltaAllTax == "0.00"

    requireCalculatorResponse { calculatorResponse =>
      val isAmendment = context.getJourneyData.declarationResponse.isDefined
      Future.successful {
        if (
          checkZeroPoundCondition(calculatorResponse) ||
          (context.getJourneyData.deltaCalculation.isDefined &&
            checkZeroPoundConditionForAmendment(calculatorResponse, context.getJourneyData.deltaCalculation.get.allTax))
        ) {
          Ok(
            zero_to_declare_your_goods(
              calculatorResponse.asDto(applySorting = false),
              calculatorResponse.allItemsUseGBP,
              isAmendment,
              backLinkModel.backLink
            )
          )
        } else {
          Ok(you_need_to_declare(isAmendment, backLinkModel.backLink))
        }
      }
    }
  }

  def enterYourDetails: Action[AnyContent] = userInfoAction { implicit context =>
    context.getJourneyData.userInformation match {

      case Some(userInformation) =>
        context.getJourneyData.euCountryCheck match {
          case Some("greatBritain") =>
            Future.successful(
              Ok(
                enter_your_details(
                  EnterYourDetailsDto
                    .form(receiptDateTime)
                    .fill(EnterYourDetailsDto.fromUserInformation(userInformation)),
                  portsOfArrivalService.getAllPortsNI,
                  context.getJourneyData.euCountryCheck,
                  backLinkModel.backLink
                )
              )
            )
          case _                    =>
            Future.successful(
              Ok(
                enter_your_details(
                  EnterYourDetailsDto
                    .form(receiptDateTime)
                    .fill(EnterYourDetailsDto.fromUserInformation(userInformation)),
                  portsOfArrivalService.getAllPorts,
                  context.getJourneyData.euCountryCheck,
                  backLinkModel.backLink
                )
              )
            )
        }
      case _                     =>
        context.getJourneyData.euCountryCheck match {
          case Some("greatBritain") =>
            Future.successful(
              Ok(
                enter_your_details(
                  EnterYourDetailsDto.form(receiptDateTime),
                  portsOfArrivalService.getAllPortsNI,
                  context.getJourneyData.euCountryCheck,
                  backLinkModel.backLink
                )
              )
            )
          case _                    =>
            Future.successful(
              Ok(
                enter_your_details(
                  EnterYourDetailsDto.form(receiptDateTime),
                  portsOfArrivalService.getAllPorts,
                  context.getJourneyData.euCountryCheck,
                  backLinkModel.backLink
                )
              )
            )
        }
    }
  }

  def processEnterYourDetails: Action[AnyContent] = dashboardAction { implicit context =>
    val form      = EnterYourDetailsDto.form(receiptDateTime)
    val boundForm = form.bindFromRequest()

    boundForm.fold(
      formWithErrors => {
        val ports = context.getJourneyData.euCountryCheck match {
          case Some("greatBritain") => portsOfArrivalService.getAllPortsNI
          case _                    => portsOfArrivalService.getAllPorts
        }
        Future.successful(
          BadRequest(
            enter_your_details(formWithErrors, ports, context.getJourneyData.euCountryCheck, backLinkModel.backLink)
          )
        )
      },
      enterYourDetailsDto => {
        val userInformation = UserInformation.build(enterYourDetailsDto)
        val correlationId   = UUID.randomUUID.toString
        userInformationService.storeUserInformation(context.getJourneyData, userInformation) flatMap { _ =>
          requireCalculatorResponse { calculatorResponse =>
            val allTax            = BigDecimal(calculatorResponse.calculation.allTax)
            val declarationResult = declarationService.submitDeclaration(
              userInformation,
              calculatorResponse,
              context.getJourneyData,
              receiptDateTime,
              correlationId
            )
            declarationResult flatMap {
              case DeclarationServiceFailureResponse     =>
                Future.successful(InternalServerError(errorTemplate()))
              case DeclarationServiceSuccessResponse(cr) =>
                if (
                  allTax == 0 && context.getJourneyData.euCountryCheck
                    .contains("greatBritain") && calculatorResponse.isAnyItemOverAllowance
                ) {
                  declarationService.storeChargeReference(context.getJourneyData, userInformation, cr.value) flatMap {
                    _ =>
                      Future.successful(Redirect(routes.ZeroDeclarationController.loadDeclarationPage()))
                  }
                } else {
                  payApiService.requestPaymentUrl(
                    cr,
                    userInformation,
                    calculatorResponse,
                    (allTax * 100).toInt,
                    isAmendment = false,
                    None
                  ) flatMap {
                    case PayApiServiceFailureResponse      => Future.successful(InternalServerError(errorTemplate()))
                    case PayApiServiceSuccessResponse(url) => Future.successful(Redirect(url))
                  }
                }
            }
          }
        }
      }
    )
  }

  def processAmendment: Action[AnyContent] = dashboardAction { implicit context =>
    val correlationId   = UUID.randomUUID.toString
    val userInformation =
      context.getJourneyData.userInformation.getOrElse(throw new RuntimeException("no user Information"))
    val amendState      = context.getJourneyData.amendState.getOrElse("")
    requireCalculatorResponse { calculatorResponse =>
      val amountPaidPreviously = if (amendState.equals("pending-payment")) {
        calculatorService
          .getPreviousPaidCalculation(context.getJourneyData.deltaCalculation.get, calculatorResponse.calculation)
          .allTax
      } else {
        context.getJourneyData.declarationResponse.get.calculation.allTax
      }
      declarationService.submitAmendment(
        userInformation,
        calculatorResponse,
        context.getJourneyData,
        receiptDateTime,
        correlationId
      ) flatMap {
        case DeclarationServiceFailureResponse     =>
          Future.successful(InternalServerError(errorTemplate()))
        case DeclarationServiceSuccessResponse(cr) =>
          handleAmendResult(
            cr,
            calculatorResponse,
            userInformation,
            context.getJourneyData,
            amountPaidPreviously.toDouble,
            amendState
          )
      }
    }
  }

  private def handleAmendResult(
    cr: ChargeReference,
    calculatorResponse: CalculatorResponse,
    userInformation: UserInformation,
    context: JourneyData,
    amountPaidPreviously: BigDecimal,
    amendState: String
  )(implicit hc: HeaderCarrier, request: Request[?]): Future[Result] = {
    val deltaAllTax = BigDecimal(context.deltaCalculation.get.allTax)
    if (
      deltaAllTax == 0 && context.euCountryCheck.contains("greatBritain") && calculatorResponse.isAnyItemOverAllowance
    ) {
      declarationService.storeChargeReference(context, userInformation, cr.value) flatMap { _ =>
        Future.successful(Redirect(routes.ZeroDeclarationController.loadDeclarationPage()))
      }
    } else {
      payApiService.requestPaymentUrl(
        cr,
        userInformation,
        calculatorResponse,
        (deltaAllTax * 100).toInt,
        isAmendment = true,
        Some(amountPaidPreviously.toString),
        Some(amendState)
      ) map {
        case PayApiServiceFailureResponse      => InternalServerError(errorTemplate())
        case PayApiServiceSuccessResponse(url) => Redirect(url)
      }
    }
  }

  def irishBorder: Action[AnyContent] = publicAction { implicit context =>
    val form = context.journeyData
      .flatMap(_.irishBorder)
      .map(irishBorder => IrishBorderDto.form.bind(Map("irishBorder" -> irishBorder.toString)))
      .getOrElse(IrishBorderDto.form)

    Future.successful(Ok(irish_border(form, backLinkModel.backLink)))
  }

  def irishBorderPost: Action[AnyContent] = publicAction { implicit context =>
    IrishBorderDto.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(irish_border(formWithErrors, backLinkModel.backLink))),
        irishBorderDto =>
          travelDetailsService.storeIrishBorder(context.journeyData)(irishBorderDto.irishBorder) flatMap { _ =>
            val updatedJourneyData = context.getJourneyData.copy(irishBorder = Some(irishBorderDto.irishBorder))

            doCalculateAction(updatedJourneyData)
          }
      )
  }

  val cannotUseService: Action[AnyContent] = dashboardAction { implicit context =>
    Future.successful(Ok(purchase_price_out_of_bounds()))
  }

  def calculate: Action[AnyContent] = dashboardAction { implicit context =>
    doCalculateAction(context.getJourneyData)
  }

  private def doCalculateAction(journeyData: JourneyData)(implicit context: LocalContext): Future[Result] =
    calculatorService.calculate(journeyData) flatMap {
      case CalculatorServiceSuccessResponse(calculatorResponse) =>
        val oldCalculation: Option[Calculation] = journeyData.declarationResponse.map(_.calculation)
        val currentCalculation: Calculation     = calculatorResponse.calculation

        val redirect = Redirect(routes.CalculateDeclareController.showCalculation)

        oldCalculation match {
          case None       =>
            calculatorService.storeCalculatorResponse(journeyData, calculatorResponse) map (_ => redirect)
          case Some(calc) =>
            val deltaCalculation = calculatorService.getDeltaCalculation(calc, currentCalculation)
            calculatorService.storeCalculatorResponse(journeyData, calculatorResponse, Some(deltaCalculation)) map (_ =>
              redirect
            )
        }

      case CalculatorServicePurchasePriceOutOfBoundsFailureResponse =>
        Future.successful(Redirect(routes.CalculateDeclareController.cannotUseService))

      case _ =>
        Future.successful(InternalServerError(errorTemplate()))
    }

  def showCalculation: Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      def checkZeroPoundCondition(calculatorResponse: CalculatorResponse): Boolean =
        calculatorResponse.isAnyItemOverAllowance &&
          context.getJourneyData.euCountryCheck.contains("greatBritain") &&
          BigDecimal(calculatorResponse.calculation.allTax) == BigDecimal(0)

      def checkZeroPoundConditionForAmendment(calculatorResponse: CalculatorResponse, deltaAlltax: String): Boolean = {
        val isGreatBritain = context.getJourneyData.euCountryCheck.contains("greatBritain")
        val isDeltaZero    = BigDecimal(deltaAlltax).compare(BigDecimal(0)) == 0
        calculatorResponse.isAnyItemOverAllowance && isGreatBritain && isDeltaZero
      }

      val deltaCalc: Option[Calculation] = context.getJourneyData.deltaCalculation
      val declarationResponse            = context.getJourneyData.declarationResponse
      requireCalculatorResponse { calculatorResponse =>
        Future.successful {
          if (declarationResponse.isDefined) {
            val oldTax = declarationResponse.get.calculation.allTax
            BigDecimal(deltaCalc.get.allTax) match {
              case _ if checkZeroPoundConditionForAmendment(calculatorResponse, deltaCalc.get.allTax) =>
                Ok(
                  zero_to_declare(
                    previousDeclaration = true,
                    calculatorResponse.asDto(applySorting = false),
                    deltaCalc,
                    oldTax,
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    backLinkModel.backLink
                  )
                )

              case allTax if allTax == 0 && calculatorResponse.withinFreeAllowance =>
                Ok(
                  nothing_to_declare(
                    previousDeclaration = true,
                    calculatorResponse.asDto(applySorting = false),
                    deltaCalc,
                    oldTax,
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    underNinePounds = false,
                    backLinkModel.backLink
                  )
                )

              case allTax if allTax == 0 && !calculatorResponse.withinFreeAllowance =>
                Ok(
                  nothing_to_declare(
                    previousDeclaration = true,
                    calculatorResponse.asDto(applySorting = false),
                    deltaCalc,
                    oldTax,
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    underNinePounds = true,
                    backLinkModel.backLink
                  )
                )

              case allTax if allTax > appConfig.paymentLimit =>
                Ok(
                  over_ninety_seven_thousand_pounds(
                    previousDeclaration = true,
                    calculatorResponse.asDto(applySorting = true),
                    deltaCalc,
                    oldTax,
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    backLinkModel.backLink
                  )
                )

              case _ =>
                Ok(
                  done(
                    previousDeclaration = true,
                    calculatorResponse.asDto(applySorting = true),
                    deltaCalc,
                    oldTax,
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    backLinkModel.backLink
                  )
                )
            }
          } else {
            BigDecimal(calculatorResponse.calculation.allTax) match {
              case _ if checkZeroPoundCondition(calculatorResponse) =>
                Ok(
                  zero_to_declare(
                    previousDeclaration = false,
                    calculatorResponse.asDto(applySorting = false),
                    None,
                    "",
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    backLinkModel.backLink
                  )
                )

              case allTax if allTax == 0 && calculatorResponse.withinFreeAllowance =>
                Ok(
                  nothing_to_declare(
                    previousDeclaration = false,
                    calculatorResponse.asDto(applySorting = false),
                    None,
                    "",
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    underNinePounds = false,
                    backLinkModel.backLink
                  )
                )

              case allTax if allTax == 0 && !calculatorResponse.withinFreeAllowance =>
                Ok(
                  nothing_to_declare(
                    previousDeclaration = false,
                    calculatorResponse.asDto(applySorting = false),
                    None,
                    "",
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    underNinePounds = true,
                    backLinkModel.backLink
                  )
                )

              case allTax if allTax > appConfig.paymentLimit =>
                Ok(
                  over_ninety_seven_thousand_pounds(
                    previousDeclaration = false,
                    calculatorResponse.asDto(applySorting = true),
                    None,
                    "",
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    backLinkModel.backLink
                  )
                )

              case _ =>
                Ok(
                  done(
                    previousDeclaration = false,
                    calculatorResponse.asDto(applySorting = true),
                    None,
                    "",
                    hideExchangeRateInfo = calculatorResponse.allItemsUseGBP,
                    backLinkModel.backLink
                  )
                )
            }
          }
        }
      }
    }
  }
}
