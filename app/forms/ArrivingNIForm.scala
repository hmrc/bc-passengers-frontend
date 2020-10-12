/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package forms

import play.api.data.Form
import play.api.data.Forms.{optional, single, text}

import scala.util.Try

object ArrivingNIForm {

  def validateForm(euCountryCheck: Option[String] = None): Form[Boolean] = Form(
    single(
      "arrivingNI" -> optional(text)
        .verifying("error.arriving_ni", x => x.fold(false)(y => y.nonEmpty && Try(y.toBoolean).toOption.isDefined))
        .verifying("error.arriving_gb", x => x.fold(true)(value => euCountryCheck match {
            case Some("greatBritain") if value == "false" => false
            case _ => true
          }))
        .transform[Boolean](_.get.toBoolean, s => Some(s.toString))
    )
  )

}
