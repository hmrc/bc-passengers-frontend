package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import play.api.mvc._

import scala.concurrent.Future
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

@Singleton
class HelloWorld @Inject()(val messagesApi: MessagesApi, implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  val helloWorld: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.hello_world()))
  }
}
