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
import models.{ProductPath, ProductTreeLeaf}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.{AlcoholAndTobaccoCalculationService, CurrencyService, ProductTreeService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.{FrontendController, FrontendHeaderCarrierProvider}
import util.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsCheckYourAnswersController @Inject() (
  dashboardAction: DashboardAction,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService,
  alcoholAndTobaccoCalculationService: AlcoholAndTobaccoCalculationService,
  cache: Cache,
  val check_your_goods_answers: views.html.purchased_products.check_your_goods_answers,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with FrontendHeaderCarrierProvider {

  def show(path: ProductPath, iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    implicit val request: Request[AnyContent] = context.request
    val item                                  = context.getJourneyData.getPurchasedProductInstance(iid).filter(_.path == path)
    val product                               = productTreeService.productTree.getDescendant(path).collect { case leaf: ProductTreeLeaf => leaf }

    (item, product) match {
      case (Some(purchasedItem), Some(productTreeLeaf)) =>
        val currency = purchasedItem.currency.flatMap(currencyService.getCurrencyByCode)
        Future.successful(Ok(check_your_goods_answers(purchasedItem, productTreeLeaf, currency)))
      case _                                            =>
        Future.successful(Redirect(routes.DashboardController.showDashboard))
    }
  }

  def submit(path: ProductPath, iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    implicit val request: Request[AnyContent] = context.request
    if (!appConfig.isWineStillOrSparklingEnabled) {
      Future.successful(Redirect(routes.SelectProductController.nextStep()))
    } else {
      val journeyData = context.getJourneyData
      val item        = journeyData.getPurchasedProductInstance(iid).filter(_.path == path)
      val product     = productTreeService.productTree.getDescendant(path).collect { case leaf: ProductTreeLeaf => leaf }

      (item, product) match {
        case (Some(purchasedItem), Some(productTreeLeaf)) if productTreeLeaf.templateId == "alcohol" =>
          val totalVolumeForAlcohol =
            alcoholAndTobaccoCalculationService.alcoholAddHelper(journeyData, BigDecimal(0), productTreeLeaf.token)
          if (alcoholVolumeConstraint(journeyData, totalVolumeForAlcohol, productTreeLeaf.token))
            Future.successful(Redirect(routes.SelectProductController.nextStep()))
          else {
            implicit val headerCarrier: HeaderCarrier = hc(context.request)
            cache.store(journeyData.removePurchasedProductInstance(iid)).map { _ =>
              Redirect(routes.LimitExceedController.onPageLoadAddJourneyAlcoholVolume(path))
                .removingFromSession(s"user-amount-input-${productTreeLeaf.token}")
                .addingToSession(
                  s"user-amount-input-${productTreeLeaf.token}" ->
                    purchasedItem.weightOrVolume.getOrElse(BigDecimal(0)).toString
                )
            }
          }
        case _                                                                                       =>
          Future.successful(Redirect(routes.SelectProductController.nextStep()))
      }
    }
  }
}
