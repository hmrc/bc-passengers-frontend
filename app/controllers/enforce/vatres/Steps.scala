/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.enforce.vatres

import controllers.enforce.JourneyStep

case object WhereGoodsBoughtStep extends JourneyStep(Nil, _ => _ => true)

case object DidYouClaimTaxBackEuOnlyStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck) == Some("euOnly")) && x.flatMap(_.arrivingNICheck).isDefined)

case object GoodsBoughtOutsideEuStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck) == Some("nonEuOnly")) && x.flatMap(_.arrivingNICheck).isDefined)

case object DidYouClaimTaxBackBothStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck) == Some("both")) && x.flatMap(_.arrivingNICheck).isDefined)

case object BringingDutyFreeEuStep extends JourneyStep(preceeding = List(DidYouClaimTaxBackEuOnlyStep), predicate = _ => _.flatMap(_.isVatResClaimed) == Some(false))

case object GoodsBoughtInsideEuStep extends JourneyStep(preceeding = List(BringingDutyFreeEuStep), predicate = _ => _.flatMap(_.isBringingDutyFree) == Some(false))

case object DeclareDutyFreeEuStep extends JourneyStep(preceeding = List(BringingDutyFreeEuStep), predicate = _ => _.flatMap(_.isBringingDutyFree) == Some(true))

case object BringingDutyFreeBothStep extends JourneyStep(preceeding = List(DidYouClaimTaxBackBothStep), predicate = _ => _.flatMap(_.isVatResClaimed) == Some(false))

case object GoodsBoughtInAndOutEuStep extends JourneyStep(preceeding = List(BringingDutyFreeBothStep), predicate = _ => _.flatMap(_.isBringingDutyFree) == Some(false))

case object DeclareDutyFreeMixStep extends JourneyStep(preceeding = List(BringingDutyFreeBothStep), predicate = _ => _.flatMap(_.isBringingDutyFree) == Some(true))

case object NoNeedToUseStep extends JourneyStep(preceeding = List(GoodsBoughtInAndOutEuStep, GoodsBoughtOutsideEuStep, DeclareDutyFreeMixStep, DeclareDutyFreeEuStep), predicate = _ => _.flatMap(_.bringingOverAllowance) == Some(false))

case object PrivateCraftStep extends JourneyStep(preceeding = List(NoNeedToUseStep,DeclareDutyFreeMixStep,GoodsBoughtInAndOutEuStep,
                                                                   GoodsBoughtOutsideEuStep,DidYouClaimTaxBackBothStep,DidYouClaimTaxBackEuOnlyStep,
                                                                   DeclareDutyFreeEuStep), predicate = { prereqsMet => jd =>

  if(!List(GoodsBoughtOutsideEuStep, NoNeedToUseStep, DeclareDutyFreeEuStep, DeclareDutyFreeMixStep, GoodsBoughtInAndOutEuStep).intersect(prereqsMet).isEmpty) {
    jd.flatMap(_.bringingOverAllowance).isDefined
  }
  else if(!List(DidYouClaimTaxBackBothStep, DidYouClaimTaxBackEuOnlyStep).intersect(prereqsMet).isEmpty) {
    jd.flatMap(_.isVatResClaimed)==Some(true)
  }
  else false

})

case object Is17OrOverStep extends JourneyStep(preceeding = List(PrivateCraftStep), predicate = _ => _.flatMap(_.privateCraft).isDefined)

case object DashboardStep extends JourneyStep(preceeding = List(Is17OrOverStep), predicate = _ => _.flatMap(_.ageOver17).isDefined)

case object ArrivingNIStep extends JourneyStep(preceeding = List(WhereGoodsBoughtStep), predicate = _ => _.flatMap(_.euCountryCheck).isDefined)