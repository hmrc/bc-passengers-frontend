package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.Configuration


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
    def vrc = context.journeyData.flatMap(_.isVatResClaimed).getOrElse(false)
    def bdf = context.journeyData.flatMap(_.isBringingDutyFree).getOrElse(false)
    def boa = context.journeyData.flatMap(_.bringingOverAllowance).getOrElse(false)

    def call = location match {
      case "did-you-claim-tax-back" | "goods-bought-outside-eu" =>
        Some(TravelDetailsController.whereGoodsBought)
      case "duty-free" =>
        Some(TravelDetailsController.didYouClaimTaxBack)
      case "goods-bought-inside-eu" | "duty-free-eu" | "goods-bought-inside-and-outside-eu" | "duty-free-mix" =>
        Some(TravelDetailsController.dutyFree)
      case "private-travel" if eucc==Some("both") & !vrc & bdf & !boa =>
        Some(TravelDetailsController.noNeedToUseService)
      case "private-travel" if eucc==Some("both") & !vrc & !bdf & boa =>
        Some(TravelDetailsController.goodsBoughtInsideAndOutsideEu)
      case "private-travel" if eucc==Some("both") & !vrc & !bdf & !boa =>
        Some(TravelDetailsController.noNeedToUseService)
      case "private-travel" if eucc==Some("both") & !vrc & bdf & boa =>
        Some(TravelDetailsController.bringingDutyFreeQuestionMix())
      case "private-travel" if eucc==Some("both") & vrc =>
        Some(TravelDetailsController.didYouClaimTaxBack)
      case "private-travel" if eucc==Some("nonEuOnly")  & boa=>
        Some(TravelDetailsController.goodsBoughtOutsideEu)
      case "private-travel" if eucc==Some("nonEuOnly") & !boa =>
        Some(TravelDetailsController.noNeedToUseService)
      case "private-travel" if eucc==Some("euOnly") & vrc =>
        Some(TravelDetailsController.didYouClaimTaxBack)
      case "private-travel" if eucc==Some("euOnly") & !vrc & boa=>
        Some(TravelDetailsController.bringingDutyFreeQuestionEu())
      case "private-travel" if eucc==Some("euOnly") & !vrc & !boa=>
        Some(TravelDetailsController.noNeedToUseService)
      case "no-need-to-use-service" if eucc==Some("both") & !bdf =>
        Some(TravelDetailsController.goodsBoughtInsideAndOutsideEu)
      case "no-need-to-use-service" if eucc==Some("both") & bdf =>
        Some(TravelDetailsController.bringingDutyFreeQuestionMix())
      case "no-need-to-use-service" if eucc==Some("euOnly") =>
        Some(TravelDetailsController.bringingDutyFreeQuestionEu())
      case "no-need-to-use-service" if eucc==Some("nonEuOnly") =>
        Some(TravelDetailsController.goodsBoughtOutsideEu)
      case "confirm-age" =>
        Some(TravelDetailsController.privateTravel)
      case "tell-us" =>
        Some(TravelDetailsController.confirmAge)
      case "ireland-to-northern-ireland" =>
        Some(DashboardController.showDashboard)
      case "calculation" if appConfig.isIrishBorderQuestionEnabled =>
        Some(CalculateDeclareController.irishBorder)
      case "calculation" if !appConfig.isIrishBorderQuestionEnabled =>
        Some(DashboardController.showDashboard)
      case _ =>
        None
    }

    call.map(_.toString)

  }

  def backLinkStandard(context: LocalContext): Option[String] = {

    val location = context.request.path.split('/').last

    def eucc = context.journeyData.flatMap(_.euCountryCheck)
    def vrc = context.journeyData.flatMap(_.isVatResClaimed).getOrElse(false)
    def bdf = context.journeyData.flatMap(_.isBringingDutyFree).getOrElse(false)
    def boa = context.journeyData.flatMap(_.bringingOverAllowance).getOrElse(false)

    def call = location match {
      case "goods-bought-inside-eu" | "goods-bought-inside-and-outside-eu" | "goods-bought-outside-eu" =>
        Some(TravelDetailsController.whereGoodsBought)
      case "private-travel" if eucc==Some("both") & boa =>
        Some(TravelDetailsController.goodsBoughtInsideAndOutsideEuPost)
      case "private-travel" if eucc==Some("both") & !boa =>
        Some(TravelDetailsController.noNeedToUseService)
      case "private-travel" if eucc==Some("nonEuOnly") & !boa =>
        Some(TravelDetailsController.noNeedToUseService)
      case "private-travel" if eucc==Some("nonEuOnly") & boa=>
        Some(TravelDetailsController.goodsBoughtOutsideEu)
      case "no-need-to-use-service" if eucc==Some("both") =>
        Some(TravelDetailsController.goodsBoughtInsideAndOutsideEu)
      case "no-need-to-use-service" if eucc==Some("nonEuOnly") =>
        Some(TravelDetailsController.goodsBoughtOutsideEu)
      case "confirm-age" =>
        Some(TravelDetailsController.privateTravel)
      case "tell-us" =>
        Some(TravelDetailsController.confirmAge)
      case "ireland-to-northern-ireland" =>
        Some(DashboardController.showDashboard())
      case "calculation" if appConfig.isIrishBorderQuestionEnabled =>
        Some(CalculateDeclareController.irishBorder)
      case "calculation" if !appConfig.isIrishBorderQuestionEnabled =>
        Some(DashboardController.showDashboard)
      case _ =>
        None
    }

    call.map(_.toString)

  }
}
