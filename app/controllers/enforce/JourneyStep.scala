/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.enforce

import controllers.LocalContext
import models.JourneyData

class JourneyStep(preceeding: List[JourneyStep], predicate: List[JourneyStep] => Option[JourneyData] => Boolean) {

  def meetsAllPrerequisites(implicit context: LocalContext): Boolean = {
    val prereqsMet: List[JourneyStep] = preceeding.filter(_.meetsAllPrerequisites)
    (preceeding.isEmpty || !prereqsMet.isEmpty) && predicate(prereqsMet)(context.journeyData)
  }
}
