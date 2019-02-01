package controllers

import config.AppConfig
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AlcoholInputController @Inject() (
  val countriesService: CountriesService,
  val calculatorService: CalculatorService,
  val travelDetailsService: TravelDetailsService,
  val purchasedProductService: PurchasedProductService,
  val currencyService: CurrencyService,
  val productTreeService: ProductTreeService,
  val volume_input: views.html.alcohol.volume_input,
  val country_of_purchase: views.html.alcohol.country_of_purchase,
  val currency_input: views.html.alcohol.currency_input,
  val cost_input: views.html.alcohol.cost_input,
  val error_template: views.html.error_template,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit override val messagesApi: MessagesApi,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {


  def startInputJourney(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    Future.successful(Redirect(routes.AlcoholInputController.displayVolumeInput(path, generateIid)))
  }

  def displayVolumeInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, Some(volume), _,  _, _, _)) if workingIid == iid =>
          VolumeDto.form().fill(VolumeDto(volume))
        case _ => VolumeDto.form()
      }
    }
    requireProduct(path) { product =>
      Future.successful(Ok(volume_input(form, product.name, product.token, path, iid)))
    }
  }

  def displayVolumeUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) flatMap { _ =>
        Future.successful(Redirect(routes.AlcoholInputController.displayVolumeInput(path, iid)))
      }
    }
  }

  def processVolumeInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val journeyData = addingWeightOrVolumeToWorkingInstance(context.getJourneyData, path, iid, VolumeDto.form().bindFromRequest.value.map(_.volume))

    requireLimitUsage(journeyData) { limits =>

      requireProduct(path) { product =>

        VolumeDto.form(limits, product.applicableLimits).bindFromRequest.fold(
          formWithErrors => {
            requireProduct(path) { product =>
              Future.successful(BadRequest(volume_input(formWithErrors, product.name, product.token, path, iid)))
            }
          },
          _ => {

            acceptingValidWorkingInstance(journeyData.workingInstance, product) {
              case Some(updatedJourneyData) =>
                purchasedProductService.cacheJourneyData(updatedJourneyData).map { _ =>
                  Redirect(routes.DashboardController.showDashboard())
                }
              case None =>
                purchasedProductService.cacheJourneyData(journeyData).map { _ =>
                  Redirect(routes.AlcoholInputController.displayCountryInput(path, iid))
                }
            }

          }
        )
      }
    }
  }

  def displayCountryInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, _,  Some(country), _, _)) if workingIid == iid => SelectedCountryDto.form(countriesService).fill(SelectedCountryDto(country.countryName, 0))
        case _ => SelectedCountryDto.form(countriesService)
      }
    }

    requireProduct(path) { product =>
      requireWorkingInstanceWeightOrVolume { volume =>
        Future.successful(Ok(country_of_purchase(form, product, path, iid, countriesService.getAllCountries, volume)))
      }
    }
  }

  def displayCountryUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) map { _ =>
        Redirect(routes.AlcoholInputController.displayCountryInput(path, iid))
      }
    }
  }

  def processCountryInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    SelectedCountryDto.form(countriesService).bindFromRequest.fold(
      formWithErrors => {
        requireProduct(path) { product =>
          requireWorkingInstanceWeightOrVolume { volume =>
            Future.successful(BadRequest(country_of_purchase(formWithErrors, product, path, iid, countriesService.getAllCountries, volume)))
          }
        }
      },
      selectedCountryDto => {
        requireCountryByName(selectedCountryDto.country) { country =>
          context.getJourneyData.workingInstance match {
            case Some(PurchasedProductInstance(_, workingIid, Some(_), None, Some(_), Some(_), Some(_))) if workingIid == iid  => purchasedProductService.updateCountry(context.getJourneyData, path, iid, country) map { _ =>
              Redirect(routes.DashboardController.showDashboard())
            }
            case _ =>
              purchasedProductService.storeCountry(context.getJourneyData, path, iid, country) map { _ =>
                Redirect(routes.AlcoholInputController.displayCurrencyInput(path, iid))
              }
          }
        }
      }
    )
  }

  def displayCurrencyUpdate(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(path, iid) { product =>
      purchasedProductService.makeWorkingInstance(context.getJourneyData, product) map { _ =>
        Redirect(routes.AlcoholInputController.displayCurrencyInput(path, iid))
      }
    }
  }


  def displayCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val form = {
      context.getJourneyData.workingInstance match {
        case Some(PurchasedProductInstance(_, workingIid, _, _, _, Some(currency), _)) if workingIid == iid => CurrencyDto.form(currencyService).fill(CurrencyDto(currency, 0))
        case _ => CurrencyDto.form(currencyService)
      }
    }

    requireWorkingInstanceWeightOrVolume { volume =>
      requireProduct(path) { product =>
        Future.successful(Ok(currency_input(form, product, path, iid, currencyService.getAllCurrencies, volume)))
      }
    }
  }

  def processCurrencyInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CurrencyDto.form(currencyService).bindFromRequest.fold(
      formWithErrors => {
        requireWorkingInstanceWeightOrVolume { volume =>
          requireProduct(path) { product =>
            Future.successful(BadRequest(currency_input(formWithErrors, product, path, iid, currencyService.getAllCurrencies, volume)))
          }
        }
      },
      dto => {
        purchasedProductService.storeCurrency(context.getJourneyData, path, iid, dto.currency) map { _ =>
          Redirect(routes.AlcoholInputController.displayCostInput(path, iid))
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

    requireWorkingInstanceWeightOrVolume { volume =>
      requireWorkingInstanceCurrency { currency: Currency =>
        requireProduct(path) { product =>
          Future.successful(Ok(cost_input(form, product, path, iid, volume, currency.displayName)))
        }
      }
    }
  }

  def processCostInput(path: ProductPath, iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    CostDto.form().bindFromRequest.fold(
      formWithErrors => {
        requireWorkingInstanceWeightOrVolume { volume =>
          requireWorkingInstanceCurrency { currency: Currency =>
            requireProduct(path) { product =>
              Future.successful(BadRequest(cost_input(formWithErrors, product, path, iid, volume, currency.displayName)))
            }
          }
        }
      },
      dto => {

        requireWorkingInstance { workingInstance =>

          requireProduct(path) { product =>

            val wi = workingInstance.copy(cost = Some(dto.cost))

            acceptingValidWorkingInstance(Some(wi), product) {
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
