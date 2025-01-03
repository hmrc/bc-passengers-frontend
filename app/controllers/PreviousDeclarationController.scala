/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.enforce.PreviousDeclarationAction
import forms.PrevDeclarationForm
import javax.inject.Inject
import models.JourneyData
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class PreviousDeclarationController @Inject() (
  val cache: Cache,
  previousDeclarationAction: PreviousDeclarationAction,
  val error_template: views.html.errorTemplate,
  val previousDeclarationPage: views.html.amendments.previous_declaration,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  val backLinkModel: BackLinkModel,
  val previousDeclarationService: services.PreviousDeclarationService,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[?] = localContext.request

  val loadPreviousDeclarationPage: Action[AnyContent] = previousDeclarationAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(
              JourneyData(
                Some(prevDeclaration),
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _
              )
            ) =>
          Ok(previousDeclarationPage(PrevDeclarationForm.validateForm().fill(prevDeclaration), backLinkModel.backLink))
        case _ =>
          Ok(previousDeclarationPage(PrevDeclarationForm.validateForm(), backLinkModel.backLink))
      }
    }
  }

  def postPreviousDeclarationPage(): Action[AnyContent] = previousDeclarationAction { implicit context =>
    PrevDeclarationForm
      .validateForm()
      .bindFromRequest()
      .fold(
        hasErrors = { formWithErrors =>
          Future.successful(
            BadRequest(previousDeclarationPage(formWithErrors, backLinkModel.backLink))
          )
        },
        success = { prevDeclaration =>
          previousDeclarationService
            .storePrevDeclaration(context.journeyData)(prevDeclaration)
            .map(_ =>
              if (prevDeclaration) {
                Redirect(routes.DeclarationRetrievalController.loadDeclarationRetrievalPage)
              } else {
                Redirect(routes.TravelDetailsController.whereGoodsBought)
              }
            )
        }
      )
  }
}
