/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.EUEvidenceItemAction
import forms.EUEvidenceItemForm
import javax.inject.Inject
import models.{JourneyData, ProductPath, PurchasedProductInstance}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class EUEvidenceController @Inject()(
                                     val cache: Cache,
                                     eUEvidenceItemAction: EUEvidenceItemAction,
                                     val error_template: views.html.error_template,
                                     val euEvidenceItem: views.html.travel_details.eu_evidence_item,
                                     override val controllerComponents: MessagesControllerComponents,
                                     implicit val appConfig: AppConfig,
                                     val backLinkModel: BackLinkModel,
                                     val travelDetailsService: services.TravelDetailsService,
                                     implicit val ec: ExecutionContext
                                   ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  def loadEUEvidenceItemPage(path: ProductPath, iid: String): Action[AnyContent] = eUEvidenceItemAction { implicit context =>
    Future.successful {
      val ppInstance =  context.journeyData.flatMap(jd => jd.purchasedProductInstances.find(p => p.iid == iid))
      ppInstance match {
        case Some(PurchasedProductInstance(_, _, _, _, _, _, _, _,_,_, _,_,Some(hasEvidence))) =>
          Ok(euEvidenceItem(EUEvidenceItemForm.form.fill(hasEvidence), backLinkModel.backLink, path, iid))
        case _ =>
          Ok(euEvidenceItem(EUEvidenceItemForm.form, backLinkModel.backLink, path, iid))
      }
    }
  }

  def postEUEvidenceItemPage(path: ProductPath, iid: String): Action[AnyContent] = eUEvidenceItemAction { implicit context =>
    EUEvidenceItemForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(euEvidenceItem(formWithErrors, backLinkModel.backLink, path, iid))
          )
      },
      success = {
        euEvidence =>
          val ppInstances = context.getJourneyData.purchasedProductInstances.map(ppi => {
            if(ppi.iid == iid) ppi.copy(hasEvidence = Some(euEvidence), isCustomPaid = Some(euEvidence))
            else ppi
          })
          cache.store(context.getJourneyData.copy(purchasedProductInstances = ppInstances)).map(_ =>
            Redirect(routes.SelectProductController.nextStep())
          )
      })
  }


}