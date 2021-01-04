/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import play.api.data.Form
import play.api.data.Forms.{optional, single, text}

import scala.util.Try

object UKVatPaidForm {
  val form: Form[Boolean] = Form(
    single(
      "isUKVatPaid" -> optional(text)
        .verifying("error.is_uk_vat_paid", x => x.fold(false)(y => y.nonEmpty && Try(y.toBoolean).toOption.isDefined))
        .transform[Boolean](_.get.toBoolean, s => Some(s.toString))
    )
  )
}
