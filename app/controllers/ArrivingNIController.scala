/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.ArrivingNIAction
import forms.ArrivingNIForm
import javax.inject.Inject
import models.JourneyData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController


import scala.concurrent.{ExecutionContext, Future}

class ArrivingNIController @Inject()(
                                      val cache: Cache,
                                      arrivingNIAction: ArrivingNIAction,
                                      val error_template: views.html.error_template,
                                      val arrivingNIPage: views.html.travel_details.arriving_ni,
                                      override val controllerComponents: MessagesControllerComponents,
                                      implicit val appConfig: AppConfig,
                                      val backLinkModel: BackLinkModel,
                                      val travelDetailsService: services.TravelDetailsService,
                                      implicit val ec: ExecutionContext
                                    ) extends FrontendController(controllerComponents) with I18nSupport {

  implicit def convertContextToRequest(implicit localContext: LocalContext): Request[_] = localContext.request

  val loadArrivingNIPage: Action[AnyContent] = arrivingNIAction { implicit context =>
    Future.successful {
      context.journeyData match {
        case Some(JourneyData(_, Some(arrivingNI), _, _,_, _, _, _, _, _, _, _, _, _, _, _, _)) =>

          Ok(arrivingNIPage(ArrivingNIForm.form.fill(arrivingNI), backLinkModel.backLink))
        case _ =>
          Ok(arrivingNIPage(ArrivingNIForm.form, backLinkModel.backLink))
      }
    }
  }

  def postArrivingNIPage(): Action[AnyContent] = arrivingNIAction { implicit context =>
    ArrivingNIForm.form.bindFromRequest().fold(
      hasErrors = {
        formWithErrors =>
          Future.successful(
            BadRequest(arrivingNIPage(formWithErrors, backLinkModel.backLink))
          )
      },
      success = {
        arrivingNI =>
          travelDetailsService.storeArrivingNI(context.journeyData)(arrivingNI).map(_ =>
            context.getJourneyData.euCountryCheck match {
              case Some(euCountryCheck) =>
                  euCountryCheck match {
                    case "euOnly" =>
                      if (arrivingNI) {
                        Redirect(routes.TravelDetailsController.goodsBoughtInsideEu())
                      } else {
                        Redirect(routes.TravelDetailsController.goodsBoughtOutsideEu())
                      }
                    case "nonEuOnly" => Redirect(routes.TravelDetailsController.goodsBoughtOutsideEu())
                    case "greatBritain" => Redirect(routes.UKVatPaidController.loadUKVatPaidPage())
                  }
            }
          )
      })
  }

}
