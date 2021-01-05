/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import com.google.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

class LocalLanguageController @Inject()(
  configuration: Configuration,
  languageUtils: LanguageUtils,
  override val messagesApi: MessagesApi,
  override protected val controllerComponents: ControllerComponents
) extends LanguageController(configuration, languageUtils, controllerComponents) {

  override def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  override def fallbackURL: String = controllers.routes.DashboardController.showDashboard().url
}