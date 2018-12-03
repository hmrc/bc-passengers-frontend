package controllers

import config.AppConfig
import javax.inject.Inject
import models.{ProductTreeBranch, ProductTreeLeaf, ProductPath, SelectProductsDto}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class   SelectProductController @Inject()(
  val productTreeService: ProductTreeService,
  val travelDetailsService: TravelDetailsService,
  val currencyService: CurrencyService,
  val countriesService: CountriesService,
  val selectProductService: SelectProductService,
  val purchasedProductService: PurchasedProductService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def nextStep(): Action[AnyContent] = DashboardAction { implicit context =>

    withNextSelectedProductPath {

      case None =>
        Future.successful(Redirect(routes.DashboardController.showDashboard()))

      case Some(productPath) =>

        selectProductService.removeSelectedProduct() flatMap { _ =>  //FIXME - if an invalid path is supplied, this would still remove the top item from the stack
          requireProductOrCategory(productPath) {

            case ProductTreeBranch(_, _, children) =>
              Future.successful(Redirect(routes.SelectProductController.askProductSelection(productPath)))

            case ProductTreeLeaf(_, _, _, templateId) =>

              templateId match {
                case "alcohol" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/products/"+productPath+"/start"))
                case "cigarettes" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/products/"+productPath+"/start"))
                case "cigars" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/products/"+productPath+"/start"))
                case "tobacco" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/products/"+productPath+"/start"))
                case "other-goods" => Future.successful(Redirect("/check-tax-on-goods-you-bring-into-the-uk/products/"+productPath+"/quantity"))
              }

          }
        }
    }
  }

  def askProductSelection(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    requireProductOrCategory(path) {

      case ProductTreeBranch(_, _, children) =>
        Future.successful(Ok(views.html.purchased_products.select_products(SelectProductsDto.form, children.map( i => ( i.name, i.token ) ), path)))
      case _ =>
        Future.successful(InternalServerError(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem.")))

    }
  }


  def processProductSelection(path: ProductPath): Action[AnyContent] = DashboardAction { implicit context =>

    requireCategory(path) { branch =>

      SelectProductsDto.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful {
            BadRequest(views.html.purchased_products.select_products(formWithErrors, branch.children.map(i => (i.name, i.token)), path))
          }
        },
        success = selectProductsDto => {

          selectProductService.addSelectedProducts(context.getJourneyData, selectProductsDto.tokens.map(path.addingComponent)) flatMap { journeyData =>
            
            purchasedProductService.clearWorkingInstance(journeyData) map { _ =>
              Redirect(routes.SelectProductController.nextStep())
            }
          }
        }
      )
    }
  }

}
