package controllers

import config.AppConfig
import javax.inject.Inject
import models.{ProductTreeBranch, ProductTreeLeaf, ProductPath, SelectProductsDto}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class SelectProductController @Inject()(
  val productTreeService: ProductTreeService,
  val travelDetailsService: TravelDetailsService,
  val currencyService: CurrencyService,
  val selectProductService: SelectProductService
)(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with PublicActions with ControllerHelpers {

  def nextStep(): Action[AnyContent] = PublicAction { implicit request =>

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
                case "alcohol" => Future.successful(Redirect("/bc-passengers-frontend/products/"+productPath+"/start"))
                case "cigarettes" => Future.successful(Redirect("/bc-passengers-frontend/products/"+productPath+"/start"))
                case "cigars" => Future.successful(Redirect("/bc-passengers-frontend/products/"+productPath+"/start"))
                case "tobacco" => Future.successful(Redirect("/bc-passengers-frontend/products/"+productPath+"/start"))
                case "other-goods" => Future.successful(Redirect("/bc-passengers-frontend/products/"+productPath+"/quantity"))
              }

          }
        }
    }
  }

  def askProductSelection(path: ProductPath): Action[AnyContent] = PublicAction { implicit request =>

    requireProductOrCategory(path) {

      case ProductTreeBranch(_, _, children) =>
        Future.successful(Ok(views.html.passengers.select_products(SelectProductsDto.form, children.map( i => ( i.name, i.token ) ), path)))
      case _ =>
        Future.successful(InternalServerError(views.html.error_template("Technical problem", "Technical problem", "There has been a technical problem.")))

    }

  }


  def processProductSelection(path: ProductPath): Action[AnyContent] = PublicAction { implicit request =>

    requireCategory(path) { branch =>

      SelectProductsDto.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful {
            BadRequest(views.html.passengers.select_products(formWithErrors, branch.children.map(i => (i.name, i.token)), path))
          }
        },
        success = selectProductsDto => {

          selectProductService.addSelectedProducts(selectProductsDto.tokens.map(path.addingComponent)) map { _ =>
            Redirect(routes.SelectProductController.nextStep())
          }
        }
      )

    }

  }

}
