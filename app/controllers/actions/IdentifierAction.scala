/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import controllers.routes
import models.IdentifierRequest
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthenticatedIdentifierAction])
trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject()(override val authConnector: AuthConnector,
                                              appConfig: AppConfig,
                                              val parser: BodyParsers.Default
                                             )(implicit val executionContext: ExecutionContext) extends IdentifierAction
  with AuthorisedFunctions with AuthRedirects {

  val config: Configuration = appConfig.runModeConfiguration
  val env: Environment = appConfig.environment

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(AuthProviders(PrivilegedApplication) and Enrolment(appConfig.role)).retrieve(credentials) {
      case Some(Credentials(providerId, _)) =>
        block(IdentifierRequest(request, providerId))
      case None =>
        Logger.warn("[IdentifierAction][invokeBlock] Could not retrieve Provider ID from auth")
        Future.successful(Redirect(routes.UnauthorisedController.show()))
    } recover {
      case _: NoActiveSession =>
        Logger.info("[IdentifierAction][invokeBlock] No active session found - redirecting to login")
        toStrideLogin(appConfig.loginContinueUrl)
      case e: AuthorisationException =>
        Logger.error(s"[IdentifierAction][invokeBlock] Unauthorised user: ${e.getMessage}")
        Redirect(routes.UnauthorisedController.show())
    }
  }
}
