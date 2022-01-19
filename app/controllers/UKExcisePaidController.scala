/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.enforce.UKExcisePaidAction
import controllers.enforce.UKExcisePaidItemAction
import forms.UKExcisePaidForm
import forms.UKExcisePaidItemForm
import javax.inject.Inject
import models.{JourneyData, ProductPath, PurchasedProductInstance}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UKExcisePaidController @Inject()(
                                     val cache: Cache,
                                     uKExcisePaidAction: UKExcisePaidAction,
                                     uKExcisePaidItemAction: UKExcisePaidItemAction,
                                     val error_template: views.html.error_template,
                                     val isUKExcisePaidPage: views.html.travel_details.ukexcise_paid,
                                     val isUKExcisePaidItemPage: views.html.travel_details.ukexcise_paid_item,
                                     override val controllerComponents: MessagesControllerComponents,
                                     implicit val appConfig: AppConfig,
                                     val backLinkModel: BackLinkModel,
                                     val travelDetailsService: services.TravelDetailsService,
                                     implicit val ec: ExecutionContext
                                   ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadUKExcisePaidPage: Action[AnyContent] = uKExcisePaidAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_,_, _, _,Some(isUKVatExcisePaid),_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _,_, _, _,_,_, _, _)) =>
          Ok(isUKExcisePaidPage(UKExcisePaidForm.form.fill(isUKVatExcisePaid), backLinkModel.backLink))
        case _ =>
          Ok(isUKExcisePaidPage(UKExcisePaidForm.form, backLinkModel.backLink))
      }
    }
  }

  def postUKExcisePaidPage(): Action[AnyContent] = uKExcisePaidAction { implicit context =>
    UKExcisePaidForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(isUKExcisePaidPage(formWithErrors, backLinkModel.backLink))
          )
      },
      success = {
        isUKVatExcisePaid =>
          travelDetailsService.storeUKExcisePaid(context.journeyData)(isUKVatExcisePaid).map(_ =>
            (context.getJourneyData.isUKResident,isUKVatExcisePaid) match {
              case (Some(true),true) => Redirect(routes.TravelDetailsController.noNeedToUseServiceGbni())
              case (Some(true),false) => Redirect(routes.TravelDetailsController.goodsBoughtIntoNI())
              case _ => Redirect(routes.TravelDetailsController.whereGoodsBought())
            }
          )
      })
  }

  def loadUKExcisePaidItemPage(path: ProductPath, iid: String): Action[AnyContent] = uKExcisePaidItemAction { implicit context =>
    Future.successful {
      val ppInstance =  context.journeyData.flatMap(jd => jd.purchasedProductInstances.find(p => p.iid == iid))
      ppInstance match {
        case Some(PurchasedProductInstance(_, _, _, _, _, _, _, _, _,_,_, Some(isExcisePaid),_,_,_)) =>
          Ok(isUKExcisePaidItemPage(UKExcisePaidItemForm.form.fill(isExcisePaid), backLinkModel.backLink, path, iid))
        case _ =>
          Ok(isUKExcisePaidItemPage(UKExcisePaidItemForm.form, backLinkModel.backLink, path, iid))
      }
    }
  }

  def postUKExcisePaidItemPage(path: ProductPath, iid: String): Action[AnyContent] = uKExcisePaidItemAction { implicit context =>
    UKExcisePaidItemForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(isUKExcisePaidItemPage(formWithErrors, backLinkModel.backLink, path, iid))
          )
      },
      success = {
        isUKExcisePaid =>
          val ppInstances = context.getJourneyData.purchasedProductInstances.map(ppi => {
            if(ppi.iid == iid) ppi.copy(isExcisePaid = Some(isUKExcisePaid)) else ppi
          })
          cache.store(context.getJourneyData.copy(purchasedProductInstances = ppInstances)).map(_ =>
            Redirect(routes.SelectProductController.nextStep())
          )
      })
  }


}
