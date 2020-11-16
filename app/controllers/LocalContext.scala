/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import models.{IdentifierRequest, JourneyData}
import play.api.mvc._

case class LocalContext(request: IdentifierRequest[AnyContent], sessionId: String, journeyData: Option[JourneyData] = None) {

  def withJourneyData(journeyData: JourneyData): LocalContext = LocalContext(request, sessionId, Some(journeyData))
  def getJourneyData: JourneyData = journeyData.getOrElse(throw new RuntimeException("no journey data."))

  def getFormParam(key: String): Option[String] = request.body.asFormUrlEncoded.flatMap(_.get(key).flatMap(_.headOption))
}
