package models

import models.{JourneyData, ProductPath, PurchasedProduct, PurchasedProductInstance}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import services.LocalSessionCache
import util.BaseSpec

import scala.concurrent.Future

class JourneyDataSpec extends BaseSpec {

  "invoking updatePurchasedProductInstance" should {
    "update the instance in place" in {
      val purchasedProduct = PurchasedProduct(ProductPath("alcohol/beer"), List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some("USD"), Some(BigDecimal("10.00"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("3.0")), None, Some("USD"), Some(BigDecimal("11.00"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid3", Some(BigDecimal("4.0")), None, Some("USD"), Some(BigDecimal("12.00")))
      ))

      val modified = purchasedProduct.updatePurchasedProductInstance("iid2") { instance =>
        instance.copy(currency = Some("AUD"))
      }

      modified shouldBe PurchasedProduct(ProductPath("alcohol/beer"), List(
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid1", Some(BigDecimal("2.0")), None, Some("USD"), Some(BigDecimal("10.00"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid2", Some(BigDecimal("3.0")), None, Some("AUD"), Some(BigDecimal("11.00"))),
        PurchasedProductInstance(ProductPath("alcohol/beer"), "iid3", Some(BigDecimal("4.0")), None, Some("USD"), Some(BigDecimal("12.00")))
      ))
    }
  }
}
