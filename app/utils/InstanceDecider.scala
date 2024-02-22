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

package utils

import models.JourneyData

trait InstanceDecider extends FormatsAndConversions {

  def originalAmountEnteredWeightOrVolume(journeyData: JourneyData, iid: String): BigDecimal =
    journeyData.workingInstance match {
      case Some(workingProductInstance) => workingProductInstance.weightOrVolume.getOrElseZero
      case None                         => journeyData.purchasedProductInstances.filter(_.iid == iid).flatMap(_.weightOrVolume).sum
    }

  def originalAmountEnteredNoOfSticks(journeyData: JourneyData, iid: String): Int =
    journeyData.workingInstance match {
      case Some(workingProductInstance) => workingProductInstance.noOfSticks.getOrElse(0)
      case None                         => journeyData.purchasedProductInstances.filter(_.iid == iid).flatMap(_.noOfSticks).sum
    }

}
