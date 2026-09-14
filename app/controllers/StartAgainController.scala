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
import forms.StartAgainForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartAgainController @Inject() (
  dashboardAction: DashboardAction,
  startAgain: views.html.purchased_products.start_again,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[?] = localContext.request

  private def calculationPage: Call = routes.CalculateDeclareController.showCalculation

  val show: Action[AnyContent] = dashboardAction { implicit context =>
    Future.successful(Ok(startAgain(StartAgainForm.form, Some(calculationPage.url))))
  }

  val submit: Action[AnyContent] = dashboardAction { implicit context =>
    StartAgainForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(startAgain(formWithErrors, Some(calculationPage.url)))),
        startAgainSelection =>
          Future.successful {
            if (startAgainSelection) {
              Redirect(routes.TravelDetailsController.newSession)
            } else {
              Redirect(calculationPage)
            }
          }
      )
  }
}
