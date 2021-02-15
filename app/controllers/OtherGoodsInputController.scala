/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.Cache
import controllers.enforce.DashboardAction

import javax.inject.Inject
import models.{OtherGoodsDto, ProductPath}
import play.api.data.Form
import play.api.data.Forms.{optional, _}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import util._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class OtherGoodsInputController @Inject()(
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,

  dashboardAction: DashboardAction,

  val other_goods_input: views.html.other_goods.other_goods_input,
  val error_template: views.html.error_template,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  val addCostForm: Form[OtherGoodsDto] = Form(
    mapping(
      "action" -> nonEmptyText,
      "country" -> text,
      "originCountry" -> optional(text),
      "currency" -> text,
      "cost" -> bigDecimal(0,2),
      "isVatPaid" -> optional(boolean),
      "isUccRelief" ->  optional(boolean),
      "isCustomPaid" -> optional(boolean),
      "hasEvidence" -> optional(boolean)
    )(OtherGoodsDto.apply)(OtherGoodsDto.unapply)
  )

  def continueForm(path: ProductPath): Form[OtherGoodsDto] = Form(
    mapping(
      "action" -> nonEmptyText,
      "country" -> text
        .verifying("error.country.invalid", name => countriesService.isValidCountryCode(name)),
      "originCountry" -> optional(text),
      "currency" -> text
        .verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost" -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint(path.toMessageKey))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid" -> optional(boolean),
      "isUccRelief" ->  optional(boolean),
      "isCustomPaid" -> optional(boolean),
      "hasEvidence" -> optional(boolean)
    )(OtherGoodsDto.apply)(OtherGoodsDto.unapply)
  )

  def displayAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>

    requireProduct(path) { product =>
      withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
          Future.successful(Ok(other_goods_input(continueForm(path).bind(Map("country" -> defaultCountry.getOrElse(""), "originCountry" -> defaultOriginCountry.getOrElse(""), "currency" -> defaultCurrency.getOrElse(""))).discardingErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck)))
      }
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        OtherGoodsDto.fromPurchasedProductInstance(ppi) match {
          case Some(dto) => Future.successful( Ok( other_goods_input(addCostForm.fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck) ) )
          case None => logAndRenderError("Unable to construct dto from PurchasedProductInstance")
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = dashboardAction { implicit context =>

    requireProduct(path) { product =>

      def processContinue = continueForm(path).bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest( other_goods_input(formWithErrors, product, path, None, countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck) ))
        },
        dto => {
          val jd = newPurchaseService.insertPurchases(path, None, None, dto.country, dto.originCountry, dto.currency, List(dto.cost))
          cache.store(jd._1) map {_ =>
            (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
              case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(path,jd._2))
              case (Some(false), Some("euOnly")) => {
                if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                  Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(path, jd._2))
                } else {
                  Redirect(routes.SelectProductController.nextStep())
                }
              }
              case _ => Redirect(routes.SelectProductController.nextStep())
            }
          }
        }
      )
      processContinue
    }

  }


  def processEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>

        def processContinue = continueForm(ppi.path).bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(other_goods_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, countriesService.getAllCountriesAndEu, currencyService.getAllCurrencies, context.getJourneyData.euCountryCheck))),
          dto => {
            val jd = newPurchaseService.updatePurchase(ppi.path, iid, None, None, dto.country, dto.originCountry, dto.currency, dto.cost)
            cache.store(jd) map { _ =>
              (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
                case (Some(true), Some("greatBritain")) => Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(ppi.path,iid))
                case (Some(false), Some("euOnly")) => {
                  if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                    Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(ppi.path,iid))
                  } else {
                    Redirect(routes.SelectProductController.nextStep())
                  }
                }
                case _ => Redirect(routes.SelectProductController.nextStep())
              }
            }
          }
        )
        processContinue
      }
    }
  }


}
