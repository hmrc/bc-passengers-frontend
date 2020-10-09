/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package forms
import play.api.data.Form
import play.api.data.Forms.{optional, single, text}
import scala.util.Try

object UKResidentForm {
  val form: Form[Boolean] = Form(
    single(
      "isUKResident" -> optional(text)
        .verifying("error.is_uk_resident", x => x.fold(false)(y => y.nonEmpty && Try(y.toBoolean).toOption.isDefined))
        .transform[Boolean](_.get.toBoolean, s => Some(s.toString))
    )
  )
}
