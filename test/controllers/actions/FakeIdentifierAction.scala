/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.actions

import models.IdentifierRequest
import play.api.inject.Injector
import play.api.mvc._
import util.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class FakeIdentifierAction extends BaseSpec with IdentifierAction {

  override implicit protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  lazy val injector: Injector = app.injector
  lazy val messagesControllerComponents: MessagesControllerComponents = injector.instanceOf[MessagesControllerComponents]
  override def parser: BodyParser[AnyContent] = messagesControllerComponents.parsers.defaultBodyParser

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    block(IdentifierRequest(request, "credId"))
}
