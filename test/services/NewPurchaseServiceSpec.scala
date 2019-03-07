package services

import controllers.LocalContext
import models.{Country, JourneyData, ProductPath, PurchasedProductInstance}
import util.BaseSpec

import scala.util.Random

class NewPurchaseServiceSpec extends BaseSpec {


  trait LocalSetup {

    lazy val s = app.injector.instanceOf[NewPurchaseService]
  }

  "Calling NewPurchaseService.insertPurchases" should {

    "add purchases to existing journey data" in new LocalSetup {

      val ppi = PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Nil)), currency = Some("USD"))

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = List(ppi))))

      val modifiedJourneyData = s.insertPurchases(ProductPath("some/item/path"), "France", "EUR", List(12.50, 13.60, 14.70), new Random(1))(localContext)

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        ppi,
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "NAvZuG", None, None, Some(Country("France", "FR", isEu=true, Nil)), Some("EUR"), Some(12.50)),
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "ESoIJh", None, None, Some(Country("France", "FR", isEu=true, Nil)), Some("EUR"), Some(13.60)),
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "bqOIsA", None, None, Some(Country("France", "FR", isEu=true, Nil)), Some("EUR"), Some(14.70))
      )

    }
  }

  "Calling NewPurchaseService.updatePurchase" should {

    "update a purchase in existing journey data" in new LocalSetup {

      val ppis = List(
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Nil)), currency = Some("USD"), cost = Some(1.69)),
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid1", country = Some(Country("Egypt", "EG", isEu = false, Nil)), currency = Some("USD"), cost = Some(2.99))
      )

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = ppis)))

      val modifiedJourneyData = s.updatePurchase(ProductPath("some/item/path"), "iid1", "France", "EUR", 14.70)(localContext)

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid0", country = Some(Country("Egypt", "EG", isEu = false, Nil)), currency = Some("USD"), cost = Some(1.69)),
        PurchasedProductInstance(ProductPath("some/item/path"), iid = "iid1", country = Some(Country("France", "FR", isEu = true, Nil)), currency = Some("EUR"), cost = Some(14.70))
      )

    }
  }

}
