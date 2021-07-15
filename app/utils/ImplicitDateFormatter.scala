/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package utils

import java.time.LocalDate

import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.{TimeZone, ULocale}
import org.joda.time.MonthDay
import play.api.i18n.Messages

import scala.language.implicitConversions

trait ImplicitDateFormatter {

  private val midday: Int = 12

  implicit def dateToString(date: LocalDate)(implicit messages: Messages): String = createDateFormatForPattern(
    "d MMMM yyyy").format {
    new SimpleDateFormat("yyyy-MM-dd'T'HH")
      .parse(date.atTime(midday, 0).toString)
  }

  implicit def monthToString(monthDay: MonthDay)(implicit messages: Messages): String = createDateFormatForPattern(
    "d MMMM").format(
    new SimpleDateFormat("yyyy-MM-dd").parse(monthDay.toLocalDate(LocalDate.now().getYear).toString)
  )

  def dayToString(date: LocalDate, dayOfWeek: Boolean = true)(implicit messages: Messages): String = {

    val number = if (date.getDayOfMonth < 20) {date.getDayOfMonth} else {date.getDayOfMonth % 10}

    val outputFormat = if (dayOfWeek){s"EEEE d MMMM yyyy"} else {s"d MMMM yyyy"}

    createDateFormatForPattern(outputFormat).format(new SimpleDateFormat("yyyy-MM-dd").parse(date.toString))
  }

  def oldStringToDate(oldFormattedDate: String): LocalDate = {

    LocalDate.parse(new SimpleDateFormat(
      s"yyyy-MM-dd").format(
      new SimpleDateFormat("HH:mm EEEE dd MMMM yyyy").parse(oldFormattedDate.replace("am, ", " ").replace("pm, ", " "))
    ))
  }

  private val defaultTimeZone: TimeZone = TimeZone.getTimeZone("Europe/London")

  private def createDateFormatForPattern(pattern: String)(implicit messages: Messages): SimpleDateFormat = {
    val uLocale = new ULocale(messages.lang.code)
    val validLang: Boolean = ULocale.getAvailableLocales.contains(uLocale)
    val locale: ULocale = if (validLang) uLocale else ULocale.getDefault
    val sdf = new SimpleDateFormat(pattern, locale)
    sdf.setTimeZone(defaultTimeZone)
    sdf
  }

}
