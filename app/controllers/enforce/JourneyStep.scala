/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.enforce

import controllers.LocalContext
import models.JourneyData

class JourneyStep(preceeding: List[JourneyStep], predicate: List[JourneyStep] => Option[JourneyData] => Boolean) {

  def meetsAllPrerequisites(implicit context: LocalContext): Boolean = {
    val prereqsMet: List[JourneyStep] = preceeding.filter(_.meetsAllPrerequisites)
    (preceeding.isEmpty || prereqsMet.nonEmpty) && predicate(prereqsMet)(context.journeyData)
  }
}
