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

package controllers.enforce.nonvatres

import controllers.enforce.JourneyStep

case object WhereGoodsBoughtStep extends JourneyStep(Nil, _ => _ => true)

case object GoodsBoughtOutsideEuStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck).contains("nonEuOnly")) && x.flatMap(_.arrivingNICheck).isDefined)

case object GoodsBoughtInsideEuStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck).contains("euOnly")) && x.flatMap(_.arrivingNICheck).isDefined)

case object GoodsBoughtInAndOutEuStep extends JourneyStep(preceeding = List(ArrivingNIStep), predicate = _ => x=> (x.flatMap(_.euCountryCheck).contains("both")) && x.flatMap(_.arrivingNICheck).isDefined)

case object NoNeedToUseStep extends JourneyStep(preceeding = List(GoodsBoughtInAndOutEuStep, GoodsBoughtOutsideEuStep), predicate = _ =>  _.flatMap(_.bringingOverAllowance).contains(false))

case object PrivateCraftStep extends JourneyStep(preceeding = List(NoNeedToUseStep,GoodsBoughtInAndOutEuStep,GoodsBoughtOutsideEuStep), predicate = _ => _.flatMap(_.bringingOverAllowance).isDefined)

case object Is17OrOverStep extends JourneyStep(preceeding = List(PrivateCraftStep), predicate = _ => _.flatMap(_.privateCraft).isDefined)

case object DashboardStep extends JourneyStep(preceeding = List(Is17OrOverStep), predicate = _ => _.flatMap(_.ageOver17).isDefined)

case object ArrivingNIStep extends JourneyStep(preceeding = List(WhereGoodsBoughtStep), predicate = _ => _.flatMap(_.euCountryCheck).isDefined)
