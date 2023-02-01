/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{ProductTreeLeaf, _}
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
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
  dashboardAction: DashboardAction,
  val dashboard: views.html.purchased_products.dashboard,
  val errorTemplate: views.html.errorTemplate,
  val purchase_price_out_of_bounds: views.html.errors.purchase_price_out_of_bounds,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with ControllerHelpers {

  def showDashboard: Action[AnyContent] = dashboardAction { implicit context =>
    implicit val lang: Lang = context.request.lang
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      revertWorkingInstance {
        cache.fetch flatMap { journeyData: Option[JourneyData] =>
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

              val previousAlcoholPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
                case item @ PurchasedItem(ppi, ProductTreeLeaf(_, _, _, tid, _), _, _, _)
                    if tid == "alcohol" && ppi.isEditable.contains(false) =>
                  item
              }

              val tobaccoPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
                case item @ PurchasedItem(ppi, ProductTreeLeaf(_, _, _, tid, _), _, _, _)
                    if (tid == "cigarettes" | tid == "cigars" | tid == "tobacco") && ppi.isEditable.contains(true) =>
                  item
              }

              val previousTobaccoPurchasedItemList: List[PurchasedItem] = purchasedItemList.collect {
                case item @ PurchasedItem(ppi, ProductTreeLeaf(_, _, _, tid, _), _, _, _)
                    if (tid == "cigarettes" | tid == "cigars" | tid == "tobacco") && ppi.isEditable.contains(false) =>
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

              val showCalculate =
                !(alcoholPurchasedItemList.isEmpty && tobaccoPurchasedItemList.isEmpty && otherGoodsPurchasedItemList.isEmpty)

              Ok(
                dashboard(
                  jd,
                  alcoholPurchasedItemList.reverse,
                  tobaccoPurchasedItemList.reverse,
                  otherGoodsPurchasedItemList.reverse,
                  previousAlcoholPurchasedItemList.reverse,
                  previousTobaccoPurchasedItemList.reverse,
                  previousOtherGoodsPurchasedItemList.reverse,
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
