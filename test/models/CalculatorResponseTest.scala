package models

import util.BaseSpec

class CalculatorResponseTest extends BaseSpec {

  "Calling CalculatorResponse.getItemsWithTaxToPay" should {

    trait LocalSetup {

      lazy val cr = CalculatorResponse(
        Some(Alcohol(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("100.00", "0.00", "0.00", "100.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "US Dollars", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("100.00", "0.00", "0.00", "100.00"))
        ), Calculation("100.00", "0.00", "0.00", "100.00"))),
        Some(Tobacco(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "US Dollars", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "US Dollars", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("100.00", "0.00", "0.00", "100.00"),
        withinFreeAllowance = false,
        limits = Map.empty
      )
    }

    "return a list of only items chere tax is payable" in new LocalSetup {

      cr.getItemsWithTaxToPay shouldBe List(

        Item("ANYTHING", "100.00", Some(1), None, Calculation("100.00", "0.00", "0.00", "100.00"), Metadata("Desc", "Desc", "100.00", Currency("USD", "US Dollars", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
      )
    }
  }

  "Calling CalculatorResponse.onlyGBP" should {

    trait LocalSetup {
      def alcoholCurrency: Currency
      def tobaccoCurrency: Currency
      def otherGoodsCurrency: Currency

      lazy val cr = CalculatorResponse(
        Some(Alcohol(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", alcoholCurrency, Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", tobaccoCurrency, Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(
          Band("A", List(
            Item("ANYTHING", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("Desc", "Desc", "100.00", otherGoodsCurrency, Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "0.00"),
        withinFreeAllowance = false,
        limits = Map.empty
      )
    }

    "work with alcohol not GBP" in new LocalSetup {

      override lazy val alcoholCurrency = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"))
      override lazy val tobaccoCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val otherGoodsCurrency = Currency("GBP", "British Pound (GBP)", None)

      cr.allItemsUseGBP shouldBe false
    }

    "work with tobacco not GBP" in new LocalSetup {

      override lazy val alcoholCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val tobaccoCurrency = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"))
      override lazy val otherGoodsCurrency = Currency("GBP", "British Pound (GBP)", None)

      cr.allItemsUseGBP shouldBe false
    }

    "work with other goods not GBP" in new LocalSetup {

      override lazy val alcoholCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val tobaccoCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val otherGoodsCurrency = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"))

      cr.allItemsUseGBP shouldBe false
    }

    "work when all GBP" in new LocalSetup {

      override lazy val alcoholCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val tobaccoCurrency = Currency("GBP", "British Pound (GBP)", None)
      override lazy val otherGoodsCurrency = Currency("GBP", "British Pound (GBP)", None)

      cr.allItemsUseGBP shouldBe true
    }
  }

  "Calling CalculatorResponse.asDto" should {

    "order products correctly when no tax is due" in {

      val calculatorResponseDto = CalculatorResponse(
        Some(Alcohol(List(
          Band("A", List(
            Item("ALC", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("an alcohol item", "an alcohol item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29"))),
            Item("ALC", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("another alcohol item", "another alcohol item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(Tobacco(List(
          Band("A", List(
            Item("TOB", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("a tobacco item", "a tobacco item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(
          Band("A", List(
            Item("OGD", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("an other-goods item", "an other-goods item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "100.00"),
        withinFreeAllowance = false,
        limits = Map.empty
      ).asDto(applySorting = true)


      calculatorResponseDto.items.map(_.metadata.description) shouldBe List(
        "an alcohol item",
        "another alcohol item",
        "a tobacco item",
        "an other-goods item"
      )
    }

    "order products with no tax due first" in {

      val calculatorResponseDto = CalculatorResponse(
        Some(Alcohol(List(
          Band("A", List(
            Item("ALC", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("an alcohol item", "an alcohol item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29"))),
            Item("ALC", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "100.00"), Metadata("an alcohol item with duty", "an alcohol item with duty", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "100.00"))
        ), Calculation("0.00", "0.00", "0.00", "100.00"))),
        Some(Tobacco(List(
          Band("A", List(
            Item("TOB", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("a tobacco item", "a tobacco item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Some(OtherGoods(List(
          Band("A", List(
            Item("OGD", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("an other-goods item", "an other-goods item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "0.00"))
        ), Calculation("0.00", "0.00", "0.00", "0.00"))),
        Calculation("0.00", "0.00", "0.00", "100.00"),
        withinFreeAllowance = false,
        limits = Map.empty
      ).asDto(applySorting = true)

      calculatorResponseDto.items.map(_.metadata.description) shouldBe List(
        "an alcohol item",
        "a tobacco item",
        "an other-goods item",
        "an alcohol item with duty"
      )
    }

    "order products with a mixture of tax due and no tax due" in {

      val calculatorResponseDto = CalculatorResponse(
        Some(Alcohol(List(
          Band("A", List(
            Item("ALC", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("an alcohol item", "an alcohol item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29"))),
            Item("ALC", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "100.00"), Metadata("an alcohol item with duty", "an alcohol item with duty", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "100.00"))
        ), Calculation("0.00", "0.00", "0.00", "100.00"))),
        Some(Tobacco(List(
          Band("A", List(
            Item("TOB", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("a tobacco item", "a tobacco item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29"))),
            Item("TOB", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "100.00"), Metadata("a tobacco item with duty", "a tobacco item with duty", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "100.00"))
        ), Calculation("0.00", "0.00", "0.00", "100.00"))),
        Some(OtherGoods(List(
          Band("A", List(
            Item("OGD", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "0.00"), Metadata("an other-goods item", "an other-goods item", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29"))),
            Item("OGD", "100.00", Some(1), None, Calculation("0.00", "0.00", "0.00", "100.00"), Metadata("an other-goods item with duty", "an other-goods item with duty", "100.00", Currency("GBP", "British Pound (GBP)", None), Country("United States of America (the)", "US", isEu = false, Some("USD"), Nil), ExchangeRate("1.20", "2018-10-29")))
          ), Calculation("0.00", "0.00", "0.00", "100.00"))
        ), Calculation("0.00", "0.00", "0.00", "100.00"))),
        Calculation("0.00", "0.00", "0.00", "300.00"),
        withinFreeAllowance = false,
        limits = Map.empty
      ).asDto(applySorting = true)


      calculatorResponseDto.items.map(_.metadata.description) shouldBe List(
        "an alcohol item",
        "a tobacco item",
        "an other-goods item",
        "an alcohol item with duty",
        "a tobacco item with duty",
        "an other-goods item with duty"
      )
    }
  }
}

