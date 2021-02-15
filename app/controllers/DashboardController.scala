/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.DashboardAction
import javax.inject.{Inject, Singleton}
import models.{ProductTreeLeaf, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext



@Singleton
class DashboardController @Inject() (
  val countriesService: CountriesService,
  val cache: Cache,
  val purchasedProductService: PurchasedProductService,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,
  val backLinkModel: BackLinkModel,

  dashboardAction: DashboardAction,
  val dashboard: views.html.purchased_products.dashboard,

  val error_template: views.html.error_template,
  val purchase_price_out_of_bounds: views.html.errors.purchase_price_out_of_bounds,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def showDashboard: Action[AnyContent] = dashboardAction { implicit context =>
    revertWorkingInstance {
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

          Ok(dashboard(jd, alcoholPurchasedItemList.reverse, tobaccoPurchasedItemList.reverse, otherGoodsPurchasedItemList.reverse, showCalculate,
            backLinkModel.backLink,
            appConfig.isIrishBorderQuestionEnabled,
            jd.euCountryCheck.contains("greatBritain") && jd.arrivingNICheck.contains(true),
            jd.euCountryCheck.contains("euOnly"),
            jd.isUKResident.contains(true)))
        }
      }
    }
  }

}
