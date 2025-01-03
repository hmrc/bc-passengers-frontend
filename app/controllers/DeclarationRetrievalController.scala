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
import controllers.enforce.{DeclarationNotFoundAction, DeclarationRetrievalAction}
import javax.inject.Inject
import models.{DeclarationRetrievalDto, PreviousDeclarationRequest}
import java.time.{LocalDateTime, ZoneOffset}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class DeclarationRetrievalController @Inject() (
  val cache: Cache,
  declarationRetrievalAction: DeclarationRetrievalAction,
  declarationNotFoundAction: DeclarationNotFoundAction,
  val error_template: views.html.errorTemplate,
  val declarationRetrievalPage: views.html.amendments.declaration_retrieval,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  val backLinkModel: BackLinkModel,
  val travelDetailsService: services.TravelDetailsService,
  val previousDeclarationService: services.PreviousDeclarationService,
  val declaration_Not_Found: views.html.amendments.declaration_not_found,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[?] = localContext.request

  val loadDeclarationRetrievalPage: Action[AnyContent] = declarationRetrievalAction { implicit context =>
    Future.successful {
      context.getJourneyData.previousDeclarationRequest match {
        case Some(previousDec) =>
          Ok(
            declarationRetrievalPage(
              DeclarationRetrievalDto.form().fill(DeclarationRetrievalDto.fromPreviousDeclarationDetails(previousDec)),
              backLinkModel.backLink
            )
          )
        case _                 =>
          Ok(declarationRetrievalPage(DeclarationRetrievalDto.form(), backLinkModel.backLink))
      }
    }
  }

  def postDeclarationRetrievalPage(): Action[AnyContent] = declarationRetrievalAction { implicit context =>
    DeclarationRetrievalDto
      .form()
      .bindFromRequest()
      .fold(
        hasErrors = { formWithErrors =>
          Future.successful(
            BadRequest(declarationRetrievalPage(formWithErrors, backLinkModel.backLink))
          )
        },
        success = { previousDeclarationDetails =>
          previousDeclarationService
            .storePrevDeclarationDetails(context.journeyData)(
              PreviousDeclarationRequest.build(previousDeclarationDetails)
            )
            .map(journeyData =>
              if (journeyData.get.declarationResponse.isDefined) {
                val date       = journeyData.get.userInformation.get.dateOfArrival
                val time       = journeyData.get.userInformation.get.timeOfArrival
                val dateTime   = LocalDateTime.of(
                  date.getYear,
                  date.getMonth,
                  date.getDayOfMonth,
                  time.getHour,
                  time.getMinute,
                  time.getSecond
                )
                val amendState = journeyData.get.amendState.getOrElse("")

                if (
                  dateTime
                    .atZone(ZoneOffset.UTC)
                    .plusDays(1L)
                    .isBefore(LocalDateTime.now().atZone(ZoneOffset.UTC))
                ) {
                  Redirect(routes.DeclarationRetrievalController.declarationNotFound)
                } else if (amendState.equals("pending-payment")) {
                  Redirect(routes.PendingPaymentController.loadPendingPaymentPage)
                } else {
                  Redirect(routes.PreviousGoodsController.showPreviousGoods)
                }
              } else {
                Redirect(routes.DeclarationRetrievalController.declarationNotFound)
              }
            )
        }
      )

  }

  val declarationNotFound: Action[AnyContent] = declarationNotFoundAction { implicit context =>
    Future.successful(Ok(declaration_Not_Found(backLinkModel.backLink)))
  }

}
