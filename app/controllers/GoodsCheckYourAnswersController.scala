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
import controllers.enforce.DashboardAction
import models.{ProductPath, ProductTreeLeaf}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.{CurrencyService, ProductTreeService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsCheckYourAnswersController @Inject() (
  dashboardAction: DashboardAction,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService,
  val check_your_goods_answers: views.html.purchased_products.check_your_goods_answers,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport {

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

  def submit: Action[AnyContent] = dashboardAction { _ =>
    Future.successful(Redirect(routes.SelectProductController.nextStep()))
  }
}
