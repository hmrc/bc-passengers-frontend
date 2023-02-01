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
import controllers.enforce.LimitExceedAction
import javax.inject.Inject
import models.ProductPath
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculatorService, ProductTreeService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class LimitExceedController @Inject() (
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val calculatorService: CalculatorService,
  limitExceedAction: LimitExceedAction,
  val errorTemplate: views.html.errorTemplate,
  val limitExceedPage: views.html.purchased_products.limit_exceed,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  val backLinkModel: BackLinkModel,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  def loadLimitExceedPage(path: ProductPath): Action[AnyContent] = limitExceedAction { implicit context =>
    requireProduct(path) { product =>
      Future.successful(
        Ok(limitExceedPage(product.name, s"heading.${product.applicableLimits.last.toLowerCase}.limit-exceeded"))
      )
    }
  }
}
