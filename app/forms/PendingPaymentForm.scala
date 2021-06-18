/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import play.api.data.Form
import play.api.data.Forms.{optional, single, text}

import scala.util.Try

object PendingPaymentForm {
  val form: Form[Boolean] = Form(
    single(
      "pendingPayment" -> optional(text)
        .verifying("error.pay_now_if_you_want_to", x => x.fold(false)(y => y.nonEmpty && Try(y.toBoolean).toOption.isDefined))
        .transform[Boolean](_.get.toBoolean, s => Some(s.toString))
    )
  )
}
