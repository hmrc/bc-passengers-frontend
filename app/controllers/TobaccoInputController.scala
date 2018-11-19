package controllers

import config.AppConfig
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent
import scala.concurrent.Future

class TobaccoInputController @Inject()(
  val countriesService: CountriesService,
  val travelDetailsService: TravelDetailsService,
  val purchasedProductService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def loadTobaccoInputPage(path: ProductPath, iid: String): Future[Result] = {
    Future.successful {
      path.components.last match {
        case "cigarettes"            =>  Redirect(routes.TobaccoInputController.displayNoOfSticksInput(path, iid))
        case "cigars" | "cigarillos" =>  Redirect(routes.TobaccoInputController.displayNoOfSticksWeightInput(path, iid))
        case "rolling" | "chewing"   =>  Redirect(routes.TobaccoInputController.displayWeightInput(path, iid))
      }
    }
  }

  def inputNoOfSticksAndWeight(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>
    val iid = generateIid
    loadTobaccoInputPage(path, iid)
  }

  def updateNoOfSticksAndWeight(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) flatMap { _ =>
        loadTobaccoInputPage(path, iid)
      }
    }
  }


  def displayNoOfSticksInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, Some(qty), _,  _, _)) if workingIid == iid => NoOfSticksDto.form.fill(NoOfSticksDto(qty))
        case _ => NoOfSticksDto.form
      }
    }

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.no_of_sticks_input(form, product.token, product.name, path, iid)))
    }
  }

  def processNoOfSticksInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    NoOfSticksDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.no_of_sticks_input(formWithErrors, product.token, product.name, path, iid)))
        }
      },
      dto => {

        context.getJourneyData.workingInstance match {
          case Some(wi@PurchasedProductInstance(_, _, _, Some(_), Some(_), Some(_), Some(_))) if wi.iid == iid => purchasedProductService.updateNoOfSticks(context.getJourneyData, path, iid, dto.noOfSticks) map { _ =>
            Redirect(routes.DashboardController.showDashboard())
          }
          case _ => purchasedProductService.storeNoOfSticks(context.getJourneyData, path, iid, dto.noOfSticks) map { _ =>
            Redirect(routes.TobaccoInputController.displayCountryInput(path, iid))
          }
        }
      }
    )
  }


  def displayNoOfSticksWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, Some(weight), Some(qty), _, _, _)) if workingIid == iid => NoOfSticksAndWeightDto.form.fill(NoOfSticksAndWeightDto(qty, weight))
        case _ => NoOfSticksAndWeightDto.form
      }
    }
    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.no_of_sticks_weight_input(form, product.token, product.name, path, iid)))
    }
  }

  def processNoOfSticksWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    NoOfSticksAndWeightDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.no_of_sticks_weight_input(formWithErrors, product.token, product.name, path, iid)))
        }
      },
      dto => {

        context.getJourneyData.workingInstance match {
          case Some(PurchasedProductInstance(_, workingIid, Some(_), Some(_), Some(_), Some(_), Some(_))) if workingIid == iid => purchasedProductService.updateNoOfSticksAndWeightOrVolume(context.getJourneyData, path, iid, dto.noOfSticks, dto.weight) map { _ =>
            Redirect(routes.DashboardController.showDashboard())
          }
          case _ => purchasedProductService.storeNoOfSticksAndWeightOrVolume(context.getJourneyData, path, iid, dto.noOfSticks, dto.weight) map { _ =>
            Redirect(routes.TobaccoInputController.displayCountryInput(path, iid))
          }
        }
      }
    )
  }


  def displayWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, Some(weight), _, _, _, _)) if workingIid == iid => WeightDto.form.fill(WeightDto(weight))
        case _ => WeightDto.form
      }
    }

    requireProduct(path) { product =>
      Future.successful(Ok(views.html.tobacco.weight_input(form, product.token, product.name, path, iid)))
    }
  }

  def processWeightInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    WeightDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(views.html.tobacco.weight_input(formWithErrors, product.token, product.name, path, iid)))
        }
      },
      dto => {

        context.getJourneyData.workingInstance match {
          case Some(PurchasedProductInstance(_, workingIid, Some(_), _, Some(_), Some(_), Some(_))) if workingIid == iid => purchasedProductService.updateWeightOrVolume(context.getJourneyData, path, iid, dto.weight) map { _ =>
            Redirect(routes.DashboardController.showDashboard())
          }
          case _ => purchasedProductService.storeWeightOrVolume(context.getJourneyData, path, iid, dto.weight) map { _ =>
            Redirect(routes.TobaccoInputController.displayCountryInput(path, iid))
          }
        }
      }
    )
  }

  def displayCountryInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, _, Some(country), _, _)) if workingIid == iid => SelectedCountryDto.form(countriesService).fill(SelectedCountryDto(country.countryName, 0))
        case _ => SelectedCountryDto.form(countriesService)
      }
    }

    requireProduct(path) { product =>
      requireWorkingInstanceDescription(product) { description =>
        Future.successful(Ok(views.html.tobacco.country_of_purchase(form, product, path, iid, countriesService.getAllCountries, description)))
      }
    }
  }

  def displayCountryUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) map { _ =>
        Redirect(routes.TobaccoInputController.displayCountryInput(path, iid))
      }
    }
  }

  def processCountryInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    SelectedCountryDto.form(countriesService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          requireWorkingInstanceDescription(product) { description =>
            Future.successful(BadRequest(views.html.tobacco.country_of_purchase(formWithErrors, product, path, iid, countriesService.getAllCountries, description)))
          }
        }
      },
      selectedCountryDto => {
        requireCountryByName(selectedCountryDto.country) { country =>
          context.getJourneyData.workingInstance match {

            case Some(PurchasedProductInstance(_, workingIid, _, _, Some(_), Some(_), Some(_))) if workingIid == iid => purchasedProductService.updateCountry(context.getJourneyData, path, iid, country) map { _ =>
              Redirect(routes.DashboardController.showDashboard())
            }
            case _ => purchasedProductService.storeCountry(context.getJourneyData, path, iid, country) map { _ =>
              Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid))
            }
          }
        }
      }
    )
  }

  def displayCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, _, _, Some(currency), _)) if workingIid == iid => CurrencyDto.form(currencyService).fill(CurrencyDto(currency, 0))
        case _ => CurrencyDto.form(currencyService)
      }
    }

    requireProduct(path) { product =>
      requireWorkingInstanceDescription(product) { description =>
        Future.successful(Ok(views.html.tobacco.currency_input(form, product, currencyService.getAllCurrencies, description, path, iid)))
      }
    }
  }

  def displayCurrencyUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) flatMap { _ =>
        Future.successful(Redirect(routes.TobaccoInputController.displayCurrencyInput(path, iid)))
      }
    }
  }

  def processCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          requireWorkingInstanceDescription(product) { description =>
            Future.successful(BadRequest(views.html.tobacco.currency_input(formWithErrors, product, currencyService.getAllCurrencies, description, path, iid)))
          }
        }
      },
      currencyDto => {
        purchasedProductService.storeCurrency(context.getJourneyData, path, iid, currencyDto.currency) map { _ =>
          Redirect(routes.TobaccoInputController.displayCostInput(path, iid))
        }
      }
    )
  }


  def displayCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, _, _, _, Some(cost))) if workingIid == iid => CostDto.form().fill(CostDto(cost, 0))
        case _ => CostDto.form()
      }
    }

    requireProduct(path) { product =>
      requireWorkingInstanceDescription(product) { description =>
        requireWorkingInstanceCurrency { currency =>
            Future.successful(Ok(views.html.tobacco.cost_input(form, product, path, iid, description, currency.displayName)))
        }
      }
    }
  }

  def processCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CostDto.form().bindFromRequest.fold(
      formWithErrors => {

        requireProduct(path) { product =>
          requireWorkingInstanceDescription(product) { description =>
            requireWorkingInstanceCurrency { currency =>
              requireProduct(path) { product =>
                Future.successful(BadRequest(views.html.tobacco.cost_input(formWithErrors, product, path, iid, description, currency.displayName)))
              }
            }
          }
        }
      },
      dto => {

        requireWorkingInstance { workingInstance =>

          requireProduct(path) { product =>

            val wi = workingInstance.copy(cost = Some(dto.cost))

            movingValidWorkingInstance(wi, product) {
              case Some(updatedJourneyData) =>
                purchasedProductService.cacheJourneyData(updatedJourneyData).map { _ =>
                  Redirect(routes.SelectProductController.nextStep())
                }
              case None =>
                Future.successful(Redirect(routes.SelectProductController.nextStep()))
            }
          }
        }
      }
    )
  }
}
