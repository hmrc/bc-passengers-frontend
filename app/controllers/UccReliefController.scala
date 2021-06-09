/*
 * Copyright 2021 HM Revenue & Customs
 *
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UccReliefController @Inject()(
                                     val cache: Cache,
                                     uccReliefAction: UccReliefAction,
                                     val error_template: views.html.error_template,
                                     val isUccReliefItemPage: views.html.travel_details.ucc_relief_item,
                                     override val controllerComponents: MessagesControllerComponents,
                                     implicit val appConfig: AppConfig,
                                     val backLinkModel: BackLinkModel,
                                     val travelDetailsService: services.TravelDetailsService,
                                     implicit val ec: ExecutionContext
                                   ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  def loadUccReliefItemPage(path: ProductPath, iid: String): Action[AnyContent] = uccReliefAction { implicit context =>
    Future.successful {
      val ppInstance =  context.journeyData.flatMap(jd => jd.purchasedProductInstances.find(p => p.iid == iid))
      ppInstance match {
        case Some(PurchasedProductInstance(_, _, _, _, _, _, _, _,_, _, _ ,_, Some(isUccRelief),_,_)) =>
          Ok(isUccReliefItemPage(UccReliefItemForm.form.fill(isUccRelief), backLinkModel.backLink, path, iid))
        case _ =>
          Ok(isUccReliefItemPage(UccReliefItemForm.form, backLinkModel.backLink, path, iid))
      }
    }
  }

  def postUccReliefItemPage(path: ProductPath, iid: String): Action[AnyContent] = uccReliefAction { implicit context =>
    UccReliefItemForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(isUccReliefItemPage(formWithErrors, backLinkModel.backLink, path, iid))
          )
      },
      success = {
        isUccRelief =>
          val ppInstances = context.getJourneyData.purchasedProductInstances.map(ppi => {
            if(ppi.iid == iid) ppi.copy(isUccRelief = Some(isUccRelief)) else ppi
          })
          cache.store(context.getJourneyData.copy(purchasedProductInstances = ppInstances)).map(_ =>
            Redirect(routes.SelectProductController.nextStep())
          )
      })
  }

}

