/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{CheckboxItem, RadioItem, Text}
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

  def checkBoxOptions(items: List[(String, String)])(implicit messages: Messages): Seq[CheckboxItem] = items.zipWithIndex.map {
    case (value, index) =>
      CheckboxItem(
        name = Some(s"tokens[$index]"),
        id = Some(value._2),
        value = value._2,
        content = Text(messages(s"${value._1}")),
      )
  }

  def radioOptions(items: List[(String, String)])(implicit messages: Messages): Seq[RadioItem] = items.zipWithIndex.map {
    case (value, _) =>
      RadioItem(
        id = Some(messages(s"tokens-${value._1}")),
        value = Some(value._1),
        content = Text(messages(s"${value._2}")),
      )
  }
}
