/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.enforce.DashboardAction
import models.{ProductTreeLeaf, *}
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
  val errorTemplate: views.html.errorTemplate,
  val purchase_price_out_of_bounds: views.html.errors.purchase_price_out_of_bounds,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with ControllerHelpers {

  private val itemsPerPage = 10

  private def requestedPage(implicit context: LocalContext): Int =
    context.request.getQueryString("page").flatMap(value => Try(value.toInt).toOption).filter(_ > 0).getOrElse(1)

  def showDashboard: Action[AnyContent] = dashboardAction { implicit context =>
    given lang: Lang = context.request.lang
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      revertWorkingInstance {
        cache.fetch.flatMap { (journeyData: Option[JourneyData]) =>
          val isAmendment                  = context.getJourneyData.declarationResponse.isDefined
          val jd                           = journeyData.getOrElse(JourneyData())
          val allPurchasedProductInstances =
            jd.declarationResponse.map(_.oldPurchaseProductInstances).getOrElse(Nil) ++ jd.purchasedProductInstances
          calculatorService.journeyDataToCalculatorRequest(jd, allPurchasedProductInstances) map {
            maybeCalculatorRequest =>
              val purchasedItemList = maybeCalculatorRequest.map(_.items).getOrElse(Nil)

              val alcoholPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
                case item @ PurchasedItem(ppi, ProductTreeLeaf(_, _, _, tid, _), _, _, _)
                    if tid == "alcohol" && ppi.isEditable.contains(true) =>
                  item
              }

              val tobaccoPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
                case item @ PurchasedItem(ppi, ProductTreeLeaf(_, _, _, tid, _), _, _, _)
                    if (tid == "cigarettes" | tid == "cigars" | tid == "tobacco") && ppi.isEditable.contains(true) =>
                  item
              }

              val otherGoodsPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
                case item @ PurchasedItem(ppi, ProductTreeLeaf(_, _, _, tid, _), _, _, _)
                    if tid == "other-goods" && ppi.isEditable.contains(true) =>
                  item
              }

              val previousOtherGoodsPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
                case item @ PurchasedItem(ppi, ProductTreeLeaf(_, _, _, tid, _), _, _, _)
                    if tid == "other-goods" && ppi.isEditable.contains(false) =>
                  item
              }

              val alcoholItems    = alcoholPurchasedItemList.reverse
              val tobaccoItems    = tobaccoPurchasedItemList.reverse
              val otherGoodsItems = otherGoodsPurchasedItemList.reverse
              val allItems        =
                alcoholItems.map("alcohol" -> _) ++ tobaccoItems.map("tobacco" -> _) ++ otherGoodsItems.map(
                  "other-goods" -> _
                )
              val totalItems      = allItems.size
              val totalPages      = math.max(1, math.ceil(totalItems.toDouble / itemsPerPage).toInt)
              val currentPage     = math.min(requestedPage, totalPages)
              val pageItems       = allItems.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage)

              val pageAlcoholItems    = pageItems.collect { case ("alcohol", item) => item }
              val pageTobaccoItems    = pageItems.collect { case ("tobacco", item) => item }
              val pageOtherGoodsItems = pageItems.collect { case ("other-goods", item) => item }

              val showCalculate = totalItems > 0

              Ok(
                dashboard(
                  jd,
                  pageAlcoholItems,
                  pageTobaccoItems,
                  pageOtherGoodsItems,
                  previousOtherGoodsPurchasedItemList.reverse,
                  totalItems,
                  otherGoodsItems.size,
                  currentPage,
                  totalPages,
                  showCalculate,
                  isAmendment,
                  backLinkModel.backLink,
                  appConfig.isIrishBorderQuestionEnabled,
                  jd.euCountryCheck.contains("greatBritain") && jd.arrivingNICheck.contains(true),
                  jd.euCountryCheck.contains("euOnly"),
                  jd.isUKResident.contains(true)
                )
              )
          }
        }
      }
    }
  }
}
