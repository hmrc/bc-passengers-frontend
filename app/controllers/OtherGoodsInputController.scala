package controllers

import config.AppConfig
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent
import scala.concurrent.{ExecutionContext, Future}

class OtherGoodsInputController @Inject() (
  val countriesService: CountriesService,
  val calculatorService: CalculatorService,
  val travelDetailsService: TravelDetailsService,
  val purchasedProductService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService,
  val quantity_input: views.html.other_goods.quantity_input,
  val country_of_purchase: views.html.other_goods.country_of_purchase,
  val currency_input: views.html.other_goods.currency_input,
  val cost_input: views.html.other_goods.cost_input,
  val error_template: views.html.error_template,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def displayQuantityInput(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      Future.successful(Ok(quantity_input(QuantityDto.form, product.name, product.token, path)))
    }
  }

  def processQuantityInput(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    QuantityDto.form.bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(quantity_input(formWithErrors, product.name, product.token, path)))
        }
      },
      quantityDto => {
        Future.successful(Redirect(routes.OtherGoodsInputController.displayCountryInput(path, generateIid, quantityDto.quantity)))
      }
    )
  }

  def displayCountryInput(path: ProductPath, iid: String, itemsRemaining: Int): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, _, Some(country), _, _)) if workingIid == iid => SelectedCountryDto.form(countriesService).fill(SelectedCountryDto(country.countryName, itemsRemaining)).discardingErrors
        case _ => SelectedCountryDto.form(countriesService).bind(Map("itemsRemaining" -> itemsRemaining.toString)).discardingErrors
      }
    }

    requireProduct(path) { product =>
      Future.successful(Ok(country_of_purchase(form, product, path, iid, countriesService.getAllCountries)))
    }
  }

  def displayCountryUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) map { _ =>
        Redirect(routes.OtherGoodsInputController.displayCountryInput(path, iid, 0))
      }
    }
  }


  def processCountryInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    SelectedCountryDto.form(countriesService, optionalItemsRemaining = false).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(country_of_purchase(formWithErrors, product, path, iid, countriesService.getAllCountries)))
        }
      },
      selectedCountryDto => {
        requireCountryByName(selectedCountryDto.country) { country =>
        context.getJourneyData.workingInstance match {
            case Some(PurchasedProductInstance(_, workingIid, _, _, Some(_), Some(_), Some(_))) if workingIid == iid => purchasedProductService.updateCountry(context.getJourneyData, path, iid, country) map { _ =>
              Redirect(routes.DashboardController.showDashboard())
            }
            case _ => purchasedProductService.storeCountry(context.getJourneyData, path, iid, country) map { _ =>
              Redirect(routes.OtherGoodsInputController.displayCurrencyInput(path, iid, selectedCountryDto.itemsRemaining))
            }
          }
        }
      }
    )
  }

  def displayCurrencyInput(path: ProductPath, iid: String, itemsRemaining: Int): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, _, _, Some(currency), _)) if workingIid == iid => CurrencyDto.form(currencyService).fill(CurrencyDto(currency, itemsRemaining)).discardingErrors
        case _ => CurrencyDto.form(currencyService).bind(Map("itemsRemaining" -> itemsRemaining.toString)).discardingErrors
      }
    }

    requireProduct(path) { product =>
      Future.successful(Ok(currency_input(form, product, path, currencyService.getAllCurrencies, iid)))
    }
  }

  def displayCurrencyUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) flatMap { _ =>
          Future.successful(Redirect(routes.OtherGoodsInputController.displayCurrencyInput(path, iid, 0)))
      }
    }
  }


  def processCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          Future.successful(BadRequest(currency_input(formWithErrors, product, path, currencyService.getAllCurrencies, iid)))
        }
      },
      currencyDto => {

        purchasedProductService.storeCurrency(context.getJourneyData, path, iid, currencyDto.currency) map { _ =>
          Redirect(routes.OtherGoodsInputController.displayCostInput(path, iid, currencyDto.itemsRemaining))
        }
      }
    )
  }


  def displayCostInput(path: ProductPath, iid: String, itemsRemaining: Int): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, _, _, _, Some(cost))) if workingIid == iid => CostDto.form(optionalItemsRemaining = false).bind(Map("cost" -> cost.toString, "itemsRemaining" -> itemsRemaining.toString)).discardingErrors
        case _ => CostDto.form(optionalItemsRemaining = false).bind(Map("itemsRemaining" -> itemsRemaining.toString)).discardingErrors
      }
    }

    requireWorkingInstanceCurrency { currency =>

      requireProduct(path) { product =>
        Future.successful(Ok(cost_input(form, product, path, iid, currency.displayName)))
      }
    }
  }


  def processCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CostDto.form(optionalItemsRemaining = false).bindFromRequest.fold(
      formWithErrors => {

        requireWorkingInstanceCurrency { currency =>
          requireProduct(path) { product =>
            Future.successful(BadRequest(cost_input(formWithErrors, product, path, iid, currency.displayName)))
          }
        }
      },
      costDto => {

        val itemsRemaining = costDto.itemsRemaining - 1

        requireWorkingInstance { workingInstance =>

          requireProduct(path) { product =>

            val wi = workingInstance.copy(cost = Some(costDto.cost))

            acceptingValidWorkingInstance(Some(wi), product) {
              case Some(updatedJourneyData) =>
                purchasedProductService.cacheJourneyData(updatedJourneyData).map { _ =>
                  if (itemsRemaining > 0) {
                    Redirect(routes.OtherGoodsInputController.displayCountryInput(path, generateIid, itemsRemaining))
                  } else {
                    Redirect(routes.SelectProductController.nextStep())
                  }
                }
              case None =>
                if (itemsRemaining > 0) {
                  Future.successful(Redirect(routes.OtherGoodsInputController.displayCountryInput(path, generateIid, itemsRemaining)))
                } else {
                  Future.successful(Redirect(routes.SelectProductController.nextStep()))
                }
              }
          }
        }
      }
    )
  }
}
