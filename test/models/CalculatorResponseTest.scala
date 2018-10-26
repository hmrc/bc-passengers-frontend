package models

import util.BaseSpec

class CalculatorResponseTest extends BaseSpec {

  "Calling CalculatorResponse.onlyGBP" should {

    trait LocalSetup {
      def alcoholCurrency: Currency
      def tobaccoCurrency: Currency
      def otherGoodsCurrency: Currency

      lazy val cr = CalculatorResponse(
        Some(Alcohol(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", alcoholCurrency, Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", tobaccoCurrency, Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", otherGoodsCurrency, Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00")
      )
    }

    "Work with alcohol not GBP" in new LocalSetup {

      override lazy val alcoholCurrency = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"))
      override lazy val tobaccoCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val otherGoodsCurrency = Currency("GBP", "British Pound (GBP)", None)

      cr.hasOnlyGBP shouldBe false
    }

    "Work with tobacco not GBP" in new LocalSetup {

      override lazy val alcoholCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val tobaccoCurrency = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"))
      override lazy val otherGoodsCurrency = Currency("GBP", "British Pound (GBP)", None)

      cr.hasOnlyGBP shouldBe false
    }

    "Work with other goods not GBP" in new LocalSetup {

      override lazy val alcoholCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val tobaccoCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val otherGoodsCurrency = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"))

      cr.hasOnlyGBP shouldBe false
    }

    "Work when all GBP" in new LocalSetup {

      override lazy val alcoholCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val tobaccoCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val otherGoodsCurrency = Currency("GBP", "British Pound (GBP)", None)

      cr.hasOnlyGBP shouldBe true
    }

  }
}
