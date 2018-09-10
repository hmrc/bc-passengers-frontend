package controllers

import java.security.cert.X509Certificate

import models.JourneyData
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future

case class LocalContext(request: Request[AnyContent], journeyData: Option[JourneyData] = None)  extends Request[AnyContent] {
  override def body: AnyContent = request.body
  override def id: Long = request.id
  override def tags: Map[String, String] = request.tags
  override def uri: String = request.uri
  override def path: String = request.path
  override def method: String = request.method
  override def version: String = request.version
  override def queryString: Map[String, Seq[String]] = request.queryString
  override def headers: Headers = request.headers
  override def remoteAddress: String = request.remoteAddress
  override def secure: Boolean = request.secure
  override def clientCertificateChain: Option[Seq[X509Certificate]] = request.clientCertificateChain

  def withJourneyData(journeyData: JourneyData) = LocalContext(request, Some(journeyData))

  def getJourneyData: JourneyData = journeyData.getOrElse(throw new RuntimeException("no journey data."))
}