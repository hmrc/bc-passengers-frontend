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

package views.viewmodels

import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.html.components._

case class TimeInput(
  id: String = "",
  namePrefix: Option[String] = None,
  items: Seq[InputItem] = Seq.empty,
  periodSelectItems: Seq[SelectItem] = Seq.empty,
  hint: Option[Hint] = None,
  errorMessage: Option[ErrorMessage] = None,
  formGroupClasses: String = "",
  fieldset: Option[Fieldset] = None,
  classes: String = "",
  attributes: Map[String, String] = Map.empty,
  showSelectPeriod: Boolean = true
)

object TimeInput {

  def defaultObject: TimeInput = TimeInput()

  given format: OFormat[TimeInput] = Json.format[TimeInput]

}
