package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.{ProductTreeLeaf, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CountriesService, CurrencyService, ProductTreeService, TravelDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

case class PurchasedProductDetails(
  purchasedProductInstance: PurchasedProductInstance,
  productTreeNode: ProductTreeNode,
  currencyDisplayName: String,
  productDescription: String,
  displayWeight: Option[String]
)

@Singleton
class DashboardController @Inject() (
  val countriesService: CountriesService,
  val travelDetailsService: TravelDetailsService,
  val messagesApi: MessagesApi,
  val productTreeService: ProductTreeService,
  val currencyService: CurrencyService
)(implicit val appConfig: AppConfig) extends FrontendController with I18nSupport with PublicActions with ControllerHelpers {

  val showDashboard: Action[AnyContent] = PublicAction { implicit request =>

    travelDetailsService.getJourneyData map { journeyData =>

      val jd = journeyData.getOrElse(JourneyData())

      val alcoholPurchasedProducts = jd.purchasedProducts.getOrElse(Nil).filter(_.path.fold(false)(_.components.head=="alcohol"))
      val tobaccoPurchasedProducts = jd.purchasedProducts.getOrElse(Nil).filter(_.path.fold(false)(_.components.head=="tobacco"))
      val otherGoodsPurchasedProducts = jd.purchasedProducts.getOrElse(Nil).filter(_.path.fold(false)(_.components.head=="other-goods"))

      val alcoholPurchasedProductDetailsList: List[PurchasedProductDetails] = for {
        pp <- alcoholPurchasedProducts
        ppi <- pp.purchasedProductInstances.getOrElse(Nil)
        path <- pp.path.toList
        ptn <- productTreeService.getProducts.getDescendant(path).collect { case p: ProductTreeLeaf => p}
        c <- currencyService.getCurrencyByCode(ppi.currency.getOrElse(""))
        description <- ptn.getDescription(ppi)
      } yield PurchasedProductDetails(ppi, ptn, c.displayName, description, None)

      val tobaccoPurchasedProductDetailsList: List[PurchasedProductDetails] = for {
        pp <- tobaccoPurchasedProducts
        ppi <- pp.purchasedProductInstances.getOrElse(Nil)
        path <- pp.path.toList
        ptn <- productTreeService.getProducts.getDescendant(path).collect { case p: ProductTreeLeaf => p}
        curCode <- ppi.currency
        currency <- currencyService.getCurrencyByCode(curCode)
        description <- ptn.getDescription(ppi)
      } yield PurchasedProductDetails(ppi, ptn, currency.displayName, description, ptn.getDisplayWeight(ppi))

      val otherGoodsPurchasedProductDetailsList: List[PurchasedProductDetails] = for {
        pp <- otherGoodsPurchasedProducts
        ppi <- pp.purchasedProductInstances.getOrElse(Nil)
        path <- pp.path.toList
        ptn <- productTreeService.getProducts.getDescendant(path).collect { case p: ProductTreeLeaf => p}
        c <- currencyService.getCurrencyByCode(ppi.currency.getOrElse(""))
      } yield PurchasedProductDetails(ppi, ptn, c.displayName, ptn.name, None)


      Ok(views.html.passengers.dashboard( jd, alcoholPurchasedProductDetailsList.reverse, tobaccoPurchasedProductDetailsList.reverse, otherGoodsPurchasedProductDetailsList.reverse))

    }
  }
}
