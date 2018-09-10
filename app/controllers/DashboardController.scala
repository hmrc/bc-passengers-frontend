package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.{ProductTreeLeaf, _}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import Console._

import scala.collection.immutable
import scala.concurrent.Future



@Singleton
class DashboardController @Inject() (
  val countriesService: CountriesService,
  val travelDetailsService: TravelDetailsService,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers  {

  val showDashboard: Action[AnyContent] = DashboardAction { implicit request =>

    travelDetailsService.getJourneyData flatMap { journeyData: Option[JourneyData] =>

      val jd = journeyData.getOrElse(JourneyData())
      calculatorService.journeyDataToCalculatorRequest(jd) map { maybeCalculatorRequest =>

        val purchasedItemList = maybeCalculatorRequest.map(_.items).getOrElse(Nil)

        val alcoholPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
          case item@PurchasedItem(_, ProductTreeLeaf(_, _, _, tid), _, _) if tid == "alcohol" => item
        }

        val tobaccoPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
          case item@PurchasedItem(_, ProductTreeLeaf(_, _, _, tid), _, _) if tid == "cigarettes" | tid == "cigars" | tid == "tobacco" => item
        }

        val otherGoodsPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
          case item@PurchasedItem(_, ProductTreeLeaf(_, _, _, tid), _, _) if tid == "other-goods" => item
        }

        val showCalculate = !(alcoholPurchasedItemList.isEmpty && tobaccoPurchasedItemList.isEmpty && otherGoodsPurchasedItemList.isEmpty)

        Ok(views.html.purchased_products.dashboard(jd, alcoholPurchasedItemList.reverse, tobaccoPurchasedItemList.reverse, otherGoodsPurchasedItemList.reverse, showCalculate))
      }
    }
  }


  val calculate: Action[AnyContent] = DashboardAction { implicit request =>
        calculatorService.calculate() map {

          case CalculatorServiceSuccessResponse(calculatorResponseDto) =>

            if(BigDecimal(calculatorResponseDto.calculation.allTax)==0)
              Ok(views.html.purchased_products.nothing_to_declare(calculatorResponseDto))
            else
              Ok(views.html.purchased_products.done(calculatorResponseDto, !calculatorResponseDto.hasOnlyGBP))

          case _ =>
            InternalServerError(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem."))
        }
      }
}
