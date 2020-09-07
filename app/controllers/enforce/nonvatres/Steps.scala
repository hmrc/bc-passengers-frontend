/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.enforce.nonvatres

import controllers.enforce.JourneyStep

case object WhereGoodsBoughtStep extends JourneyStep(Nil, _ => _ => true)

case object GoodsBoughtOutsideEuStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck) == Some("nonEuOnly")) && x.flatMap(_.arrivingNICheck).isDefined)

case object GoodsBoughtInsideEuStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck) == Some("euOnly")) && x.flatMap(_.arrivingNICheck).isDefined)

case object GoodsBoughtInAndOutEuStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck) == Some("both")) && x.flatMap(_.arrivingNICheck).isDefined)

case object NoNeedToUseStep extends JourneyStep(preceeding = List(GoodsBoughtInAndOutEuStep, GoodsBoughtOutsideEuStep), predicate = _ => _.flatMap(_.bringingOverAllowance) == Some(false))

case object PrivateCraftStep extends JourneyStep(preceeding = List(NoNeedToUseStep,GoodsBoughtInAndOutEuStep,GoodsBoughtOutsideEuStep), predicate = _ => _.flatMap(_.bringingOverAllowance).isDefined)

case object Is17OrOverStep extends JourneyStep(preceeding = List(PrivateCraftStep), predicate = _ => _.flatMap(_.privateCraft).isDefined)

case object DashboardStep extends JourneyStep(preceeding = List(Is17OrOverStep), predicate = _ => _.flatMap(_.ageOver17).isDefined)

case object ArrivingNIStep extends JourneyStep(preceeding = List(WhereGoodsBoughtStep), predicate = _ => _.flatMap(_.euCountryCheck).isDefined)
