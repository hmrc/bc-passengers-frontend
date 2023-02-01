/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.enforce.DashboardAction
import models.{OtherGoodsDto, OtherGoodsSearchItem, ProductPath}
import play.api.data.Form
import play.api.data.Forms.{optional, _}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherGoodsInputController @Inject() (
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
) extends FrontendController(controllerComponents)
    with I18nSupport
    with ControllerHelpers {

  val addCostForm: Form[OtherGoodsDto] = Form(
    mapping(
      "searchTerm"    -> optional(text)
        .transform[Option[OtherGoodsSearchItem]](
          _.flatMap(term => productTreeService.otherGoodsSearchItems.find(_.name == term)),
          _.map(_.name)
        ),
      "country"       -> text
        .verifying("error.country.invalid", name => countriesService.isValidCountryCode(name)),
      "originCountry" -> optional(text),
      "currency"      -> text
        .verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost"          -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint("other-goods.price"))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid"     -> optional(boolean),
      "isUccRelief"   -> optional(boolean),
      "isCustomPaid"  -> optional(boolean),
      "hasEvidence"   -> optional(boolean)
    )(OtherGoodsDto.apply)(OtherGoodsDto.unapply)
  )

  def continueForm(implicit context: LocalContext): Form[OtherGoodsDto] = Form(
    mapping(
      "searchTerm"    -> optional(text)
        .verifying(
          "error.other_goods_search",
          _.fold(context.getJourneyData.selectedAliases.nonEmpty) { term =>
            productTreeService.otherGoodsSearchItems.exists(_.name == term)
          }
        )
        .transform[Option[OtherGoodsSearchItem]](
          _.flatMap(term => productTreeService.otherGoodsSearchItems.find(_.name == term)),
          _.map(_.name)
        ),
      "country"       -> text
        .verifying("error.country.invalid", name => countriesService.isValidCountryCode(name)),
      "originCountry" -> optional(text),
      "currency"      -> text
        .verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "cost"          -> text
        .transform[String](s => s.filter(_ != ','), identity)
        .verifying(bigDecimalCostCheckConstraint("other-goods.price"))
        .transform[BigDecimal](BigDecimal.apply, formatMonetaryValue),
      "isVatPaid"     -> optional(boolean),
      "isUccRelief"   -> optional(boolean),
      "isCustomPaid"  -> optional(boolean),
      "hasEvidence"   -> optional(boolean)
    )(OtherGoodsDto.apply)(OtherGoodsDto.unapply)
  )

  val displayAddForm: Action[AnyContent] = dashboardAction { implicit context =>
    if (context.journeyData.isDefined && context.getJourneyData.amendState.getOrElse("").equals("pending-payment")) {
      Future.successful(Redirect(routes.PreviousDeclarationController.loadPreviousDeclarationPage))
    } else {
      withDefaults(context.getJourneyData) { defaultCountry => defaultOriginCountry => defaultCurrency =>
        val term: List[String] = context.getJourneyData.selectedAliases.map(_.term)
        term.size match {
          case 1 =>
            cache
              .storeJourneyData(context.getJourneyData.copy(selectedAliases = Nil))
              .map(_ =>
                Ok(
                  other_goods_input(
                    continueForm
                      .bind(
                        Map(
                          "searchTerm"    -> term.head,
                          "country"       -> defaultCountry.getOrElse(""),
                          "originCountry" -> defaultOriginCountry.getOrElse(""),
                          "currency"      -> defaultCurrency.getOrElse("")
                        )
                      )
                      .discardingErrors,
                    None,
                    countriesService.getAllCountries,
                    countriesService.getAllCountriesAndEu,
                    currencyService.getAllCurrencies,
                    context.getJourneyData.euCountryCheck,
                    productTreeService.otherGoodsSearchItems,
                    "create",
                    ProductPath.apply(Nil)
                  )
                )
              )
          case _ =>
            cache
              .storeJourneyData(context.getJourneyData.copy(selectedAliases = Nil))
              .map(_ =>
                Ok(
                  other_goods_input(
                    continueForm
                      .bind(
                        Map(
                          "searchTerm"    -> "",
                          "country"       -> defaultCountry.getOrElse(""),
                          "originCountry" -> defaultOriginCountry.getOrElse(""),
                          "currency"      -> defaultCurrency.getOrElse("")
                        )
                      )
                      .discardingErrors,
                    None,
                    countriesService.getAllCountries,
                    countriesService.getAllCountriesAndEu,
                    currencyService.getAllCurrencies,
                    context.getJourneyData.euCountryCheck,
                    productTreeService.otherGoodsSearchItems,
                    "create",
                    ProductPath.apply(Nil)
                  )
                )
              )
        }
      }
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      OtherGoodsDto.fromPurchasedProductInstance(ppi) match {
        case Some(dto) =>
          Future.successful(
            Ok(
              other_goods_input(
                addCostForm.fill(dto),
                Some(iid),
                countriesService.getAllCountries,
                countriesService.getAllCountriesAndEu,
                currencyService.getAllCurrencies,
                context.getJourneyData.euCountryCheck,
                productTreeService.otherGoodsSearchItems,
                "edit",
                ppi.path
              )
            )
          )
        case None      => logAndRenderError("Unable to construct dto from PurchasedProductInstance")
      }
    }
  }

  val processAddForm: Action[AnyContent] = dashboardAction { implicit context =>
    def processContinue = continueForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(
          BadRequest(
            other_goods_input(
              formWithErrors,
              None,
              countriesService.getAllCountries,
              countriesService.getAllCountriesAndEu,
              currencyService.getAllCurrencies,
              context.getJourneyData.euCountryCheck,
              productTreeService.otherGoodsSearchItems,
              "create",
              ProductPath.apply(Nil)
            )
          )
        ),
      dto =>
        requireProduct(dto.searchTerm.get.path) { _ =>
          val jd = newPurchaseService.insertPurchases(
            dto.searchTerm.get.path,
            None,
            None,
            dto.country,
            dto.originCountry,
            dto.currency,
            List(dto.cost),
            dto.searchTerm
          )
          cache.store(jd._1) map { _ =>
            (context.getJourneyData.arrivingNICheck, context.getJourneyData.euCountryCheck) match {
              case (Some(true), Some("greatBritain")) =>
                Redirect(routes.UKVatPaidController.loadItemUKVatPaidPage(dto.searchTerm.get.path, jd._2))
              case (Some(false), Some("euOnly"))      =>
                if (countriesService.isInEu(dto.originCountry.getOrElse(""))) {
                  Redirect(routes.EUEvidenceController.loadEUEvidenceItemPage(dto.searchTerm.get.path, jd._2))
                } else {
                  Redirect(routes.SelectProductController.nextStep)
                }
              case _                                  => Redirect(routes.SelectProductController.nextStep)
            }
          }
        }
    )
    processContinue

  }

  def processEditForm(iid: String): Action[AnyContent] = dashboardAction { implicit context =>
    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { _ =>
        def processContinue = addCostForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                other_goods_input(
                  formWithErrors,
                  Some(iid),
                  countriesService.getAllCountries,
                  countriesService.getAllCountriesAndEu,
                  currencyService.getAllCurrencies,
                  context.getJourneyData.euCountryCheck,
                  productTreeService.otherGoodsSearchItems,
                  "edit",
                  ppi.path
                )
              )
            ),
          dto => {
            val jd = newPurchaseService.updatePurchase(
              ppi.path,
              iid,
              None,
              None,
              dto.country,
              dto.originCountry,
              dto.currency,
              dto.cost,
              ppi.searchTerm
            )
            cache.store(jd) map { _ =>
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
          }
        )
        processContinue
      }
    }
  }

}
