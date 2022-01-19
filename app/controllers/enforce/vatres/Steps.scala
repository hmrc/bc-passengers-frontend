/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.enforce.vatres

import controllers.enforce.JourneyStep

case object WhereGoodsBoughtAmendmentStep extends JourneyStep(preceeding = List(PreviousDeclarationStep), predicate = _ => _.flatMap(_.prevDeclaration).isDefined)

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

case object DashboardStep extends JourneyStep(preceeding = List(DeclarationRetrievalStep, Is17OrOverStep), predicate = _ => _.flatMap(_.ageOver17).isDefined)

case object ArrivingNIStep extends JourneyStep(preceeding = List(WhereGoodsBoughtStep), predicate = _ => _.flatMap(_.euCountryCheck).isDefined)

case object UKVatPaidStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> x.flatMap(_.euCountryCheck).contains("greatBritain") && x.flatMap(_.arrivingNICheck).contains(true))

case object UKExcisePaidStep extends JourneyStep(preceeding = List(UKResidentStep), predicate = _ => _.flatMap(_.isUKResident).contains(true))

case object UKExcisePaidItemStep extends JourneyStep(preceeding = List(UKResidentStep), predicate = _ => x=> x.flatMap(_.euCountryCheck).contains("greatBritain") && x.flatMap(_.arrivingNICheck).contains(true))

case object UKResidentStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> x.flatMap(_.euCountryCheck).contains("greatBritain") && x.flatMap(_.arrivingNICheck).contains(true))

case object UccReliefStep extends JourneyStep(preceeding = List(UKResidentStep), predicate = _ => x=> x.flatMap(_.isUKResident).contains(false))

case object noNeedToUseServiceGbniStep extends JourneyStep(preceeding = List(UKExcisePaidStep), predicate = _ => x=> x.flatMap(_.isUKVatExcisePaid) .contains(true) && x.flatMap(_.isUKResident) .contains(true))

case object PrivateCraftStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => _.flatMap(_.bringingOverAllowance).isDefined)

case object ZeroDeclarationStep extends JourneyStep(preceeding = List(DashboardStep), predicate = _ => _.flatMap(_.userInformation).isDefined)

case object PreviousDeclarationStep extends JourneyStep(Nil, _ => _ => true)

case object DeclarationRetrievalStep extends JourneyStep(preceeding = List(PreviousDeclarationStep), predicate = _ => _.flatMap(_.prevDeclaration).contains(true))

case object declarationNotFoundStep extends JourneyStep(preceeding = List(DeclarationRetrievalStep), predicate = _ => x=> x.flatMap(_.prevDeclaration).contains(true))

case object noFurtherAmendmentStep extends JourneyStep(preceeding = List(DeclarationRetrievalStep), predicate = _ => x=> x.flatMap(_.prevDeclaration).contains(true))

case object pendingPaymentStep extends JourneyStep(preceeding = List(DeclarationRetrievalStep), predicate = _ => x=> x.flatMap(_.prevDeclaration).contains(true))

case object EUEvidenceItemStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> x.flatMap(_.euCountryCheck).contains("euOnly") && x.flatMap(_.arrivingNICheck).contains(false))
