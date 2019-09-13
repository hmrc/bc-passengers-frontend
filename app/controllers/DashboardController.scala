package controllers

import config.AppConfig
import connectors.Cache
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
  val cache: Cache,
  val purchasedProductService: PurchasedProductService,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,
  val backLinkModel: BackLinkModel,

  val dashboard: views.html.purchased_products.dashboard,
  val nothing_to_declare: views.html.purchased_products.nothing_to_declare,
  val done: views.html.purchased_products.done,
  val over_ninty_seven_thousand_pounds: views.html.purchased_products.over_ninty_seven_thousand_pounds,
  val error_template: views.html.error_template,
  val purchase_price_out_of_bounds: views.html.errors.purchase_price_out_of_bounds,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def showDashboard: Action[AnyContent] = DashboardAction { implicit context =>

    cache.fetch flatMap { journeyData: Option[JourneyData] =>

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

        Ok(dashboard(jd, alcoholPurchasedItemList.reverse, tobaccoPurchasedItemList.reverse, otherGoodsPurchasedItemList.reverse, showCalculate, backLinkModel.backLink))

      }
    }
  }


  def calculate: Action[AnyContent] = DashboardAction { implicit context =>
    calculatorService.calculate() flatMap {

      case CalculatorServiceSuccessResponse(calculatorResponse) =>

        calculatorService.storeCalculatorResponse(context.getJourneyData, calculatorResponse) map { _ =>
          Redirect(routes.DashboardController.showCalculation())
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

  def showCalculation: Action[AnyContent] = DashboardAction { implicit context =>
    requireCalculatorResponse { calculatorResponse =>

      Future.successful {
        BigDecimal(calculatorResponse.calculation.allTax) match {
          case allTax if allTax == 0 && calculatorResponse.withinFreeAllowance =>
            Ok (nothing_to_declare (calculatorResponse.asDto(applySorting = false), calculatorResponse.allItemsUseGBP, false))

          case allTax if allTax > 0 && allTax < 9 || allTax == 0 && !calculatorResponse.withinFreeAllowance =>
            Ok (nothing_to_declare (calculatorResponse.asDto(applySorting = false), calculatorResponse.allItemsUseGBP, true))

          case allTax if allTax > 97000  =>
            Ok (over_ninty_seven_thousand_pounds (calculatorResponse.asDto(applySorting = true), calculatorResponse.allItemsUseGBP))

          case _ => Ok (done (calculatorResponse.asDto(applySorting = true), calculatorResponse.allItemsUseGBP) )
        }
      }
    }
  }

}
