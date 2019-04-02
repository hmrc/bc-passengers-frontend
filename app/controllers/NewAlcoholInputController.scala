package controllers

import config.AppConfig
import connectors.Cache
import javax.inject.Inject
import models.{AlcoholDto, OtherGoodsDto, ProductPath}
import play.api.data.Form
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

class NewAlcoholInputController @Inject() (
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,

  val error_template: views.html.error_template,
  val alcohol_input: views.html.new_alcohol.alcohol_input,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

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

    )(AlcoholDto.apply)(AlcoholDto.unapply)
  )

  def displayAddForm(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>
    requireProduct(path) { product =>
      Future.successful(Ok( alcohol_input(alcoholForm(path), product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies) ))
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        AlcoholDto.fromPurchasedProductInstance(ppi) match {
          case Some(dto) => Future.successful( Ok( alcohol_input(alcoholForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies) ) )
          case None => logAndRenderError("Unable to construct dto from PurchasedProductInstance")
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    val resilientForm: Form[AlcoholDto] = Form(
      mapping(
        "weightOrVolume" -> optional(text).transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), _ => None),
        "country"        -> optional(text).transform[String](_.mkString, _ => None),
        "currency"       -> optional(text).transform[String](_.mkString, _ => None),
        "cost"           -> optional(text).transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), x => Some(x.toString))
      )(AlcoholDto.apply)(AlcoholDto.unapply)
    )

    val dto = resilientForm.bindFromRequest.value.get
    val jd = newPurchaseService.insertPurchases(path, Some(dto.weightOrVolume), None, dto.country, dto.currency, List(dto.cost))

    requireLimitUsage(jd) { limits =>
      requireProduct(path) { product =>
        alcoholForm(path, limits, product.applicableLimits).bindFromRequest.fold(
          formWithErrors => {
            Future.successful(BadRequest(alcohol_input(formWithErrors, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies)))
          },
          _ => {
            cache.store(jd) map { _ =>
              Redirect(routes.SelectProductController.nextStep())
            }
          }
        )
      }
    }
  }

  def processEditForm(iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val resilientForm: Form[AlcoholDto] = Form(
      mapping(
        "weightOrVolume" -> optional(text).transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), _ => None),
        "country"        -> optional(text).transform[String](_.mkString, _ => None),
        "currency"       -> optional(text).transform[String](_.mkString, _ => None),
        "cost"           -> optional(text).transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), x => Some(x.toString))
      )(AlcoholDto.apply)(AlcoholDto.unapply)
    )

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>

        val dto = resilientForm.bindFromRequest.value.get
        val jd = newPurchaseService.updatePurchase(ppi.path, iid, Some(dto.weightOrVolume), None, dto.country, dto.currency, dto.cost)

        requireLimitUsage(jd) { limits =>
          alcoholForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(alcohol_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies)))
            },
            _ => {
              cache.store(jd) map { _ =>
                Redirect(routes.SelectProductController.nextStep())
              }
            }
          )
        }
      }
    }
  }
}
