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

package controllers

import config.AppConfig

import javax.inject.{Inject, Singleton}

@Singleton
class BackLinkModel @Inject() (appConfig: AppConfig) {

  import routes._

  def backLink(implicit context: LocalContext): Option[String] =
    if (appConfig.isVatResJourneyEnabled) backLinkVatRes(context) else backLinkStandard(context)

  def backLinkVatRes(context: LocalContext): Option[String] = {

    val path     = context.request.path
    val location = path.split('/').last

    def eucc     = context.journeyData.flatMap(_.euCountryCheck)
    def boa      = context.journeyData.flatMap(_.bringingOverAllowance).getOrElse(false)
    def ukr      = context.journeyData.flatMap(_.isUKResident).getOrElse(false)
    def arN      = context.journeyData.flatMap(_.arrivingNICheck).getOrElse(false)
    def prevDecl = context.journeyData.flatMap(_.prevDeclaration).getOrElse(false)

    def call = location match {
      case "duty-free"                                                                    =>
        Some(TravelDetailsController.didYouClaimTaxBack)
      case "where-goods-bought"                                                           =>
        if (appConfig.isAmendmentsEnabled) {
          Some(PreviousDeclarationController.loadPreviousDeclarationPage)
        } else {
          Some(appConfig.declareGoodsUrl)
        }
      case "declaration-not-found"                                                        =>
        Some(DeclarationRetrievalController.loadDeclarationRetrievalPage)
      case "arriving-ni"                                                                  =>
        Some(TravelDetailsController.whereGoodsBought)
      case "gb-ni-vat-check"                                                              =>
        val iid = getIid(context.request.path)
        context.request.path match {
          case path if path.contains("enter-goods/alcohol")     => Some(AlcoholInputController.displayEditForm(iid))
          case path if path.contains("enter-goods/tobacco")     => Some(TobaccoInputController.displayEditForm(iid))
          case path if path.contains("enter-goods/other-goods") => Some(OtherGoodsInputController.displayEditForm(iid))
        }
      case "gb-ni-excise-check"
          if context.request.path.contains("enter-goods/alcohol")
            || context.request.path.contains("enter-goods/tobacco") =>
        val iid = getIid(context.request.path)
        Some(
          UKVatPaidController.loadItemUKVatPaidPage(
            context.getJourneyData.purchasedProductInstances.filter(ppi => ppi.iid == iid).head.path,
            iid
          )
        )
      case "goods-bought-into-northern-ireland-inside-eu"                                 =>
        Some(ArrivingNIController.loadArrivingNIPage)
      case "gb-ni-vat-excise-check"                                                       =>
        Some(UKResidentController.loadUKResidentPage)
      case "gb-ni-uk-resident-check"                                                      =>
        Some(ArrivingNIController.loadArrivingNIPage)
      case "gb-ni-exemptions" if context.request.path.contains("enter-goods/other-goods") =>
        val iid = getIid(context.request.path)
        Some(
          UKVatPaidController.loadItemUKVatPaidPage(
            context.getJourneyData.purchasedProductInstances.filter(ppi => ppi.iid == iid).head.path,
            iid
          )
        )
      case "eu-evidence-check" if eucc.contains("euOnly") & !arN                          =>
        val iid = getIid(context.request.path)
        context.request.path match {
          case path if path.contains("enter-goods/alcohol")     => Some(AlcoholInputController.displayEditForm(iid))
          case path if path.contains("enter-goods/tobacco")     => Some(TobaccoInputController.displayEditForm(iid))
          case path if path.contains("enter-goods/other-goods") => Some(OtherGoodsInputController.displayEditForm(iid))
        }
      case "gb-ni-no-need-to-use-service"                                                 =>
        Some(UKExcisePaidController.loadUKExcisePaidPage)
      case "goods-brought-into-northern-ireland" if !eucc.contains("greatBritain")        =>
        Some(ArrivingNIController.loadArrivingNIPage)
      case "goods-brought-into-northern-ireland" if eucc.contains("greatBritain") & !ukr  =>
        Some(UKResidentController.loadUKResidentPage)
      case "goods-brought-into-northern-ireland" if eucc.contains("greatBritain") & ukr   =>
        Some(UKExcisePaidController.loadUKExcisePaidPage)
      case "goods-brought-into-great-britain-iom"                                         =>
        Some(ArrivingNIController.loadArrivingNIPage)
      case "private-travel" if arN & boa                                                  =>
        Some(TravelDetailsController.goodsBoughtIntoNI)
      case "private-travel" if !arN & boa                                                 =>
        Some(TravelDetailsController.goodsBoughtIntoGB)
      case "private-travel" if !boa                                                       =>
        Some(TravelDetailsController.noNeedToUseService)
      case "no-need-to-use-service" if arN                                                =>
        Some(TravelDetailsController.goodsBoughtIntoNI)
      case "no-need-to-use-service" if !arN                                               =>
        Some(TravelDetailsController.goodsBoughtIntoGB)
      case "confirm-age"                                                                  =>
        Some(TravelDetailsController.privateTravel)
      case "tell-us"                                                                      =>
        if (prevDecl) {
          Some(DeclarationRetrievalController.loadDeclarationRetrievalPage)
        } else {
          Some(TravelDetailsController.confirmAge)
        }
      case "ireland-to-northern-ireland"                                                  =>
        Some(DashboardController.showDashboard)
      case "tax-due" if appConfig.isIrishBorderQuestionEnabled                            =>
        Some(CalculateDeclareController.irishBorder)
      case "tax-due" if !appConfig.isIrishBorderQuestionEnabled                           =>
        Some(DashboardController.showDashboard)
      case "declare-your-goods"                                                           =>
        Some(CalculateDeclareController.showCalculation)
      case "user-information"                                                             =>
        Some(CalculateDeclareController.declareYourGoods)
      case "previous-declaration"                                                         =>
        Some(appConfig.declareGoodsUrl)
      case "declaration-retrieval"                                                        =>
        Some(PreviousDeclarationController.loadPreviousDeclarationPage)
      case "pending-payment"                                                              =>
        Some(DeclarationRetrievalController.loadDeclarationRetrievalPage)
      case "no-further-amendments"                                                        =>
        Some(PendingPaymentController.loadPendingPaymentPage)
      case x // other goods has cancel button that is because it is following different pattern of adding items
          if path.endsWith("select-goods/alcohol")
            || path.endsWith("select-goods/tobacco") =>
        Some(DashboardController.showDashboard)
      case _                                                                              =>
        None
    }

    call.map(_.toString)

  }

  def backLinkStandard(context: LocalContext): Option[String] =
    backLinkVatRes(context)

  private def getIid(path: String): String = {
    val items = path.split('/')
    items(items.length - 2)
  }
}
