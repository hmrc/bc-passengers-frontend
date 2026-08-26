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
import models.{GoodsTypeDto, ProductPath}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddItemController @Inject() (
  dashboardAction: DashboardAction,
  val add_item: views.html.purchased_products.add_item,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport {

  def show: Action[AnyContent] = dashboardAction { implicit context =>
    implicit val request: Request[AnyContent] = context.request
    Future.successful(Ok(add_item(GoodsTypeDto.form)))
  }

  def submit: Action[AnyContent] = dashboardAction { implicit context =>
    implicit val request: Request[AnyContent] = context.request
    GoodsTypeDto.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(add_item(formWithErrors))),
        goodsType =>
          Future.successful(
            Redirect(routes.SelectProductController.clearAndAskProductSelection(ProductPath(goodsType.goodsType)))
          )
      )
  }
}
