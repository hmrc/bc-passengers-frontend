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

      val ppi = PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", false, Nil)), Some("USD"))

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = List(ppi))))

      val modifiedJourneyData = s.insertPurchases(ProductPath("some/item/path"), Some(185.5), Some(100), "FR", "EUR", List(12.50, 13.60, 14.70), new Random(1))(localContext)

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        ppi,
        PurchasedProductInstance(ProductPath("some/item/path"), "NAvZuG", Some(185.5), Some(100), Some(Country("FR", "title.france", "FR", true, Nil)), Some("EUR"), Some(12.50)),
        PurchasedProductInstance(ProductPath("some/item/path"), "ESoIJh", Some(185.5), Some(100), Some(Country("FR", "title.france", "FR", true, Nil)), Some("EUR"), Some(13.60)),
        PurchasedProductInstance(ProductPath("some/item/path"), "bqOIsA", Some(185.5), Some(100), Some(Country("FR", "title.france", "FR", true, Nil)), Some("EUR"), Some(14.70))
      )
    }

    "set default country and currency in journey data" in new LocalSetup{

      val ppi = PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", false, Nil)), Some("USD"))

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = List(ppi))))

      val modifiedJourneyData = s.insertPurchases(ProductPath("some/item/path"), Some(185.5), Some(100), "FR", "EUR", List(12.50, 13.60, 14.70), new Random(1))(localContext)

      modifiedJourneyData.defaultCountry shouldBe Some("FR")
      modifiedJourneyData.defaultCurrency shouldBe Some("EUR")
    }
  }

  "Calling NewPurchaseService.updatePurchase" should {

    "update a purchase in existing journey data" in new LocalSetup {

      val ppis = List(
        PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", false, Nil)), Some("USD"), Some(1.69)),
        PurchasedProductInstance(ProductPath("some/item/path"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", false, Nil)), Some("USD"), Some(2.99))
      )

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = ppis)))

      val modifiedJourneyData = s.updatePurchase(ProductPath("some/item/path"), "iid1", Some(185.5), Some(100), "FR", "EUR", 14.70)(localContext)

      modifiedJourneyData.purchasedProductInstances shouldBe List(
        PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", false, Nil)), Some("USD"), Some(1.69)),
        PurchasedProductInstance(ProductPath("some/item/path"), "iid1", Some(185.5), Some(100), Some(Country("FR", "title.france", "FR", true, Nil)), Some("EUR"), Some(14.70))
      )

    }

    "set default country and currency in journey data" in new LocalSetup{

      val ppis = List(
        PurchasedProductInstance(ProductPath("some/item/path"), "iid0", None, None, Some(Country("EG", "title.egypt", "EG", false, Nil)), Some("USD"), Some(1.69)),
        PurchasedProductInstance(ProductPath("some/item/path"), "iid1", None, None, Some(Country("EG", "title.egypt", "EG", false, Nil)), Some("USD"), Some(2.99))
      )

      val localContext = LocalContext(EnhancedFakeRequest("GET", "anything"), "123", Some(JourneyData(purchasedProductInstances = ppis)))

      val modifiedJourneyData = s.updatePurchase(ProductPath("some/item/path"), "iid1", Some(185.5), Some(100), "BG", "AED", 14.70)(localContext)

      modifiedJourneyData.defaultCountry shouldBe Some("BG")
      modifiedJourneyData.defaultCurrency shouldBe Some("AED")
    }
  }

}
