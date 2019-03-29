package controllers

import config.AppConfig
import connectors.Cache
import javax.inject.Inject
import models.{NoOfSticksDto, ProductPath, TobaccoDto}
import play.api.data.Form
import play.api.data.Forms.{mapping, text, _}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import util._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NewTobaccoInputController @Inject()(
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,

  val error_template: views.html.error_template,
  val tobacco_input: views.html.new_tobacco.tobacco_input,
  val weight_or_volume_input: views.html.new_tobacco.weight_or_volume_input,
  val no_of_sticks_input: views.html.new_tobacco.no_of_sticks_input,
  val no_of_sticks_weight_or_volume_input: views.html.new_tobacco.no_of_sticks_weight_or_volume_input,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  def weightOrVolumeNoOfSticksForm(path: ProductPath, limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks" -> text
        .verifying("error.no_of_sticks.required." + path.toMessageKey, noOfSticks => !noOfSticks.isEmpty)
        .verifying("error.invalid.characters.noofsticks." + path.toMessageKey, noOfSticks => noOfSticks.isEmpty || (Try(BigInt(noOfSticks) > 0).getOrElse(false)) )
        .transform[Option[Int]](noOfSticks => Some(Try(noOfSticks.toInt).toOption.getOrElse(Integer.MAX_VALUE)), int => int.mkString)
        .verifying(calculatorLimitConstraintOptionInt(limits, applicableLimits)),
      "weightOrVolume" -> optional(text)
        .verifying("error.weight_or_volume.required."+ path.toMessageKey, weightOrVolume => weightOrVolume.isDefined)
        .verifying("error.invalid.characters.weight", weightOrVolume => !weightOrVolume.isDefined || weightOrVolume.flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0)).getOrElse(false))
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(x)), kilos => kilos.map( x => x.toString ))
        .verifying("error.max.decimal.places.weight", weightOrVolume => weightOrVolume.fold(true)(x => x.scale  <= 2))
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(decimalFormat5.format(x.toDouble/1000))), kilos => kilos.map(x => BigDecimal(decimalFormat5.format(x * 1000)))),
      "country" -> text.verifying("error.country.invalid", name => countriesService.isValidCountryName(name)),
      "currency" -> text
        .transform[String](
          name => currencyService.getCodeByDisplayName(name).getOrElse(name),
          code => currencyService.getDisplayNameByCode(code).getOrElse(code)
        )
        .verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def noOfSticksForm(path: ProductPath, limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks" -> text
        .verifying("error.no_of_sticks.required." + path.toMessageKey, noOfSticks => !noOfSticks.isEmpty)
        .verifying("error.invalid.characters.noofsticks." + path.toMessageKey, noOfSticks => noOfSticks.isEmpty || (Try(BigInt(noOfSticks) > 0).getOrElse(false)) )
        .transform[Option[Int]](noOfSticks => Some(Try(noOfSticks.toInt).toOption.getOrElse(Integer.MAX_VALUE)), int => int.mkString)
        .verifying(calculatorLimitConstraintOptionInt(limits, applicableLimits)),
      "weightOrVolume" -> ignored[Option[BigDecimal]](None),
      "country" -> text.verifying("error.country.invalid", name => countriesService.isValidCountryName(name)),
      "currency" -> text
        .transform[String](
          name => currencyService.getCodeByDisplayName(name).getOrElse(name),
          code => currencyService.getDisplayNameByCode(code).getOrElse(code)
        )
        .verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def weightOrVolumeForm(path: ProductPath, limits: Map[String, BigDecimal] = Map.empty, applicableLimits: List[String] = Nil): Form[TobaccoDto] = Form(
    mapping("noOfSticks" -> ignored[Option[Int]](None),
      "weightOrVolume" -> optional(text)
        .verifying("error.required.weight."+ path.toMessageKey, weightOrVolume => weightOrVolume.isDefined)
        .verifying("error.invalid.characters.weight", weightOrVolume => !weightOrVolume.isDefined || weightOrVolume.flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0)).getOrElse(false))
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(x)), kilos => kilos.map( x => x.toString ))
        .verifying("error.max.decimal.places.weight", weightOrVolume => weightOrVolume.fold(true)(x => x.scale  <= 2))
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(decimalFormat5.format(x.toDouble/1000))), kilos => kilos.map(x => BigDecimal(decimalFormat5.format(x * 1000))))
        .verifying(calculatorLimitConstraintOptionBigDecimal(limits, applicableLimits)),
      "country" -> text.verifying("error.country.invalid", name => countriesService.isValidCountryName(name)),
      "currency" -> text
        .transform[String](
        name => currencyService.getCodeByDisplayName(name).getOrElse(name),
        code => currencyService.getDisplayNameByCode(code).getOrElse(code)
      )
        .verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )
  
  def displayNoOfSticksAddForm(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>
    requireProduct(path) { product =>
      Future.successful(Ok( no_of_sticks_input(noOfSticksForm(path), product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies) ))
    }
  }


  def displayWeightAddForm(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>
    requireProduct(path) { product =>
      Future.successful(Ok( weight_or_volume_input(weightOrVolumeForm(path), product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies) ))
    }
  }



  def displayNoOfSticksWeightAddForm(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>
    requireProduct(path) { product =>
      Future.successful(Ok( no_of_sticks_weight_or_volume_input(weightOrVolumeNoOfSticksForm(path), product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies) ))
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = DashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        TobaccoDto.fromPurchasedProductInstance(ppi).fold(logAndRenderError("Unable to construct dto from PurchasedProductInstance")) { dto =>
          Future.successful {
            product.templateId match {
              case "cigarettes" =>
                Ok(no_of_sticks_input(noOfSticksForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies))
              case "tobacco" =>
                Ok(weight_or_volume_input(weightOrVolumeForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies))
              case _ =>
                Ok(no_of_sticks_weight_or_volume_input(weightOrVolumeNoOfSticksForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies))
            }
          }
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>
    val resilientForm: Form[TobaccoDto] = Form(
      mapping(
        "noOfSticks" -> optional(text).transform[Option[Int]](_.fold(Some(0))(x => Some(Try(x.toInt).getOrElse(Int.MaxValue))), _.map(_.toString)),
        "weightOrVolume" -> optional(text).transform[Option[BigDecimal]](_.map(x => Try(BigDecimal(x)/1000).getOrElse(0)), _.map(_.toString)),
        "country" -> text,
        "currency" -> text,
        "cost" -> optional(text).transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), x => Some(x.toString))
      )(TobaccoDto.apply)(TobaccoDto.unapply)
    )


    val dto = resilientForm.bindFromRequest.value.get
    val jd = newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.currency, List(dto.cost))

    requireLimitUsage(jd) { limits =>
      requireProduct(path) { product =>
        def processNoOfSticksAddForm = {
          noOfSticksForm(path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(no_of_sticks_input(formWithErrors, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies)))
            },
            dto => {
              val jd = newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.currency, List(dto.cost))
              cache.store(jd) map { _ =>
                Redirect(routes.SelectProductController.nextStep())
              }
            }
          )
        }
        def processWeightAddForm = {
          weightOrVolumeForm(path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(weight_or_volume_input(formWithErrors, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies)))
            },
            dto => {
              val jd = newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.currency, List(dto.cost))
              cache.store(jd) map { _ =>
                Redirect(routes.SelectProductController.nextStep())
              }
            }
          )
        }
        def processNoOfSticksWeightAddForm = {
          weightOrVolumeNoOfSticksForm(path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(no_of_sticks_weight_or_volume_input(formWithErrors, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies)))
            },
            dto => {
              val jd = newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.currency, List(dto.cost))
              cache.store(jd) map { _ =>
                Redirect(routes.SelectProductController.nextStep())
              }
            }
          )
        }

        product.templateId match {
          case "cigarettes" =>
            processNoOfSticksAddForm
          case "tobacco" =>
            processWeightAddForm
          case _ =>
            processNoOfSticksWeightAddForm
        }
      }
    }
  }


  def processEditForm(iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    val resilientForm: Form[TobaccoDto] = Form(
      mapping(
        "noOfSticks" -> optional(text).transform[Option[Int]](_.fold(Some(0))(x => Some(Try(x.toInt).getOrElse(Int.MaxValue))), _.map(_.toString)),
        "weightOrVolume" -> optional(text).transform[Option[BigDecimal]](_.map(x => Try(BigDecimal(x)/1000).getOrElse(0)), _.map(_.toString)),
        "country" -> text,
        "currency" -> text,
        "cost" -> optional(text).transform[BigDecimal](_.flatMap(x => Try(BigDecimal(x)).toOption).getOrElse(0), x => Some(x.toString))
      )(TobaccoDto.apply)(TobaccoDto.unapply)
    )

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>

        val dto = resilientForm.bindFromRequest.value.get
        val jd = newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.currency, dto.cost)

        requireLimitUsage(jd) { limits =>

            def processCigarettesEditForm = {
              noOfSticksForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(no_of_sticks_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies)))
                },
                dto => {
                  val jd = newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.currency, dto.cost)
                  cache.store(jd) map { _ =>
                    Redirect(routes.SelectProductController.nextStep())
                  }
                }
              )
            }

            def processTobaccoEditForm = {
              weightOrVolumeForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(weight_or_volume_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies)))
                },
                dto => {
                  val jd = newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.currency, dto.cost)
                  cache.store(jd) map { _ =>
                    Redirect(routes.SelectProductController.nextStep())
                  }
                }
              )
            }

            def processOtherTobaccoEditForm = {
              weightOrVolumeNoOfSticksForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(no_of_sticks_weight_or_volume_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies)))
                },
                dto => {
                  val jd = newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.currency, dto.cost)
                  cache.store(jd) map { _ =>
                    Redirect(routes.SelectProductController.nextStep())
                  }
                }
              )
            }

            product.templateId match {
              case "cigarettes" =>
                processCigarettesEditForm
              case "tobacco" =>
                processTobaccoEditForm
              case _ =>
                processOtherTobaccoEditForm
            }
          }
        }
      }
    }
}
