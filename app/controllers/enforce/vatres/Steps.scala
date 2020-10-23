/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.enforce.vatres

import controllers.enforce.JourneyStep

case object WhereGoodsBoughtStep extends JourneyStep(Nil, _ => _ => true)

case object DidYouClaimTaxBackEuOnlyStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> x.flatMap(_.euCountryCheck).contains("euOnly") && x.flatMap(_.arrivingNICheck).isDefined)

case object GoodsBoughtIntoNIStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=>  (x.flatMap(_.euCountryCheck).contains("nonEuOnly") && x.flatMap(_.arrivingNICheck).contains(true))
                                                                                                                  || (x.flatMap(_.euCountryCheck).contains("greatBritain") && x.flatMap(_.arrivingNICheck).contains(true)))

case object GoodsBoughtIntoGBStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> !x.flatMap(_.euCountryCheck).contains("greatBritain") && x.flatMap(_.arrivingNICheck).contains(false))

case object DidYouClaimTaxBackBothStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> x.flatMap(_.euCountryCheck).contains("both") && x.flatMap(_.arrivingNICheck).isDefined)

case object BringingDutyFreeEuStep extends JourneyStep(preceeding = List(DidYouClaimTaxBackEuOnlyStep), predicate = _ => _.flatMap(_.isVatResClaimed).contains(false))

case object GoodsBoughtInsideEuStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> x.flatMap(_.euCountryCheck).contains("euOnly") && x.flatMap(_.arrivingNICheck).contains(true))

case object DeclareDutyFreeEuStep extends JourneyStep(preceeding = List(BringingDutyFreeEuStep), predicate = _ => _.flatMap(_.isBringingDutyFree).contains(true))

case object BringingDutyFreeBothStep extends JourneyStep(preceeding = List(DidYouClaimTaxBackBothStep), predicate = _ => _.flatMap(_.isVatResClaimed).contains(false))

case object DeclareDutyFreeMixStep extends JourneyStep(preceeding = List(BringingDutyFreeBothStep), predicate = _ => _.flatMap(_.isBringingDutyFree).contains(true))

case object NoNeedToUseStep extends JourneyStep(preceeding = List(GoodsBoughtIntoGBStep, GoodsBoughtIntoNIStep, DeclareDutyFreeMixStep, DeclareDutyFreeEuStep), predicate = _ => _.flatMap(_.bringingOverAllowance).contains(false))

case object Is17OrOverStep extends JourneyStep(preceeding = List(PrivateCraftStep), predicate = _ => _.flatMap(_.privateCraft).isDefined)

case object DashboardStep extends JourneyStep(preceeding = List(Is17OrOverStep), predicate = _ => _.flatMap(_.ageOver17).isDefined)

case object ArrivingNIStep extends JourneyStep(preceeding = List(WhereGoodsBoughtStep), predicate = _ => _.flatMap(_.euCountryCheck).isDefined)

case object UKVatPaidStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> x.flatMap(_.euCountryCheck).contains("greatBritain") && x.flatMap(_.arrivingNICheck).contains(true))

case object UKExcisePaidStep extends JourneyStep(preceeding = List(UKVatPaidStep), predicate = _ => _.flatMap(_.isUKVatPaid).isDefined)

case object UKResidentStep extends JourneyStep(preceeding = List(UKExcisePaidStep), predicate = _ => _.flatMap(_.isUKExcisePaid).isDefined)

case object UccReliefStep extends JourneyStep(preceeding = List(UKResidentStep), predicate = _ => x=> x.flatMap(_.isUKResident).contains(false))

case object noNeedToUseServiceGbniStep extends JourneyStep(preceeding = List(UKResidentStep), predicate = _ => x=> x.flatMap(_.isUKVatPaid) .contains(true) && x.flatMap(_.isUKExcisePaid) .contains(true) && x.flatMap(_.isUKResident) .contains(true))

case object PrivateCraftStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => _.flatMap(_.bringingOverAllowance).isDefined)
