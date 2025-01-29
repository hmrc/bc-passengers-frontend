/*
 * Copyright 2025 HM Revenue & Customs
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

/*
 * Copyright 2020 HM Revenue & Customs
 *
 */
package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.UccReliefAction
import forms.UccReliefItemForm
import javax.inject.Inject
import models.{ProductPath, PurchasedProductInstance}
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UccReliefController @Inject() (
  val cache: Cache,
  uccReliefAction: UccReliefAction,
  val error_template: views.html.errorTemplate,
  val isUccReliefItemPage: views.html.travel_details.ucc_relief_item,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  val backLinkModel: BackLinkModel,
  val travelDetailsService: services.TravelDetailsService,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[?] = localContext.request

  def loadUccReliefItemPage(path: ProductPath, iid: String): Action[AnyContent] = uccReliefAction { implicit context =>
    Future.successful {
      val ppInstance = context.journeyData.flatMap(jd => jd.purchasedProductInstances.find(p => p.iid == iid))
      ppInstance match {
        case Some(PurchasedProductInstance(_, _, _, _, _, _, _, _, _, _, _, _, Some(isUccRelief), _, _)) =>
          Ok(isUccReliefItemPage(UccReliefItemForm.form.fill(isUccRelief), backLinkModel.backLink, path, iid))
        case _                                                                                           =>
          Ok(isUccReliefItemPage(UccReliefItemForm.form, backLinkModel.backLink, path, iid))
      }
    }
  }

  def postUccReliefItemPage(path: ProductPath, iid: String): Action[AnyContent] = uccReliefAction { implicit context =>
    UccReliefItemForm.form
      .bindFromRequest()
      .fold(
        hasErrors = { formWithErrors =>
          Future.successful(
            BadRequest(isUccReliefItemPage(formWithErrors, backLinkModel.backLink, path, iid))
          )
        },
        success = { isUccRelief =>
          val ppInstances = context.getJourneyData.purchasedProductInstances.map { ppi =>
            if (ppi.iid == iid) ppi.copy(isUccRelief = Some(isUccRelief)) else ppi
          }
          cache
            .store(context.getJourneyData.copy(purchasedProductInstances = ppInstances))
            .map(_ => Redirect(routes.SelectProductController.nextStep()))
        }
      )
  }

}
