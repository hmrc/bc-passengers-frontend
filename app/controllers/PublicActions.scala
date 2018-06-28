package controllers

import java.security.cert.X509Certificate

import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future

trait PublicActions extends FrontendController {

  def PublicAction(block: LocalContext => Future[Result]): Action[AnyContent] = {

    UnauthorisedAction.async { implicit request =>

      trimmingFormUrlEncodedData { implicit request =>

        request.session.get(SessionKeys.sessionId) match {
          case Some(s) =>
            block(LocalContext(request))
          case None =>
            Future.successful(Redirect(routes.TravelDetailsController.start()))
        }

      }
    }
  }

  def trimmingFormUrlEncodedData(block: Request[AnyContent] => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    block {
      request.map {
        case AnyContentAsFormUrlEncoded(data) =>
          AnyContentAsFormUrlEncoded(data.map {
            case (key, vals) => (key, vals.map(_.trim))
          })
        case b => b
      }
    }
  }
}

case class LocalContext(request: Request[AnyContent]) extends Request[AnyContent] {
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
}