/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package models

import play.api.mvc.{Request, WrappedRequest}

case class IdentifierRequest[A] (request: Request[A], providerId: String) extends WrappedRequest[A](request)