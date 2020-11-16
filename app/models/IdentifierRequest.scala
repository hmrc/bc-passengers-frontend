/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package models

import play.api.mvc.{Request, WrappedRequest}

case class IdentifierRequest[A] (request: Request[A], credId: String) extends WrappedRequest[A](request)