/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.{DashboardAction, PublicAction}
import javax.inject.Inject
import models.{ProductPath, TobaccoDto}
import play.api.data.Form
import play.api.data.Forms.{mapping, text, _}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import util._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TobaccoInputController @Inject()(
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,

  publicAction: PublicAction,
  dashboardAction: DashboardAction,

  val error_template: views.html.error_template,
  val tobacco_input: views.html.tobacco.tobacco_input,
  val weight_or_volume_input: views.html.tobacco.weight_or_volume_input,
  val no_of_sticks_input: views.html.tobacco.no_of_sticks_input,
  val no_of_sticks_weight_or_volume_input: views.html.tobacco.no_of_sticks_weight_or_volume_input,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  val resilientForm: Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks" -> optional(text).transform[Option[Int]](_.fold(Some(0))(x => Some(Try(x.toInt).getOrElse(Int.MaxValue))), _.map(_.toString)),
      "weightOrVolume" -> optional(text).transform[Option[BigDecimal]](_.map(x => Try(BigDecimal(x)/1000).getOrElse(0)), _.map(_.toString)),
      "country" -> ignored(""),
      "originCountry" -> optional(text),
      "currency" -> ignored(""),
      "cost" -> ignored(BigDecimal(0)),
      "isVatPaid" -> optional(boolean),
      "isExcisePaid" -> optional(boolean),
      "isCustomPaid" -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

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
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )
  
  def displayNoOfSticksAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireProduct(path) { product =>
      withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
        Future.successful(Ok( no_of_sticks_input(noOfSticksForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "originCountry" -> defaultOriginCountry.getOrElse(""),"currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck) ))
      }
    }
  }


  def displayWeightAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireProduct(path) { product =>
      withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(Ok(weight_or_volume_input(weightOrVolumeForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "originCountry" -> defaultOriginCountry.getOrElse(""), "currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
      }
    }
  }

  def displayNoOfSticksWeightAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    requireProduct(path) { product =>
      withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(Ok(no_of_sticks_weight_or_volume_input(weightOrVolumeNoOfSticksForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "originCountry" -> defaultOriginCountry.getOrElse(""), "currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
      }
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        TobaccoDto.fromPurchasedProductInstance(ppi).fold(logAndRenderError("Unable to construct dto from PurchasedProductInstance")) { dto =>
          Future.successful {
            product.templateId match {
              case "cigarettes" =>
                Ok(no_of_sticks_input(noOfSticksForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck))
              case "tobacco" =>
                Ok(weight_or_volume_input(weightOrVolumeForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck))
              case _ =>
                Ok(no_of_sticks_weight_or_volume_input(weightOrVolumeNoOfSticksForm(ppi.path).fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck))
            }
          }
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>

    requireLimitUsage({
      val dto = resilientForm.bindFromRequest.value.get
      newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, List(dto.cost))._1
    }) { limits =>
      requireProduct(path) { product =>
        def processNoOfSticksAddForm = {
          noOfSticksForm(path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(no_of_sticks_input(formWithErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
            },
            dto => {
              val item =  newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, List(dto.cost))
              cache.store( item._1 ) map { _ =>
                (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                  case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path,item._2))
                  case _ => Redirect(routes.SelectProductController.nextStep())
                }
              }
            }
          )
        }
        def processWeightAddForm = {
          weightOrVolumeForm(path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(weight_or_volume_input(formWithErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
            },
            dto => {
              val item = newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, List(dto.cost))
                cache.store( item._1 ) map { _ =>
                  (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                    case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path,item._2))
                    case _ => Redirect(routes.SelectProductController.nextStep())
                  }
              }
            }
          )
        }
        def processNoOfSticksWeightAddForm = {
          weightOrVolumeNoOfSticksForm(path, limits, product.applicableLimits).bindFromRequest.fold(
            formWithErrors => {
              Future.successful(BadRequest(no_of_sticks_weight_or_volume_input(formWithErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
            },
            dto => {
              val item = newPurchaseService.insertPurchases(path, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, List(dto.cost))
              cache.store( item._1 ) map { _ =>
                (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                  case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path,item._2))
                  case _ => Redirect(routes.SelectProductController.nextStep())
                }
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


  def processEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>

        requireLimitUsage({
          val dto = resilientForm.bindFromRequest.value.get
          newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, dto.cost)
        }) { limits =>

            def processCigarettesEditForm = {
              noOfSticksForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(no_of_sticks_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
                },
                dto => {
                  cache.store( newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, dto.cost) ) map { _ =>
                    (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                      case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path,iid))
                      case _ => Redirect(routes.SelectProductController.nextStep())
                    }
                  }
                }
              )
            }

            def processTobaccoEditForm = {
              weightOrVolumeForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(weight_or_volume_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
                },
                dto => {
                  cache.store( newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, dto.cost) ) map { _ =>
                    (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                      case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path,iid))
                      case _ => Redirect(routes.SelectProductController.nextStep())
                    }
                  }
                }
              )
            }

            def processOtherTobaccoEditForm = {
              weightOrVolumeNoOfSticksForm(ppi.path, limits, product.applicableLimits).bindFromRequest.fold(
                formWithErrors => {
                  Future.successful(BadRequest(no_of_sticks_weight_or_volume_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
                },
                dto => {
                  cache.store( newPurchaseService.updatePurchase(ppi.path, iid, dto.weightOrVolume, dto.noOfSticks, dto.country, dto.originCountry, dto.currency, dto.cost) ) map { _ =>
                    (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                      case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path,iid))
                      case _ => Redirect(routes.SelectProductController.nextStep())
                    }
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
