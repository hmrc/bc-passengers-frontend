/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services


import javax.inject.Singleton
import org.joda.time.DateTime

@Singleton
class DateTimeProviderService {

  def now: DateTime = DateTime.now()

  def javaNow: java.time.LocalDate = java.time.LocalDate.now
}
