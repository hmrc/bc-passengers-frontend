/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.enforce.{DashboardAction, PublicAction}
import javax.inject.Inject
import models.{ConfirmRemoveDto, ProductPath}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerComponents, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class AlterProductsController @Inject() (
  val cache: Cache,
  val calculatorService: CalculatorService,
  val purhasedProductService: PurchasedProductService,
  val currencyService: CurrencyService,
  val countriesService: CountriesService,
  val productTreeService: ProductTreeService,

  publicAction: PublicAction,
  dashboardAction: DashboardAction,

  val remove: views.html.purchased_products.remove,
  val error_template: views.html.error_template,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def confirmRemove(path: ProductPath, iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    Future.successful(Ok(remove(ConfirmRemoveDto.form, path, iid)))
  }

  def remove(path: ProductPath, iid: String): Action[AnyContent] = dashboardAction { implicit context =>

    ConfirmRemoveDto.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(remove(formWithErrors, path, iid)))
      },
      confirmRemoveDto => {
        if (confirmRemoveDto.confirmRemove) {
          purhasedProductService.removePurchasedProductInstance(context.getJourneyData, path, iid) map { _ =>
            Redirect(routes.DashboardController.showDashboard())
          }
        }
        else {
          Future.successful(Redirect(routes.DashboardController.showDashboard()))
        }
      }
    )
  }
}
