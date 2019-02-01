package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.{ProductTreeLeaf, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}



@Singleton
class DashboardController @Inject() (
  val countriesService: CountriesService,
  val travelDetailsService: TravelDetailsService,
  val purhasedProductService: PurchasedProductService,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,
  val dashboard: views.html.purchased_products.dashboard,
  val nothing_to_declare: views.html.purchased_products.nothing_to_declare,
  val done: views.html.purchased_products.done,
  val under_nine_pounds: views.html.purchased_products.under_nine_pounds,
  val over_ninty_seven_thousand_pounds: views.html.purchased_products.over_ninty_seven_thousand_pounds,
  val error_template: views.html.error_template,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def showDashboard: Action[AnyContent] = DashboardAction { implicit context =>

    travelDetailsService.getJourneyData flatMap { journeyData: Option[JourneyData] =>

      val jd = journeyData.getOrElse(JourneyData())
      calculatorService.journeyDataToCalculatorRequest(jd) map { maybeCalculatorRequest =>

        val purchasedItemList = maybeCalculatorRequest.map(_.items).getOrElse(Nil)

        val alcoholPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
          case item@PurchasedItem(_, ProductTreeLeaf(_, _, _, tid, _), _, _, _) if tid == "alcohol" => item
        }

        val tobaccoPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
          case item@PurchasedItem(_, ProductTreeLeaf(_, _, _, tid, _), _, _, _) if tid == "cigarettes" | tid == "cigars" | tid == "tobacco" => item
        }

        val otherGoodsPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
          case item@PurchasedItem(_, ProductTreeLeaf(_, _, _, tid, _), _, _, _) if tid == "other-goods" => item
        }

        val showCalculate = !(alcoholPurchasedItemList.isEmpty && tobaccoPurchasedItemList.isEmpty && otherGoodsPurchasedItemList.isEmpty)

        Ok(dashboard(jd, alcoholPurchasedItemList.reverse, tobaccoPurchasedItemList.reverse, otherGoodsPurchasedItemList.reverse, showCalculate))

      }
    }
  }


  def calculate: Action[AnyContent] = DashboardAction { implicit context =>
    calculatorService.calculate() flatMap {

      case CalculatorServiceSuccessResponse(calculatorResponse) =>

        calculatorService.storeCalculatorResponse(context.getJourneyData, calculatorResponse) map { _ =>
          Redirect(routes.DashboardController.showCalculation())
        }


      case _ =>
        Future.successful {
          InternalServerError(error_template("Technical problem", "Technical problem", "There has been a technical problem."))
        }
    }
  }

  def showCalculation: Action[AnyContent] = DashboardAction { implicit context =>
    requireCalculatorResponse { calculatorResponse =>

      Future.successful {
        BigDecimal(calculatorResponse.calculation.allTax) match {
          case allTax if allTax == 0 && calculatorResponse.withinFreeAllowance =>
            Ok (nothing_to_declare (calculatorResponse.asDto(applySorting = false), calculatorResponse.allItemsUseGBP))

          case allTax if allTax > 0 && allTax < 9 || allTax == 0 && !calculatorResponse.withinFreeAllowance =>
            Ok (under_nine_pounds (calculatorResponse.asDto(applySorting = false), calculatorResponse.allItemsUseGBP))

          case allTax if allTax > 97000  =>
            Ok (over_ninty_seven_thousand_pounds (calculatorResponse.asDto(applySorting = true), calculatorResponse.allItemsUseGBP))

          case _ => Ok (done (calculatorResponse.asDto(applySorting = true), calculatorResponse.allItemsUseGBP) )
        }
      }
    }
  }

}
