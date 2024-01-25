/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.ControllerHelpers
import controllers.enforce.DashboardAction
import models.{JourneyData, ProductPath, TobaccoDto}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

// scalastyle:off
class TobaccoInputController @Inject() (
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,
  val backLinkModel: BackLinkModel,
  dashboardAction: DashboardAction,
  val errorTemplate: views.html.errorTemplate,
  val tobacco_input: views.html.tobacco.tobacco_input,
  val weight_or_volume_input: views.html.tobacco.weight_or_volume_input,
  val no_of_sticks_input: views.html.tobacco.no_of_sticks_input,
  val no_of_sticks_weight_or_volume_input: views.html.tobacco.no_of_sticks_weight_or_volume_input,
  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  val resilientForm: Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks"     -> optional(text)
        .transform[Option[Int]](_.fold(Some(0))(x => Some(Try(x.toInt).getOrElse(Int.MaxValue))), _.map(_.toString)),
      "weightOrVolume" -> optional(text)
        .transform[Option[BigDecimal]](_.map(x => Try(BigDecimal(x) / 1000).getOrElse(0)), _.map(_.toString)),
      "country"        -> ignored(""),
      "originCountry"  -> optional(text),
      "currency"       -> ignored(""),
      "cost"           -> ignored(BigDecimal(0)),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def cigarAndCigarilloForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks"     -> text
        .verifying("error.no_of_sticks.required." + path.toMessageKey, noOfSticks => noOfSticks.nonEmpty)
        .verifying(
          "error.invalid.characters.noofsticks." + path.toMessageKey,
          noOfSticks => noOfSticks.isEmpty || Try(BigInt(noOfSticks) > 0).getOrElse(false)
        )
        .transform[Option[Int]](
          noOfSticks => Some(Try(noOfSticks.toInt).toOption.getOrElse(Integer.MAX_VALUE)),
          int => int.mkString
        ),
      "weightOrVolume" -> optional(text)
        .verifying("error.weight_or_volume.required." + path.toMessageKey, weightOrVolume => weightOrVolume.isDefined)
        .verifying(
          "error.invalid.characters.weight",
          weightOrVolume =>
            weightOrVolume.isEmpty || weightOrVolume
              .flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0))
              .getOrElse(false)
        )
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(x)), kilos => kilos.map(x => x.toString))
        .verifying("error.max.decimal.places.weight", weightOrVolume => weightOrVolume.fold(true)(x => x.scale <= 2))
        .transform[Option[BigDecimal]](
          grams => grams.map(x => (x / 1000).setScale(2, BigDecimal.RoundingMode.HALF_UP)),
          kilos => kilos.map(x => (x * 1000).setScale(2, BigDecimal.RoundingMode.HALF_UP))
        ),
      "country"        -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry"  -> optional(text),
      "currency"       -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost"           -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def cigaretteAndHeatedTobaccoForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks"     -> text
        .verifying("error.no_of_sticks.required." + path.toMessageKey, noOfSticks => noOfSticks.nonEmpty)
        .verifying(
          "error.invalid.characters.noofsticks." + path.toMessageKey,
          noOfSticks => noOfSticks.isEmpty || Try(BigInt(noOfSticks) > 0).getOrElse(false)
        )
        .transform[Option[Int]](
          noOfSticks => Some(Try(noOfSticks.toInt).toOption.getOrElse(Integer.MAX_VALUE)),
          int => int.mkString
        ),
      "weightOrVolume" -> ignored[Option[BigDecimal]](None),
      "country"        -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry"  -> optional(text),
      "currency"       -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost"           -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def looseTobaccoWeightForm(path: ProductPath): Form[TobaccoDto] = Form(
    mapping(
      "noOfSticks"     -> ignored[Option[Int]](None),
      "weightOrVolume" -> optional(text)
        .verifying("error.required.weight." + path.toMessageKey, weightOrVolume => weightOrVolume.isDefined)
        .verifying(
          "error.invalid.characters.weight",
          weightOrVolume =>
            weightOrVolume.isEmpty || weightOrVolume
              .flatMap(x => Try(BigDecimal(x)).toOption.map(d => d > 0.0))
              .getOrElse(false)
        )
        .transform[Option[BigDecimal]](grams => grams.map(x => BigDecimal(x)), kilos => kilos.map(x => x.toString))
        .verifying("error.max.decimal.places.weight", weightOrVolume => weightOrVolume.fold(true)(x => x.scale <= 2))
        .transform[Option[BigDecimal]](
          grams => grams.map(x => (x / 1000)),
          kilos => kilos.map(x => (x * 1000).setScale(2, BigDecimal.RoundingMode.HALF_UP))
        ),
      "country"        -> text.verifying("error.country.invalid", code => countriesService.isValidCountryCode(code)),
      "originCountry"  -> optional(text),
      "currency"       -> text.verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost"           -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid"      -> optional(boolean),
      "isExcisePaid"   -> optional(boolean),
      "isCustomPaid"   -> optional(boolean),
      "hasEvidence"    -> optional(boolean)
    )(TobaccoDto.apply)(TobaccoDto.unapply)
  )

  def displayCigaretteAndHeatedTobaccoForm(path: ProductPath): Action[AnyContent] = dashboardAction {
    implicit context =>
      if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
        Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
      } else {
        requireProduct(path) { product =>
          withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
            Future.successful(
              Ok(
                no_of_sticks_input(
                  cigaretteAndHeatedTobaccoForm(path)
                    .bind(
                      Map(
                        "country"       -> defaultCountry.getOrElse(""),
                        "originCountry" -> defaultOriginCountry.getOrElse(""),
                        "currency"      -> defaultCurrency.getOrElse("")
                      )
                    )
                    .discardingErrors,
                  backLinkModel.backLink,
                  customBackLink = false,
                  product,
                  path,
                  None,
                  countriesService.getAllCountries,
                  countriesService.getAllCountriesAndEu,
                  currencyService.getAllCurrencies,
                  context.getJourneyData.euCountryCheck
                )
              )
            )
          }
        }
      }
  }

  def displayLooseTobaccoForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(
            Ok(
              weight_or_volume_input(
                looseTobaccoWeightForm(path)
                  .bind(
                    Map(
                      "country"       -> defaultCountry.getOrElse(""),
                      "originCountry" -> defaultOriginCountry.getOrElse(""),
                      "currency"      -> defaultCurrency.getOrElse("")
                    )
                  )
                  .discardingErrors,
                backLinkModel.backLink,
                customBackLink = false,
                product,
                path,
                None,
                countriesService.getAllCountries,
                countriesService.getAllCountriesAndEu,
                currencyService.getAllCurrencies,
                context.getJourneyData.euCountryCheck
              )
            )
          )
        }
      }
    }
  }

  def displayCigarAndCigarilloForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      requireProduct(path) { product =>
        withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(
            Ok(
              no_of_sticks_weight_or_volume_input(
                cigarAndCigarilloForm(path)
                  .bind(
                    Map(
                      "country"       -> defaultCountry.getOrElse(""),
                      "originCountry" -> defaultOriginCountry.getOrElse(""),
                      "currency"      -> defaultCurrency.getOrElse("")
                    )
                  )
                  .discardingErrors,
                backLinkModel.backLink,
                customBackLink = false,
                product,
                path,
                None,
                countriesService.getAllCountries,
                countriesService.getAllCountriesAndEu,
                currencyService.getAllCurrencies,
                context.getJourneyData.euCountryCheck
              )
            )
          )
        }
      }
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        TobaccoDto
          .fromPurchasedProductInstance(ppi)
          .fold(logAndRenderError("Unable to construct dto from PurchasedProductInstance")) { dto =>
            Future.successful {
              product.templateId match {
                case "cigarettes" =>
                  Ok(
                    no_of_sticks_input(
                      cigaretteAndHeatedTobaccoForm(ppi.path).fill(dto),
                      backLinkModel.backLink,
                      customBackLink = true,
                      product,
                      ppi.path,
                      Some(iid),
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                case "tobacco"    =>
                  Ok(
                    weight_or_volume_input(
                      looseTobaccoWeightForm(ppi.path).fill(dto),
                      backLinkModel.backLink,
                      customBackLink = true,
                      product,
                      ppi.path,
                      Some(iid),
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                case _            =>
                  Ok(
                    no_of_sticks_weight_or_volume_input(
                      cigarAndCigarilloForm(ppi.path).fill(dto),
                      backLinkModel.backLink,
                      customBackLink = true,
                      product,
                      ppi.path,
                      Some(iid),
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
              }
            }
          }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>
    val dto              = resilientForm.bindFromRequest().value.get
    val (journeyData, _) =
      newPurchaseService.insertPurchases(
        path,
        dto.weightOrVolume,
        dto.noOfSticks,
        dto.country,
        dto.originCountry,
        dto.currency,
        List(dto.cost)
      )

    requireLimitUsage(journeyData) { limits =>
      requireProduct(path) { product =>
        def processNoOfSticksAddForm =
          cigaretteAndHeatedTobaccoForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    no_of_sticks_input(
                      formWithErrors,
                      backLinkModel.backLink,
                      customBackLink = false,
                      product,
                      path,
                      None,
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                ),
              dto => {

                val totalNoOfSticksForItemType: Future[Int] = for {
                  data: Option[JourneyData]       <- cache.fetch
                  previousTotalWeightOrVolume: Int =
                    calculatorService.calculateTotalNumberOfSticksForItemType(data, product.token)
                  totalNoOfSticks: Int             =
                    previousTotalWeightOrVolume +
                      dto.noOfSticks.getOrElse(0)

                } yield totalNoOfSticks

                totalNoOfSticksForItemType.flatMap { noOfSticks =>
                  if (cigaretteAndHeatedTobaccoConstraint(noOfSticks)) {
                    val (journeyData: JourneyData, itemId: String) =
                      newPurchaseService.insertPurchases(
                        path = path,
                        weightOrVolume = dto.weightOrVolume,
                        noOfSticks = dto.noOfSticks,
                        countryCode = dto.country,
                        originCountryCode = dto.originCountry,
                        currency = dto.currency,
                        costs = List(dto.cost)
                      )

                    cache.store(journeyData) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) =>
                          Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path, itemId))
                        case (Some(false), Some("euOnly"))
                            if countriesService.isInEu(dto.originCountry.getOrElse("")) =>
                          Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, itemId))
                        case _                                  => Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else {
                    cache.fetch.map { data =>
                      Redirect(
                        routes.LimitExceedController.loadLimitExceedPage(path)
                      )
                        .removingFromSession(s"user-amount-input-${product.token}")
                        .addingToSession(s"user-amount-input-${product.token}" -> noOfSticks.toString)
                    }
                  }
                }
              }
            )

        def processWeightAddForm =
          looseTobaccoWeightForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    weight_or_volume_input(
                      form = formWithErrors,
                      backLink = backLinkModel.backLink,
                      customBackLink = false,
                      product = product,
                      path = path,
                      iid = None,
                      countries = countriesService.getAllCountries,
                      countriesEU = countriesService.getAllCountriesAndEu,
                      currencies = currencyService.getAllCurrencies,
                      journeyStart = context.getJourneyData.euCountryCheck
                    )
                  )
                ),
              success = dto => {
                val totalWeightForLooseTobacco: Future[BigDecimal] =
                  for {
                    data: Option[JourneyData]            <- cache.fetch
                    chewingTobaccoTotalWeight: BigDecimal =
                      calculatorService.calculateTotalWeightOrVolumeForItemType(data, "chewing-tobacco")
                    rollingTobaccoTotalWeight: BigDecimal =
                      calculatorService.calculateTotalWeightOrVolumeForItemType(data, "rolling-tobacco")
//                    looseTobaccoTotalWeightInGrams: BigDecimal =
//                      (dto.weightOrVolume
//                        .getOrElse(BigDecimal(0)) + chewingTobaccoTotalWeight + rollingTobaccoTotalWeight)
//                        .setScale(5, BigDecimal.RoundingMode.HALF_UP) * 1000
                  } yield {
                    val looseTobaccoTotalWeightInGrams: BigDecimal =
                      (dto.weightOrVolume
                        .getOrElse(BigDecimal(0)) +
                        chewingTobaccoTotalWeight + rollingTobaccoTotalWeight)
                        .setScale(5, BigDecimal.RoundingMode.HALF_UP) * 1000
                    looseTobaccoTotalWeightInGrams
                  }

                totalWeightForLooseTobacco.flatMap { weightInGrams =>
                  if (looseTobaccoWeightConstraint(weightInGrams)) {
                    val (journeyData: JourneyData, itemId: String) =
                      newPurchaseService.insertPurchases(
                        path,
                        dto.weightOrVolume,
                        dto.noOfSticks,
                        dto.country,
                        dto.originCountry,
                        dto.currency,
                        List(dto.cost)
                      )
                    cache.store(journeyData) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) =>
                          Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path, itemId))
                        case (Some(false), Some("euOnly"))
                            if countriesService.isInEu(dto.originCountry.getOrElse("")) =>
                          Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, itemId))
                        case _                                  => Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else {
                    Future(
                      Redirect(routes.LimitExceedController.loadLimitExceedPage(path))
                        .removingFromSession(s"user-amount-input-${product.token}")
                        .addingToSession(
                          s"user-amount-input-${product.token}" -> weightInGrams
                            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
                            .toString()
                        )
                    )
                  }
                }
              }
            )

        def processNoOfSticksWeightAddForm =
          cigarAndCigarilloForm(path)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    no_of_sticks_weight_or_volume_input(
                      formWithErrors,
                      backLinkModel.backLink,
                      customBackLink = false,
                      product,
                      path,
                      None,
                      countriesService.getAllCountries,
                      countriesService.getAllCountriesAndEu,
                      currencyService.getAllCurrencies,
                      context.getJourneyData.euCountryCheck
                    )
                  )
                ),
              dto => {
                val totalNoOfSticksForItemType: Future[Int] = for {
                  data: Option[JourneyData]       <- cache.fetch
                  previousTotalWeightOrVolume: Int =
                    calculatorService.calculateTotalNumberOfSticksForItemType(data, product.token)
                  totalNoOfSticks: Int             =
                    previousTotalWeightOrVolume +
                      dto.noOfSticks.getOrElse(0)
                } yield totalNoOfSticks

                totalNoOfSticksForItemType.flatMap { noOfSticks =>
                  if (cigarAndCigarilloConstraint(noOfSticks, product.token)) {
                    val (journeyData: JourneyData, itemId: String) =
                      newPurchaseService.insertPurchases(
                        path,
                        dto.weightOrVolume,
                        dto.noOfSticks,
                        dto.country,
                        dto.originCountry,
                        dto.currency,
                        List(dto.cost)
                      )

                    cache.store(journeyData) map { _ =>
                      (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                        case (Some(true), Some("greatBritain")) =>
                          Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path, itemId))
                        case (Some(false), Some("euOnly"))
                            if countriesService.isInEu(dto.originCountry.getOrElse("")) =>
                          Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, itemId))
                        case _                                  =>
                          Redirect(routes.SelectProductController.nextStep)
                      }
                    }
                  } else {
                    cache.fetch.map { data =>
                      Redirect(
                        routes.LimitExceedController.loadLimitExceedPage(path)
                      )
                        .removingFromSession(s"user-amount-input-${product.token}")
                        .addingToSession(s"user-amount-input-${product.token}" -> noOfSticks.toString)
                    }
                  }
                }
              }
            )

        product.templateId match {
          case "cigarettes" =>
            processNoOfSticksAddForm
          case "tobacco"    =>
            processWeightAddForm
          case _            =>
            processNoOfSticksWeightAddForm
        }
      }
    }
  }

  def processEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        requireLimitUsage {
          val dto = resilientForm.bindFromRequest().value.get
          newPurchaseService.updatePurchase(
            ppi.path,
            iid,
            dto.weightOrVolume,
            dto.noOfSticks,
            dto.country,
            dto.originCountry,
            dto.currency,
            dto.cost
          )
        } { limits =>
          def processCigarettesEditForm: Future[Result] =
            cigaretteAndHeatedTobaccoForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      no_of_sticks_input(
                        formWithErrors,
                        backLinkModel.backLink,
                        customBackLink = true,
                        product,
                        ppi.path,
                        Some(iid),
                        countriesService.getAllCountries,
                        countriesService.getAllCountriesAndEu,
                        currencyService.getAllCurrencies,
                        context.getJourneyData.euCountryCheck
                      )
                    )
                  ),
                dto => {
                  val totalNoOfSticksForItemType: Future[Int] = for {
                    data: Option[JourneyData]       <- cache.fetch
                    previousTotalWeightOrVolume: Int =
                      calculatorService.calculateTotalNumberOfSticksForItemType(data, product.token)
                    totalNoOfSticks: Int             =
                      previousTotalWeightOrVolume +
                        dto.noOfSticks.getOrElse(0)
                  } yield totalNoOfSticks

                  totalNoOfSticksForItemType.flatMap { noOfSticks =>
                    if (cigaretteAndHeatedTobaccoConstraint(noOfSticks)) {
                      cache.store(
                        newPurchaseService.updatePurchase(
                          ppi.path,
                          iid,
                          dto.weightOrVolume,
                          dto.noOfSticks,
                          dto.country,
                          dto.originCountry,
                          dto.currency,
                          dto.cost
                        )
                      ) map { _ =>
                        (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                          case (Some(true), Some("greatBritain")) =>
                            Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                          case (Some(false), Some("euOnly"))      =>
                            if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                              Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                            } else {
                              Redirect(routes.SelectProductController.nextStep)
                            }
                          case _                                  => Redirect(routes.SelectProductController.nextStep)
                        }
                      }
                    } else {
                      cache.fetch.map { data =>
                        Redirect(
                          routes.LimitExceedController.loadLimitExceedPage(ppi.path)
                        )
                          .removingFromSession(s"user-amount-input-${product.token}")
                          .addingToSession(s"user-amount-input-${product.token}" -> noOfSticks.toString)
                      }
                    }
                  }
                }
              )

          def processTobaccoEditForm: Future[Result] =
            looseTobaccoWeightForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      weight_or_volume_input(
                        formWithErrors,
                        backLinkModel.backLink,
                        customBackLink = true,
                        product,
                        ppi.path,
                        Some(iid),
                        countriesService.getAllCountries,
                        countriesService.getAllCountriesAndEu,
                        currencyService.getAllCurrencies,
                        context.getJourneyData.euCountryCheck
                      )
                    )
                  ),
                success = dto => {

                  val grabOriginalWeight: Future[Option[BigDecimal]] =
                    for {
                      data: Option[JourneyData]          <- cache.fetch
                      originalWeight: Option[BigDecimal] <-
                        Future(
                          data.map(_.purchasedProductInstances.filter(_.iid == iid).flatMap(_.weightOrVolume).head)
                        )
                    } yield originalWeight

                  val totalWeightForLooseTobaccoPlusUserAnswers: Future[BigDecimal] =
                    for {
                      jd: JourneyData                      <- cache.store(
                                                                newPurchaseService.updatePurchase(
                                                                  ppi.path,
                                                                  iid,
                                                                  dto.weightOrVolume,
                                                                  dto.noOfSticks,
                                                                  dto.country,
                                                                  dto.originCountry,
                                                                  dto.currency,
                                                                  dto.cost
                                                                )
                                                              )
                      chewingTobaccoTotalWeight: BigDecimal =
                        calculatorService.calculateTotalWeightOrVolumeForItemType(Some(jd), "chewing-tobacco")
                      rollingTobaccoTotalWeight: BigDecimal =
                        calculatorService.calculateTotalWeightOrVolumeForItemType(Some(jd), "rolling-tobacco")
                    } yield {
                      val looseTobaccoTotalWeightInGrams: BigDecimal =
                        (chewingTobaccoTotalWeight + rollingTobaccoTotalWeight)
                          .setScale(5, BigDecimal.RoundingMode.HALF_UP) * 1000
                      looseTobaccoTotalWeightInGrams
                    }

                  val updatedLooseTobaccoWeightsWithCleanup = for {
                    w                  <- grabOriginalWeight
                    updatedUserAnswers <- totalWeightForLooseTobaccoPlusUserAnswers
                    _: JourneyData     <- cache.store(
                                            newPurchaseService.updatePurchase(
                                              ppi.path,
                                              iid,
                                              w,
                                              dto.noOfSticks,
                                              dto.country,
                                              dto.originCountry,
                                              dto.currency,
                                              dto.cost
                                            )
                                          )
                  } yield updatedUserAnswers

                  updatedLooseTobaccoWeightsWithCleanup.flatMap { priorWeightAndVolume =>
                    if (looseTobaccoWeightConstraint(priorWeightAndVolume)) {
                      cache.store(
                        newPurchaseService.updatePurchase(
                          ppi.path,
                          iid,
                          dto.weightOrVolume,
                          dto.noOfSticks,
                          dto.country,
                          dto.originCountry,
                          dto.currency,
                          dto.cost
                        )
                      ) map { _ =>
                        (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                          case (Some(true), Some("greatBritain")) =>
                            Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                          case (Some(false), Some("euOnly"))
                              if countriesService.isInEu(dto.originCountry.getOrElse("")) =>
                            Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                          case _                                  => Redirect(routes.SelectProductController.nextStep)
                        }
                      }
                    } else {
                      Future(
                        Redirect(
                          routes.LimitExceedController.loadLimitExceedPage(ppi.path)
                        )
                          .removingFromSession(s"user-amount-input-${product.token}")
                          .addingToSession(
                            s"user-amount-input-${product.token}" -> priorWeightAndVolume
                              .setScale(2, BigDecimal.RoundingMode.HALF_UP)
                              .toString
                          )
                      )
                    }
                  }
                }
              )

          def processOtherTobaccoEditForm: Future[Result] =
            cigarAndCigarilloForm(ppi.path)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      no_of_sticks_weight_or_volume_input(
                        formWithErrors,
                        backLinkModel.backLink,
                        customBackLink = true,
                        product,
                        ppi.path,
                        Some(iid),
                        countriesService.getAllCountries,
                        countriesService.getAllCountriesAndEu,
                        currencyService.getAllCurrencies,
                        context.getJourneyData.euCountryCheck
                      )
                    )
                  ),
                dto => {
                  val totalNoOfSticksForItemType: Future[Int] = for {
                    data: Option[JourneyData]       <- cache.fetch
                    previousTotalWeightOrVolume: Int =
                      calculatorService.calculateTotalNumberOfSticksForItemType(data, product.token)
                    totalNoOfSticks: Int             =
                      previousTotalWeightOrVolume +
                        dto.noOfSticks.getOrElse(0)
                  } yield totalNoOfSticks

                  totalNoOfSticksForItemType.flatMap { noOfSticks =>
                    if (cigarAndCigarilloConstraint(noOfSticks, product.token)) {
                      cache.store(
                        newPurchaseService.updatePurchase(
                          ppi.path,
                          iid,
                          dto.weightOrVolume,
                          dto.noOfSticks,
                          dto.country,
                          dto.originCountry,
                          dto.currency,
                          dto.cost
                        )
                      ) map { _ =>
                        (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                          case (Some(true), Some("greatBritain")) =>
                            Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path, iid))
                          case (Some(false), Some("euOnly"))      =>
                            if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                              Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path, iid))
                            } else {
                              Redirect(routes.SelectProductController.nextStep)
                            }
                          case _                                  => Redirect(routes.SelectProductController.nextStep)
                        }
                      }
                    } else {
                      cache.fetch.map { data =>
                        Redirect(
                          routes.LimitExceedController.loadLimitExceedPage(ppi.path)
                        )
                          .removingFromSession(s"user-amount-input-${product.token}")
                          .addingToSession(s"user-amount-input-${product.token}" -> noOfSticks.toString)
                      }
                    }
                  }
                }
              )

          product.templateId match {
            case "cigarettes" =>
              processCigarettesEditForm
            case "tobacco"    =>
              processTobaccoEditForm
            case _            =>
              processOtherTobaccoEditForm
          }
        }
      }
    }
  }
}
