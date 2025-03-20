/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}
import util.BaseSpec

class CalculatorResponseSpec extends BaseSpec {

  "Calling CalculatorResponse.getItemsWithTaxToPay" should {

    trait LocalSetup {

      lazy val cr: CalculatorResponse = CalculatorResponse(
        Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ANYTHING",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("100.00", "0.00", "0.00", "100.00"),
                    Metadata(
                      "Desc",
                      "Desc",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "US Dollars", Some("USD"), Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("100.00", "0.00", "0.00", "100.00")
              )
            ),
            Calculation("100.00", "0.00", "0.00", "100.00")
          )
        ),
        Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ANYTHING",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "Desc",
                      "Desc",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "US Dollars", Some("USD"), Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Some(
          OtherGoods(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ANYTHING",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "Desc",
                      "Desc",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("USD", "US Dollars", Some("USD"), Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Calculation("100.00", "0.00", "0.00", "100.00"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )
    }

    "return a list of all the items where tax liability is Zero pounds or the tax is payable" in new LocalSetup {

      cr.getItemsWithTaxToPay shouldBe List(
        Item(
          "ANYTHING",
          "100.00",
          Some(1),
          None,
          Calculation("100.00", "0.00", "0.00", "100.00"),
          Metadata(
            "Desc",
            "Desc",
            "100.00",
            DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
            Currency("USD", "US Dollars", Some("USD"), List()),
            Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, List()),
            ExchangeRate("1.20", "2018-10-29"),
            None
          ),
          None,
          None,
          None,
          None
        ),
        Item(
          "ANYTHING",
          "100.00",
          Some(1),
          None,
          Calculation("0.00", "0.00", "0.00", "0.00"),
          Metadata(
            "Desc",
            "Desc",
            "100.00",
            DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
            Currency("USD", "US Dollars", Some("USD"), List()),
            Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, List()),
            ExchangeRate("1.20", "2018-10-29"),
            None
          ),
          None,
          None,
          None,
          None
        ),
        Item(
          "ANYTHING",
          "100.00",
          Some(1),
          None,
          Calculation("0.00", "0.00", "0.00", "0.00"),
          Metadata(
            "Desc",
            "Desc",
            "100.00",
            DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
            Currency("USD", "US Dollars", Some("USD"), List()),
            Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, List()),
            ExchangeRate("1.20", "2018-10-29"),
            None
          ),
          None,
          None,
          None,
          None
        )
      )
    }
  }

  "Calling CalculatorResponse.onlyGBP" should {

    trait LocalSetup {
      def alcoholCurrency: Currency

      def tobaccoCurrency: Currency

      def otherGoodsCurrency: Currency

      lazy val cr: CalculatorResponse = CalculatorResponse(
        Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ANYTHING",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "Desc",
                      "Desc",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      alcoholCurrency,
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ANYTHING",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "Desc",
                      "Desc",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      tobaccoCurrency,
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Some(
          OtherGoods(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ANYTHING",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "Desc",
                      "Desc",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      otherGoodsCurrency,
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Calculation("0.00", "0.00", "0.00", "0.00"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
      )
    }

    "work with alcohol not GBP" in new LocalSetup {

      override lazy val alcoholCurrency: Currency    = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"), Nil)
      override lazy val tobaccoCurrency: Currency    = Currency("GBP", "British Pound (GBP)", None, Nil)
      override lazy val otherGoodsCurrency: Currency = Currency("GBP", "British Pound (GBP)", None, Nil)

      cr.allItemsUseGBP shouldBe false
    }

    "work with tobacco not GBP" in new LocalSetup {

      override lazy val alcoholCurrency: Currency    = Currency("GBP", "British Pound (GBP)", None, Nil)
      override lazy val tobaccoCurrency: Currency    = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"), Nil)
      override lazy val otherGoodsCurrency: Currency = Currency("GBP", "British Pound (GBP)", None, Nil)

      cr.allItemsUseGBP shouldBe false
    }

    "work with other goods not GBP" in new LocalSetup {

      override lazy val alcoholCurrency: Currency    = Currency("GBP", "British Pound (GBP)", None, Nil)
      override lazy val tobaccoCurrency: Currency    = Currency("GBP", "British Pound (GBP)", None, Nil)
      override lazy val otherGoodsCurrency: Currency = Currency("UGX", "Uganda Schilling (UGX)", Some("UGX"), Nil)

      cr.allItemsUseGBP shouldBe false
    }

    "work when all GBP" in new LocalSetup {

      override lazy val alcoholCurrency: Currency    = Currency("GBP", "British Pound (GBP)", None, Nil)
      override lazy val tobaccoCurrency: Currency    = Currency("GBP", "British Pound (GBP)", None, Nil)
      override lazy val otherGoodsCurrency: Currency = Currency("GBP", "British Pound (GBP)", None, Nil)

      cr.allItemsUseGBP shouldBe true
    }
  }

  "Calling CalculatorResponse.asDto" should {

    "order products correctly when no tax is due" in {

      val calculatorResponseDto = CalculatorResponse(
        Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ALC",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "an alcohol item",
                      "an alcohol item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "ALC",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "another alcohol item",
                      "another alcohol item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "TOB",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "a tobacco item",
                      "a tobacco item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Some(
          OtherGoods(
            List(
              Band(
                "A",
                List(
                  Item(
                    "OGD",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "an other-goods item",
                      "an other-goods item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Calculation("0.00", "0.00", "0.00", "100.00"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = false
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
        Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ALC",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "an alcohol item",
                      "an alcohol item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "ALC",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "100.00"),
                    Metadata(
                      "an alcohol item with duty",
                      "an alcohol item with duty",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "100.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "100.00")
          )
        ),
        Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "TOB",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "a tobacco item",
                      "a tobacco item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Some(
          OtherGoods(
            List(
              Band(
                "A",
                List(
                  Item(
                    "OGD",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "an other-goods item",
                      "an other-goods item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "0.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "0.00")
          )
        ),
        Calculation("0.00", "0.00", "0.00", "100.00"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = false
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
        Some(
          Alcohol(
            List(
              Band(
                "A",
                List(
                  Item(
                    "ALC",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "an alcohol item",
                      "an alcohol item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "ALC",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "100.00"),
                    Metadata(
                      "an alcohol item with duty",
                      "an alcohol item with duty",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "100.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "100.00")
          )
        ),
        Some(
          Tobacco(
            List(
              Band(
                "A",
                List(
                  Item(
                    "TOB",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "a tobacco item",
                      "a tobacco item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "TOB",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "100.00"),
                    Metadata(
                      "a tobacco item with duty",
                      "a tobacco item with duty",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "100.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "100.00")
          )
        ),
        Some(
          OtherGoods(
            List(
              Band(
                "A",
                List(
                  Item(
                    "OGD",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "0.00"),
                    Metadata(
                      "an other-goods item",
                      "an other-goods item",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  ),
                  Item(
                    "OGD",
                    "100.00",
                    Some(1),
                    None,
                    Calculation("0.00", "0.00", "0.00", "100.00"),
                    Metadata(
                      "an other-goods item with duty",
                      "an other-goods item with duty",
                      "100.00",
                      DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                      Currency("GBP", "British Pound (GBP)", None, Nil),
                      Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                      ExchangeRate("1.20", "2018-10-29"),
                      None
                    ),
                    None,
                    None,
                    None,
                    None
                  )
                ),
                Calculation("0.00", "0.00", "0.00", "100.00")
              )
            ),
            Calculation("0.00", "0.00", "0.00", "100.00")
          )
        ),
        Calculation("0.00", "0.00", "0.00", "300.00"),
        withinFreeAllowance = false,
        limits = Map.empty,
        isAnyItemOverAllowance = true
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
  private val calculation: Calculation = Calculation(
    excise = "0.00",
    customs = "0.00",
    vat = "0.00",
    allTax = "0.00"
  )

  private val json: JsObject = Json.obj(
    "excise"  -> "0.00",
    "customs" -> "0.00",
    "vat"     -> "0.00",
    "allTax"  -> "0.00"
  )
  "Calculation" should {
    "serialise to JSON" when {
      "all fields are valid" in {
        Json.toJson(calculation) shouldBe json
      }
    }

    "deserialise from JSON" when {

      "all fields are valid" in {
        json.as[Calculation] shouldBe calculation
      }

      "there is type mismatch" in {
        val json = Json.obj(
          "rate" -> true,
          "date" -> true
        )
        json.validate[ExchangeRate] shouldBe a[JsError]
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[Calculation] shouldBe a[JsError]
      }
    }
  }

  "ExchangeRate" should {

    val exchangeRate = ExchangeRate(
      rate = "1.00",
      date = "2018-10-29"
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(exchangeRate) shouldBe Json.obj(
          "rate" -> "1.00",
          "date" -> "2018-10-29"
        )
      }

      "all fields are blank" in {
        val exchangeRateWithEmptyStrings = exchangeRate.copy("", "")

        Json.toJson(exchangeRateWithEmptyStrings) shouldBe Json.obj(
          "rate" -> "",
          "date" -> ""
        )
      }

      "one of the fields are missing" in {
        val exchangeRateWithMissingField = exchangeRate.copy(rate = "", date = "2018-10-29")

        Json.toJson(exchangeRateWithMissingField) shouldBe Json.obj(
          "rate" -> "",
          "date" -> "2018-10-29"
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "rate" -> "1.00",
          "date" -> "2018-10-29"
        )
        json.validate[ExchangeRate] shouldBe JsSuccess(exchangeRate)
      }

      "there is type mismatch" in {
        val json = Json.obj(
          "rate" -> true,
          "date" -> true
        )
        json.validate[ExchangeRate] shouldBe a[JsError]
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[ExchangeRate] shouldBe a[JsError]
      }
      "error when JSON is invalid" in {
        Json.arr().validate[ExchangeRate] shouldBe a[JsError]
      }
    }
  }

  "Metadata" should {

    val metadata = Metadata(
      description = "description",
      name = "name",
      cost = "00.00",
      descriptionLabels = DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
      currency = Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
      country = Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
      exchangeRate = ExchangeRate("1.20", "2018-10-29"),
      originCountry = None
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(metadata) shouldBe Json.obj(
          "name"              -> "name",
          "description"       -> "description",
          "country"           -> Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
          "exchangeRate"      -> ExchangeRate("1.20", "2018-10-29"),
          "cost"              -> "00.00",
          "descriptionLabels" -> DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
          "currency"          -> Currency("USD", "USA dollars (USD)", Some("USD"), Nil)
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "description"       -> "description",
          "name"              -> "name",
          "cost"              -> "00.00",
          "descriptionLabels" -> DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
          "currency"          -> Currency("USD", "USA dollars (USD)", Some("USD"), Nil),
          "country"           -> Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
          "exchangeRate"      -> ExchangeRate("1.20", "2018-10-29"),
          "originCountry"     -> None
        )
        json.validate[Metadata] shouldBe JsSuccess(metadata)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[Metadata] shouldBe a[JsError]
      }

      "error when JSON is invalid" in {
        Json.arr().validate[Metadata] shouldBe a[JsError]
      }
    }
  }

  "Item" should {

    val item = Item(
      rateId = "rateId",
      purchaseCost = "purchaseCost",
      noOfUnits = Some(100),
      weightOrVolume = None,
      calculation = Calculation("0.00", "0.00", "0.00", "0.00"),
      metadata = Metadata(
        "Desc",
        "Desc",
        "100.00",
        DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
        Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
        Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
        ExchangeRate("1.20", "2018-10-29"),
        None
      ),
      isVatPaid = None,
      isCustomPaid = None,
      isExcisePaid = None,
      isUccRelief = None
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(item) shouldBe Json.obj(
          "noOfUnits"    -> Some(100),
          "calculation"  -> Calculation("0.00", "0.00", "0.00", "0.00"),
          "metadata"     -> Metadata(
            "Desc",
            "Desc",
            "100.00",
            DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
            Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
            Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),
            None
          ),
          "purchaseCost" -> "purchaseCost",
          "rateId"       -> "rateId"
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "noOfUnits"    -> Some(100),
          "calculation"  -> Calculation("0.00", "0.00", "0.00", "0.00"),
          "metadata"     -> Metadata(
            "Desc",
            "Desc",
            "100.00",
            DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
            Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
            Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),
            None
          ),
          "purchaseCost" -> "purchaseCost",
          "rateId"       -> "rateId"
        )
        json.validate[Item] shouldBe JsSuccess(item)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[Item] shouldBe a[JsError]
      }

      "error when JSON is invalid" in {
        Json.arr().validate[Item] shouldBe a[JsError]
      }
    }
  }

  "Band" should {

    val band = Band(
      code = "A",
      items = List(
        Item(
          "ANYTHING",
          "100.00",
          Some(1),
          None,
          Calculation("0.00", "0.00", "0.00", "0.00"),
          Metadata(
            "Desc",
            "Desc",
            "100.00",
            DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
            Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
            Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
            ExchangeRate("1.20", "2018-10-29"),
            None
          ),
          None,
          None,
          None,
          None
        )
      ),
      calculation = Calculation("0.00", "0.00", "0.00", "0.00")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(band) shouldBe Json.obj(
          "code"        -> "A",
          "items"       -> List(
            Item(
              "ANYTHING",
              "100.00",
              Some(1),
              None,
              Calculation("0.00", "0.00", "0.00", "0.00"),
              Metadata(
                "Desc",
                "Desc",
                "100.00",
                DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                ExchangeRate("1.20", "2018-10-29"),
                None
              ),
              None,
              None,
              None,
              None
            )
          ),
          "calculation" -> Calculation("0.00", "0.00", "0.00", "0.00")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "code"        -> "A",
          "items"       -> List(
            Item(
              "ANYTHING",
              "100.00",
              Some(1),
              None,
              Calculation("0.00", "0.00", "0.00", "0.00"),
              Metadata(
                "Desc",
                "Desc",
                "100.00",
                DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                ExchangeRate("1.20", "2018-10-29"),
                None
              ),
              None,
              None,
              None,
              None
            )
          ),
          "calculation" -> Calculation("0.00", "0.00", "0.00", "0.00")
        )
        json.validate[Band] shouldBe JsSuccess(band)
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[Band] shouldBe a[JsError]
      }

      "error when JSON is invalid" in {
        Json.arr().validate[Band] shouldBe a[JsError]
      }
    }
  }

  "Alcohol" should {

    val alcohol = Alcohol(
      bands = List(
        Band(
          "A",
          List(
            Item(
              "ANYTHING",
              "100.00",
              Some(1),
              None,
              Calculation("0.00", "0.00", "0.00", "0.00"),
              Metadata(
                "Desc",
                "Desc",
                "100.00",
                DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                ExchangeRate("1.20", "2018-10-29"),
                None
              ),
              None,
              None,
              None,
              None
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      calculation = Calculation("0.00", "0.00", "0.00", "0.00")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(alcohol) shouldBe Json.obj(
          "bands"       -> List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "calculation" -> Calculation("0.00", "0.00", "0.00", "0.00")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "bands"       -> List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "calculation" -> Calculation("0.00", "0.00", "0.00", "0.00")
        )
        json.validate[Alcohol] shouldBe JsSuccess(alcohol)
      }

      "error when JSON is invalid" in {
        Json.arr().validate[Alcohol] shouldBe a[JsError]
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[Alcohol] shouldBe a[JsError]
      }
    }
  }

  "Tobacco" should {

    val tobacco = Tobacco(
      bands = List(
        Band(
          "T",
          List(
            Item(
              "ANYTHING",
              "100.00",
              Some(1),
              None,
              Calculation("0.00", "0.00", "0.00", "0.00"),
              Metadata(
                "Desc",
                "Desc",
                "100.00",
                DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                ExchangeRate("1.20", "2018-10-29"),
                None
              ),
              None,
              None,
              None,
              None
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      calculation = Calculation("0.00", "0.00", "0.00", "0.00")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(tobacco) shouldBe Json.obj(
          "bands"       -> List(
            Band(
              "T",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "calculation" -> Calculation("0.00", "0.00", "0.00", "0.00")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "bands"       -> List(
            Band(
              "T",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "calculation" -> Calculation("0.00", "0.00", "0.00", "0.00")
        )

        json.validate[Tobacco] shouldBe JsSuccess(tobacco)
      }

      "error when JSON is invalid" in {
        Json.arr().validate[Tobacco] shouldBe a[JsError]
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[Tobacco] shouldBe a[JsError]
      }
    }
  }

  "Other Goods" should {

    val otherGoods = OtherGoods(
      bands = List(
        Band(
          "O",
          List(
            Item(
              "ANYTHING",
              "100.00",
              Some(1),
              None,
              Calculation("0.00", "0.00", "0.00", "0.00"),
              Metadata(
                "Desc",
                "Desc",
                "100.00",
                DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                ExchangeRate("1.20", "2018-10-29"),
                None
              ),
              None,
              None,
              None,
              None
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      calculation = Calculation("0.00", "0.00", "0.00", "0.00")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(otherGoods) shouldBe Json.obj(
          "bands"       -> List(
            Band(
              "O",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "calculation" -> Calculation("0.00", "0.00", "0.00", "0.00")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "bands"       -> List(
            Band(
              "O",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "calculation" -> Calculation("0.00", "0.00", "0.00", "0.00")
        )

        json.validate[OtherGoods] shouldBe JsSuccess(otherGoods)
      }

      "error when JSON is invalid" in {
        Json.arr().validate[OtherGoods] shouldBe a[JsError]
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[OtherGoods] shouldBe a[JsError]
      }
    }
  }

  "CalculatorResponse" should {

    val calculatorResponse = CalculatorResponse(
      alcohol = Some(
        Alcohol(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      tobacco = Some(
        Tobacco(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      otherGoods = Some(
        OtherGoods(
          List(
            Band(
              "A",
              List(
                Item(
                  "ANYTHING",
                  "100.00",
                  Some(1),
                  None,
                  Calculation("0.00", "0.00", "0.00", "0.00"),
                  Metadata(
                    "Desc",
                    "Desc",
                    "100.00",
                    DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                    Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                    Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                    ExchangeRate("1.20", "2018-10-29"),
                    None
                  ),
                  None,
                  None,
                  None,
                  None
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          Calculation("0.00", "0.00", "0.00", "0.00")
        )
      ),
      calculation = Calculation("0.00", "0.00", "0.00", "9.00"),
      withinFreeAllowance = true,
      limits = Map.empty,
      isAnyItemOverAllowance = false
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(calculatorResponse) shouldBe Json.obj(
          "withinFreeAllowance"    -> true,
          "otherGoods"             -> Some(
            OtherGoods(
              List(
                Band(
                  "A",
                  List(
                    Item(
                      "ANYTHING",
                      "100.00",
                      Some(1),
                      None,
                      Calculation("0.00", "0.00", "0.00", "0.00"),
                      Metadata(
                        "Desc",
                        "Desc",
                        "100.00",
                        DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                        Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                        Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                        ExchangeRate("1.20", "2018-10-29"),
                        None
                      ),
                      None,
                      None,
                      None,
                      None
                    )
                  ),
                  Calculation("0.00", "0.00", "0.00", "0.00")
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "calculation"            -> Calculation("0.00", "0.00", "0.00", "9.00"),
          "tobacco"                -> Some(
            Tobacco(
              List(
                Band(
                  "A",
                  List(
                    Item(
                      "ANYTHING",
                      "100.00",
                      Some(1),
                      None,
                      Calculation("0.00", "0.00", "0.00", "0.00"),
                      Metadata(
                        "Desc",
                        "Desc",
                        "100.00",
                        DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                        Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                        Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                        ExchangeRate("1.20", "2018-10-29"),
                        None
                      ),
                      None,
                      None,
                      None,
                      None
                    )
                  ),
                  Calculation("0.00", "0.00", "0.00", "0.00")
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "isAnyItemOverAllowance" -> false,
          "alcohol"                -> Some(
            Alcohol(
              List(
                Band(
                  "A",
                  List(
                    Item(
                      "ANYTHING",
                      "100.00",
                      Some(1),
                      None,
                      Calculation("0.00", "0.00", "0.00", "0.00"),
                      Metadata(
                        "Desc",
                        "Desc",
                        "100.00",
                        DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                        Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                        Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                        ExchangeRate("1.20", "2018-10-29"),
                        None
                      ),
                      None,
                      None,
                      None,
                      None
                    )
                  ),
                  Calculation("0.00", "0.00", "0.00", "0.00")
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "limits"                 -> Json.toJson(Map.empty[String, String])
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "withinFreeAllowance"    -> true,
          "otherGoods"             -> Some(
            OtherGoods(
              List(
                Band(
                  "A",
                  List(
                    Item(
                      "ANYTHING",
                      "100.00",
                      Some(1),
                      None,
                      Calculation("0.00", "0.00", "0.00", "0.00"),
                      Metadata(
                        "Desc",
                        "Desc",
                        "100.00",
                        DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                        Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                        Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                        ExchangeRate("1.20", "2018-10-29"),
                        None
                      ),
                      None,
                      None,
                      None,
                      None
                    )
                  ),
                  Calculation("0.00", "0.00", "0.00", "0.00")
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "calculation"            -> Calculation("0.00", "0.00", "0.00", "9.00"),
          "tobacco"                -> Some(
            Tobacco(
              List(
                Band(
                  "A",
                  List(
                    Item(
                      "ANYTHING",
                      "100.00",
                      Some(1),
                      None,
                      Calculation("0.00", "0.00", "0.00", "0.00"),
                      Metadata(
                        "Desc",
                        "Desc",
                        "100.00",
                        DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                        Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                        Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                        ExchangeRate("1.20", "2018-10-29"),
                        None
                      ),
                      None,
                      None,
                      None,
                      None
                    )
                  ),
                  Calculation("0.00", "0.00", "0.00", "0.00")
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "isAnyItemOverAllowance" -> false,
          "alcohol"                -> Some(
            Alcohol(
              List(
                Band(
                  "A",
                  List(
                    Item(
                      "ANYTHING",
                      "100.00",
                      Some(1),
                      None,
                      Calculation("0.00", "0.00", "0.00", "0.00"),
                      Metadata(
                        "Desc",
                        "Desc",
                        "100.00",
                        DescriptionLabels("label.Xg_of_X", List("200", "label.tobacco.rolling-tobacco")),
                        Currency("USD", "USA Dollar (USD)", Some("USD"), Nil),
                        Country("US", "United States of America (the)", "US", isEu = false, isCountry = true, Nil),
                        ExchangeRate("1.20", "2018-10-29"),
                        None
                      ),
                      None,
                      None,
                      None,
                      None
                    )
                  ),
                  Calculation("0.00", "0.00", "0.00", "0.00")
                )
              ),
              Calculation("0.00", "0.00", "0.00", "0.00")
            )
          ),
          "limits"                 -> Json.toJson(Map.empty[String, String])
        )

        json.validate[CalculatorResponse] shouldBe JsSuccess(calculatorResponse)
      }

      "error when JSON is invalid" in {
        Json.arr().validate[CalculatorResponse] shouldBe a[JsError]
      }

      "there is type mismatch" in {
        val json = Json.obj(
          "description" -> true,
          "args"        -> true
        )
        json.validate[CalculatorResponse] shouldBe a[JsError]
      }

      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[CalculatorResponse] shouldBe a[JsError]
      }
    }

    "Description Labels" should {

      val descriptionLabels = DescriptionLabels(
        description = "label.Xg_of_X",
        args = List("200", "label.tobacco.rolling-tobacco")
      )

      "serialize to JSON" when {
        "all fields are valid" in {
          Json.toJson(descriptionLabels) shouldBe Json.obj(
            "description" -> "label.Xg_of_X",
            "args"        -> List("200", "label.tobacco.rolling-tobacco")
          )
        }
      }

      "deserialize from JSON" when {
        "all fields are valid" in {
          val json = Json.obj(
            "description" -> "label.Xg_of_X",
            "args"        -> List("200", "label.tobacco.rolling-tobacco")
          )
          json.validate[DescriptionLabels] shouldBe JsSuccess(descriptionLabels)
        }

        "error when JSON is invalid" in {
          Json.arr().validate[DescriptionLabels] shouldBe a[JsError]
        }

        "there is type mismatch" in {
          val json = Json.obj(
            "description" -> true,
            "args"        -> true
          )
          json.validate[DescriptionLabels] shouldBe a[JsError]
        }

        "an empty JSON object" in {
          val json = Json.obj()
          json.validate[DescriptionLabels] shouldBe a[JsError]
        }
      }
    }
  }

}
