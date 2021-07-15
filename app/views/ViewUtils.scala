/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views

import play.api.data.Form
import play.api.i18n.Messages
import utils.{CurrencyFormatter, ImplicitDateFormatter}

object ViewUtils extends ImplicitDateFormatter with CurrencyFormatter {

  def title(form: Form[_], titleStr: String, section: Option[String] = None, titleMessageArgs: Seq[String] = Seq())
           (implicit messages: Messages): String = {
      titleNoForm(s"${errorPrefix(form)} ${messages(titleStr, titleMessageArgs: _*)}", section)
  }

  def titleNoForm(title: String, section: Option[String] = None, titleMessageArgs: Seq[String] = Seq())(implicit messages: Messages): String =
    s"${messages(title, titleMessageArgs: _*)} - ${section.fold("")(messages(_) + " - ")}${messages("service.name")} - ${messages("site.govuk")}"

  def errorPrefix(form: Form[_])(implicit messages: Messages): String = {
    if (form.hasErrors || form.hasGlobalErrors) messages("error.browser.title.prefix") else ""
  }
}
