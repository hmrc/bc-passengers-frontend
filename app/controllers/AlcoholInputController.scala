/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, PublicAction}
import javax.inject.Inject
import models.{AlcoholDto, OtherGoodsDto, ProductPath}
import play.api.data.Forms.{list, mapping, text}
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
      "currency"       -> ignored(""),
      "cost"           -> ignored(BigDecimal(0))
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
      "currency" -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue)
      // TODO Add validations for vat and excise
    )(AlcoholDto.apply)(AlcoholDto.unapply)
  )

  def displayAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireProduct(path) { product =>
      withDefaults(context.getJourneyData) { defaultCountry => defaultCurrency =>
        Future.successful(Ok(alcohol_input(alcoholForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies)))
      }
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        AlcoholDto.fromPurchasedProductInstance(ppi) match {
          case Some(dto) => Future.successful( Ok( alcohol_input(alcoholForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies) ) )
          case None => logAndRenderError("Unable to construct dto from PurchasedProductInstance")
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireLimitUsage({
      val dto = resilientForm.bindFromRequest.value.get
      newPurchaseService.insertPurchases(path, Some(dto.weightOrVolume), None, dto.country, dto.currency, List(dto.cost))
    }) { limits =>
      requireProduct(path) { product =>
        alcoholForm(path, limits, product.applicableLimits).bindFromRequest.fold(
          formWithErrors => {
            Future.successful(BadRequest(alcohol_input(formWithErrors, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies)))
          },
          dto => {
            cache.store( newPurchaseService.insertPurchases(path, Some(dto.weightOrVolume), None, dto.country, dto.currency, List(dto.cost), isVatPaid = Some(true), isExcisePaid = Some(false)) ) map { _ =>
              Redirect(routes.SelectProductController.nextStep())
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
          newPurchaseService.updatePurchase(ppi.path, iid, Some(dto.weightOrVolume), None, dto.country, dto.currency, dto.cost)
        }) { limits =>
          alcoholForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(alcohol_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies)))
            },
            dto => {
              cache.store( newPurchaseService.updatePurchase(ppi.path, iid, Some(dto.weightOrVolume), None, dto.country, dto.currency, dto.cost) ) map { _ =>
                Redirect(routes.SelectProductController.nextStep())
              }
            }
          )
        }
      }
    }
  }
}
