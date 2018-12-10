package controllers

import java.security.cert.X509Certificate

import models.JourneyData
import play.api.mvc._

case class LocalContext(request: Request[AnyContent], sessionId: String, journeyData: Option[JourneyData] = None) {

  def withJourneyData(journeyData: JourneyData) = LocalContext(request, sessionId, Some(journeyData))
  def getJourneyData: JourneyData = journeyData.getOrElse(throw new RuntimeException("no journey data."))
}