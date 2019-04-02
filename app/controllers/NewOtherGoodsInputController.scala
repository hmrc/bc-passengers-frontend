package controllers

import config.AppConfig
import connectors.Cache
import javax.inject.Inject
import models.{ProductPath, OtherGoodsDto}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import util._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NewOtherGoodsInputController @Inject() (
  val cache: Cache,
  val productTreeService: ProductTreeService,
  val newPurchaseService: NewPurchaseService,
  val countriesService: CountriesService,
  val currencyService: CurrencyService,
  val calculatorService: CalculatorService,

  val other_goods_input: views.html.new_other_goods.other_goods_input,
  val error_template: views.html.error_template,

  override val controllerComponents: MessagesControllerComponents,
  implicit val appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents) with I18nSupport with ControllerHelpers {

  val addCostForm: Form[OtherGoodsDto] = Form(
    mapping(
      "action" -> nonEmptyText,
      "country" -> text,
      "currency" -> text,
      "costs" -> list(text)
        .transform[List[BigDecimal]](
          _.map(s => Try(BigDecimal(s.filter(_ != ','))).toOption.getOrElse(BigDecimal(0))),
          _.map(bd => if(bd>0) formatMonetaryValue(bd) else "")
        )
    )(OtherGoodsDto.apply)(OtherGoodsDto.unapply)
  )

  def continueForm(path: ProductPath): Form[OtherGoodsDto] = Form(
    mapping(
      "action" -> nonEmptyText,
      "country" -> text
        .verifying("error.country.invalid", name => countriesService.isValidCountryCode(name)),
      "currency" -> text
        .verifying("error.currency.invalid", code => currencyService.isValidCurrencyCode(code)),
      "costs" -> list(
        text
          .transform[String](
            s => if(s.lastOption == Some('.')) s+"0" else s,
            s => s
          )
          .verifying(blankOkCostCheckConstraint(path.toMessageKey))
      )
      .transform[List[String]](_.filter(!_.isEmpty), identity)
      .verifying("error.required.cost."+path.toMessageKey, _.size > 0)
      .transform[List[BigDecimal]](_.map(s => BigDecimal(s.filter(_ != ','))), _.map(formatMonetaryValue))
    )(OtherGoodsDto.apply)(OtherGoodsDto.unapply)
  )

  def displayAddForm(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>
      Future.successful(Ok( other_goods_input(continueForm(path), product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies) ))
    }
  }

  def displayEditForm(iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>
        OtherGoodsDto.fromPurchasedProductInstance(ppi) match {
          case Some(dto) => Future.successful( Ok( other_goods_input(addCostForm.fill(dto), product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies) ) )
          case None => logAndRenderError("Unable to construct dto from PurchasedProductInstance")
        }
      }
    }
  }

  def processAddForm(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    requireProduct(path) { product =>

      def processContinue = continueForm(path).bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest( other_goods_input(formWithErrors, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies) ))
        },
        dto => {
          val jd = newPurchaseService.insertPurchases(path, None, None, dto.country, dto.currency, dto.costs)
          cache.store(jd) map {_ =>
            Redirect(routes.SelectProductController.nextStep())
          }
        }
      )

      def processAddCost = addCostForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest( other_goods_input(formWithErrors, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies) )),
        dto => {
          val f = addCostForm.fill( dto.copy(costs = (dto.costs :+ BigDecimal(0)).take(9)) )
          Future.successful(Ok( other_goods_input(f, product, path, None, countriesService.getAllCountries, currencyService.getAllCurrencies) ))
        }
      )

      if (context.getFormParam("action") == Some("add-cost")) processAddCost
      else processContinue
    }

  }


  def processEditForm(iid: String): Action[AnyContent] = DashboardAction { implicit context =>

    requirePurchasedProductInstance(iid) { ppi =>
      requireProduct(ppi.path) { product =>

        def processContinue = continueForm(ppi.path).bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(other_goods_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies))),
          dto => {
            val jd = newPurchaseService.updatePurchase(ppi.path, iid, None, None, dto.country, dto.currency, dto.costs.head)
            cache.store(jd) map { _ =>
              Redirect(routes.SelectProductController.nextStep())
            }
          }
        )

        def processAddCost = addCostForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(other_goods_input(formWithErrors, product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies))),
          dto => {
            val f = addCostForm.fill(dto.copy(costs = dto.costs :+ BigDecimal(0)))
            Future.successful(Ok(other_goods_input(f, product, ppi.path, Some(iid), countriesService.getAllCountries, currencyService.getAllCurrencies)))
          }
        )

        if (context.getFormParam("action") == Some("add-cost")) processAddCost
        else processContinue
      }
    }
  }
}
