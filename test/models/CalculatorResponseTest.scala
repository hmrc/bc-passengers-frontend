package models

import util.BaseSpec

class CalculatorResponseTest extends BaseSpec {

  "Calling CalculatorResponse.onlyGBP" should {

    trait LocalSetup {
      def alcoholCurrency: String
      def tobaccoCurrency: String
      def otherGoodsCurrency: String

      lazy val cr = CalculatorResponse(
        Some(Alcohol(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "100.00", alcoholCurrency))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "100.00", tobaccoCurrency))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "100.00", otherGoodsCurrency))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00")
      )
    }

    "Work with alcohol not GBP" in new LocalSetup {

      override lazy val alcoholCurrency: String = "UGX"
      override lazy val tobaccoCurrency: String = "GBP"
      override lazy val otherGoodsCurrency: String = "GBP"

      cr.hasOnlyGBP shouldBe false
    }

    "Work with tobacco not GBP" in new LocalSetup {

      override lazy val alcoholCurrency: String = "GBP"
      override lazy val tobaccoCurrency: String = "UGX"
      override lazy val otherGoodsCurrency: String = "GBP"

      cr.hasOnlyGBP shouldBe false
    }

    "Work with other goods not GBP" in new LocalSetup {

      override lazy val alcoholCurrency: String = "GBP"
      override lazy val tobaccoCurrency: String = "GBP"
      override lazy val otherGoodsCurrency: String = "UGX"

      cr.hasOnlyGBP shouldBe false
    }

    "Work when all GBP" in new LocalSetup {

      override lazy val alcoholCurrency: String = "GBP"
      override lazy val tobaccoCurrency: String = "GBP"
      override lazy val otherGoodsCurrency: String = "GBP"

      cr.hasOnlyGBP shouldBe true
    }

  }
}
