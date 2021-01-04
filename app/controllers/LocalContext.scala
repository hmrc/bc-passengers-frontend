/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import models.JourneyData
import play.api.mvc._

case class LocalContext(request: Request[AnyContent], sessionId: String, journeyData: Option[JourneyData] = None) {

  def withJourneyData(journeyData: JourneyData) = LocalContext(request, sessionId, Some(journeyData))
  def getJourneyData: JourneyData = journeyData.getOrElse(throw new RuntimeException("no journey data."))

  def getFormParam(key: String) = request.body.asFormUrlEncoded.flatMap(_.get(key).flatMap(_.headOption))
}
