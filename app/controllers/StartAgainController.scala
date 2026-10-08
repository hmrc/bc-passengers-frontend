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
import controllers.enforce.{DashboardAction, PublicAction}
import forms.StartAgainForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartAgainController @Inject() (
  cache: Cache,
  dashboardAction: DashboardAction,
  publicAction: PublicAction,
  startAgain: views.html.purchased_products.start_again,
  declarationDeletedPage: views.html.purchased_products.declaration_deleted,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with FrontendHeaderCarrierProvider {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[?] = localContext.request

  private def calculationPage: Call = routes.CalculateDeclareController.showCalculation

  private def startPageAfterDeletion: Call =
    Call("GET", s"${routes.PreviousDeclarationController.loadPreviousDeclarationPage.url}?startAgain=true")

  val show: Action[AnyContent] = dashboardAction { implicit context =>
    Future.successful(Ok(startAgain(StartAgainForm.form, Some(calculationPage.url))))
  }

  val submit: Action[AnyContent] = dashboardAction { implicit context =>
    StartAgainForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(startAgain(formWithErrors, Some(calculationPage.url)))),
        startAgainSelection =>
          if (startAgainSelection) {
            cache.removeFrontendCache.map { _ =>
              Redirect(startPageAfterDeletion)
                .addingToSession(SessionKeys.sessionId -> UUID.randomUUID.toString)
            }
          } else {
            Future.successful(Redirect(calculationPage))
          }
      )
  }

  val declarationDeleted: Action[AnyContent] = publicAction { implicit context =>
    Future.successful(Ok(declarationDeletedPage()))
  }
}
