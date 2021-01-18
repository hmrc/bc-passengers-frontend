/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}


@Singleton
class BackLinkModel @Inject() (
  appConfig: AppConfig
) {

  import routes._

  def backLink(implicit context: LocalContext): Option[String] =
    if (appConfig.isVatResJourneyEnabled) backLinkVatRes(context) else backLinkStandard(context)


  def backLinkVatRes(context: LocalContext): Option[String] = {

    val location = context.request.path.split('/').last

    def eucc = context.journeyData.flatMap(_.euCountryCheck)
    def boa = context.journeyData.flatMap(_.bringingOverAllowance).getOrElse(false)
    def ukr = context.journeyData.flatMap(_.isUKResident).getOrElse(false)
    def arN = context.journeyData.flatMap(_.arrivingNICheck).getOrElse(false)

    def call = location match {
      case "duty-free" =>
        Some(TravelDetailsController.didYouClaimTaxBack)
      case "where-goods-bought" =>
        if(appConfig.isAmendmentsEnabled)
          Some(PreviousDeclarationController.loadPreviousDeclarationPage())
        else
          Some(appConfig.declareGoodsUrl)
      case "arriving-ni" =>
        Some(TravelDetailsController.whereGoodsBought)
      case "gb-ni-vat-check" =>{
        val items = context.request.path.split('/')
        val iid = items(items.length-2)
        context.request.path match {
          case path if path.contains("enter-goods/alcohol") => Some(AlcoholInputController.displayEditForm(iid))
          case path if path.contains("enter-goods/tobacco") => Some(TobaccoInputController.displayEditForm(iid))
          case path if path.contains("enter-goods/other-goods") => Some(OtherGoodsInputController.displayEditForm(iid))
        }
      }
      case "goods-bought-into-northern-ireland-inside-eu" =>
        Some(ArrivingNIController.loadArrivingNIPage)
      case "gb-ni-vat-excise-check" =>
        Some(UKResidentController.loadUKResidentPage)
      case "gb-ni-uk-resident-check" =>
        Some(ArrivingNIController.loadArrivingNIPage)
      case "gb-ni-exemptions" =>
        Some(UKResidentController.loadUKResidentPage())
      case "gb-ni-no-need-to-use-service" =>
        Some(UKExcisePaidController.loadUKExcisePaidPage)
      case "goods-brought-into-northern-ireland" if eucc!=Some("greatBritain") =>
        Some(ArrivingNIController.loadArrivingNIPage)
      case "goods-brought-into-northern-ireland" if eucc==Some("greatBritain") & !ukr =>
        Some(UKResidentController.loadUKResidentPage)
      case "goods-brought-into-northern-ireland" if eucc==Some("greatBritain") & ukr =>
        Some(UKExcisePaidController.loadUKExcisePaidPage())
      case "goods-brought-into-great-britain-iom" =>
        Some(ArrivingNIController.loadArrivingNIPage)
      case "private-travel" if arN & boa =>
        Some(TravelDetailsController.goodsBoughtIntoNI)
      case "private-travel" if !arN & boa =>
        Some(TravelDetailsController.goodsBoughtIntoGB)
      case "private-travel" if !boa=>
        Some(TravelDetailsController.noNeedToUseService)
      case "no-need-to-use-service" if arN =>
        Some(TravelDetailsController.goodsBoughtIntoNI())
      case "no-need-to-use-service" if !arN =>
        Some(TravelDetailsController.goodsBoughtIntoGB())
      case "confirm-age" =>
        Some(TravelDetailsController.privateTravel)
      case "tell-us" =>
        Some(TravelDetailsController.confirmAge)
      case "ireland-to-northern-ireland" =>
        Some(DashboardController.showDashboard)
      case "tax-due" if appConfig.isIrishBorderQuestionEnabled =>
        Some(CalculateDeclareController.irishBorder)
      case "tax-due" if !appConfig.isIrishBorderQuestionEnabled =>
        Some(DashboardController.showDashboard)
      case "declare-your-goods" =>
        Some(CalculateDeclareController.showCalculation())
      case "user-information" =>
        Some(CalculateDeclareController.declareYourGoods())
      case "previous-declaration" =>
        Some(appConfig.declareGoodsUrl)
      case _ =>
        None
    }

    call.map(_.toString)

  }

  def backLinkStandard(context: LocalContext): Option[String] = {
    backLinkVatRes(context)
  }
}
