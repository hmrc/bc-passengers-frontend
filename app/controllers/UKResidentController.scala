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
import controllers.enforce.UKResidentAction
import forms.UKResidentForm
import javax.inject.Inject
import models.JourneyData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UKResidentController @Inject()(
                                      val cache: Cache,
                                      uKResidentAction: UKResidentAction,
                                      val error_template: views.html.error_template,
                                      val isUKResidentPage: views.html.travel_details.uk_resident,
                                      override val controllerComponents: MessagesControllerComponents,
                                      implicit val appConfig: AppConfig,
                                      val backLinkModel: BackLinkModel,
                                      val travelDetailsService: services.TravelDetailsService,
                                      implicit val ec: ExecutionContext
                                    ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadUKResidentPage: Action[AnyContent] = uKResidentAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, _, _, _, _,Some(isUKResident),_,_, _, _, _, _, _, _, _, _, _, _, _, _, _,_, _, _,_,_, _, _)) =>
          Ok(isUKResidentPage(UKResidentForm.form.fill(isUKResident), backLinkModel.backLink))
        case _ =>
          Ok(isUKResidentPage(UKResidentForm.form, backLinkModel.backLink))
      }
    }
  }

  def postUKResidentPage(): Action[AnyContent] = uKResidentAction { implicit context =>
    UKResidentForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(isUKResidentPage(formWithErrors, backLinkModel.backLink))
          )
      },
      success = {
        isUKResident =>
          travelDetailsService.storeUKResident(context.journeyData)(isUKResident).map(f = _ =>
            isUKResident match {
              case true => Redirect(routes.UKExcisePaidController.loadUKExcisePaidPage())
              case false => Redirect(routes.TravelDetailsController.goodsBoughtIntoNI())
            }
          )
      })
  }

}
