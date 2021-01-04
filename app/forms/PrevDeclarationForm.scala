/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package forms

import play.api.data.Form
import play.api.data.Forms.{optional, single, text}

import scala.util.Try

object PrevDeclarationForm {

  def validateForm(): Form[Boolean] = Form(
    single(
      "prevDeclaration" -> optional(text)
        .verifying("error.previous_declaration", x => x.fold(false)(y => y.nonEmpty && Try(y.toBoolean).toOption.isDefined))
        )
        .transform[Boolean](_.get.toBoolean, s => Some(s.toString))
    )
}
