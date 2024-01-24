/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        .verifying(
          "error.arriving_gb",
          x =>
            x.fold(true)(value =>
              euCountryCheck match {
                case Some("greatBritain") if value == "false" => false
                case _                                        => true
              }
            )
        )
        .transform[Boolean](_.get.toBoolean, s => Some(s.toString))
    )
  )

}
