/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import java.util.UUID

import config.AppConfig
import connectors.Cache
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class PublicController @Inject() (
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val calculatorService: CalculatorService,

  val error_template: views.html.error_template,
  val time_out: views.html.time_out,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def timeOut: Action[AnyContent] = Action.async { implicit context =>
    Future.successful(Ok(time_out()).addingToSession(SessionKeys.sessionId -> UUID.randomUUID.toString))

  }


}
