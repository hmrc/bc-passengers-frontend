/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, PublicAction}
import javax.inject.Inject
import models.{AlcoholDto, ProductPath}
import play.api.data.Forms.{mapping, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import util._
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AlcoholInputController @Inject()(
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,

  publicAction: PublicAction,
  dashboardAction: DashboardAction,

  val error_template: views.html.error_template,
  val alcohol_input: views.html.alcohol.alcohol_input,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  val resilientForm: Form[AlcoholDto] = Form(
    mapping(
      "weightOrVolume" -> optional(text).transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), _ => None),
      "country"        -> ignored(""),
      "originCountry"  -> optional(text),
      "currency"       -> ignored(""),
      "cost"           -> ignored(BigDecimal(0)),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid" -> optional(boolean)
    )(AlcoholDto.apply)(AlcoholDto.unapply)
  )

  def alcoholForm(path: ProductPath, limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Form[AlcoholDto] = Form(
    mapping(
      "weightOrVolume" -> optional(text)
        .verifying("error.required.volume."+ path.toMessageKey, _.isDefined)
        .verifying("error.invalid.characters.volume", x => !x.isDefined || x.flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0)).getOrElse(false))
        .transform[BigDecimal](_.fold(BigDecimal(0))(x => BigDecimal(x)), x => Some(x.toString) )
        .verifying("error.max.decimal.places.volume", _.scale  <= 3).transform[BigDecimal](identity, identity)
        .verifying(calculatorLimitConstraintBigDecimal(limits, applicableLimits)),
      "country" -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry" -> optional(text),
      "currency" -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid" -> optional(boolean),
      "isExcisePaid" -> optional(boolean),
      "isCustomPaid" -> optional(boolean)
    )(AlcoholDto.apply)(AlcoholDto.unapply)
  )

  def displayAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireProduct(path) { product =>
      withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
        Future.successful(Ok(alcohol_input(alcoholForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "originCountry" -> defaultOriginCountry.getOrElse(""), "currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
      }
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        AlcoholDto.fromPurchasedProductInstance(ppi) match {
          case Some(dto) => Future.successful( Ok( alcohol_input(alcoholForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck) ) )
          case None => logAndRenderError("Unable to construct dto from PurchasedProductInstance")
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireLimitUsage({
      val dto = resilientForm.bindFromRequest.value.get
      newPurchaseService.insertPurchases(path, Some(dto.weightOrVolume), None, dto.country, dto.originCountry, dto.currency, List(dto.cost))._1
    }) { limits =>
      requireProduct(path) { product =>
        alcoholForm(path, limits, product.applicableLimits).bindFromRequest.fold(
          formWithErrors => {
            Future.successful(BadRequest(alcohol_input(formWithErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
          },
          dto => {
            val item = newPurchaseService.insertPurchases(path, Some(dto.weightOrVolume), None, dto.country, dto.originCountry, dto.currency, List(dto.cost))
            cache.store(item._1) map { _ =>
              (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path,item._2))
                case _ => Redirect(routes.SelectProductController.nextStep())
              }
            }
          }
        )
      }
    }
  }

  def processEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>

        requireLimitUsage({
          val dto = resilientForm.bindFromRequest.value.get
          newPurchaseService.updatePurchase(ppi.path, iid, Some(dto.weightOrVolume), None, dto.country, dto.originCountry, dto.currency, dto.cost)
        }) { limits =>
          alcoholForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(alcohol_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
            },
            dto => {
              cache.store( newPurchaseService.updatePurchase(ppi.path, iid, Some(dto.weightOrVolume), None, dto.country, dto.originCountry, dto.currency, dto.cost) ) map { _ =>
                (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                  case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path,iid))
                  case _ => Redirect(routes.SelectProductController.nextStep())
                }
              }
            }
          )
        }
      }
    }
  }
}
