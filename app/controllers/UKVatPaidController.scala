/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.UKVatPaidAction
import forms.UKVatPaidForm

import javax.inject.Inject
import models.{ProductPath, PurchasedProductInstance}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UKVatPaidController @Inject()(
                                      val cache: Cache,
                                      uKVatPaidAction: UKVatPaidAction,
                                      val error_template: views.html.error_template,
                                      val isUKVatPaidItemPage: views.html.travel_details.ukvat_paid_item,
                                      override val controllerComponents: MessagesControllerComponents,
                                      implicit val appConfig: AppConfig,
                                      val backLinkModel: BackLinkModel,
                                      val travelDetailsService: services.TravelDetailsService,
                                      implicit val ec: ExecutionContext
                                    ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  def loadItemUKVatPaidPage(path: ProductPath, iid: String): Action[AnyContent] = uKVatPaidAction { implicit context =>
    Future.successful {
      val ppInstance =  context.journeyData.flatMap(jd => jd.purchasedProductInstances.find(p => p.iid == iid))
      ppInstance match {
        case Some(PurchasedProductInstance(_, _,_,_,_,_,_,_,Some(isVatPaid),_,_, _,_,_)) =>
          Ok(isUKVatPaidItemPage(UKVatPaidForm.form.fill(isVatPaid), backLinkModel.backLink,path,iid))
        case _ =>
          Ok(isUKVatPaidItemPage(UKVatPaidForm.form, backLinkModel.backLink,path,iid))
      }
    }
  }

  def postItemUKVatPaidPage(path: ProductPath, iid: String): Action[AnyContent] = uKVatPaidAction { implicit context =>
    UKVatPaidForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(isUKVatPaidItemPage(formWithErrors, backLinkModel.backLink, path, iid))
          )
      },
      success = {
        isVatPaid =>
          val ppInstances = context.getJourneyData.purchasedProductInstances.map(ppi => {
            if (ppi.iid == iid) ppi.copy(isVatPaid = Some(isVatPaid)) else ppi
          })
          cache.store(context.getJourneyData.copy(purchasedProductInstances = ppInstances)).map(_ =>
            path.toString.contains("other-goods") match {
              case true =>
                if (context.getJourneyData.isUKResident.isDefined && !context.getJourneyData.isUKResident.get)
                  Redirect (routes.UccReliefController.loadUccReliefItemPage(path, iid))
                else
                  Redirect(routes.SelectProductController.nextStep())
              case false =>
                Redirect(routes.UKExcisePaidController.loadUKExcisePaidItemPage(path, iid))
            }
          )
      })
  }
}

