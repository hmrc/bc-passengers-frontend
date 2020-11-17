/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculatorService, CountriesService, CurrencyService, ProductTreeService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UnauthorisedController @Inject()(
                                        val unauthorised_error_page: views.html.errors.unauthorised_error_page,
                                        override val controllerComponents: MessagesControllerComponents,
                                        implicit val ec: ExecutionContext
                                      ) extends FrontendController(controllerComponents) with I18nSupport {

  def show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(unauthorised_error_page())
    )
  }
}
