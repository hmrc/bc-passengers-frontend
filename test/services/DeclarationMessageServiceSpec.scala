package services

import helpers.Helpers.SchemaValidator
import models._
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import play.api.libs.json.{JsNull, Json}
import util.BaseSpec
import helpers.Helpers._

class DeclarationMessageServiceSpec extends BaseSpec {

  val declarationMessageService: DeclarationMessageService = app.injector.instanceOf[DeclarationMessageService]

  val journeyDataToTransform = JourneyData(None,Some(false),Some(true),List(),
    List(
      PurchasedProductInstance(ProductPath("other-goods/electronic-devices/televisions"),"ZFkad1",None,None,Some(Country("United States of America (the)", "US", isEu = false, Some("USD"))),Some("USD"),Some(1500)),
      PurchasedProductInstance(ProductPath("other-goods/electronic-devices/televisions"),"ywLQaN",None,None,Some(Country("United States of America (the)", "US", isEu = false, Some("USD"))),Some("GBP"),Some(1300)),
      PurchasedProductInstance(ProductPath("alcohol/cider"),"Jh9VCz",Some(5),None,Some(Country("United States of America (the)", "US", isEu = false, Some("USD"))),Some("USD"),Some(120)),
      PurchasedProductInstance(ProductPath("tobacco/cigarettes"),"6yydJR",None,Some(250),Some(Country("United States of America (the)", "US", isEu = false, Some("USD"))),Some("USD"),Some(400)),
      PurchasedProductInstance(ProductPath("tobacco/rolling"),"HK0nPG",Some(0.12),None,Some(Country("United States of America (the)", "US", isEu = false, Some("USD"))),Some("USD"),Some(200))),
    None,
    None,
    Some(CalculatorResponse(Some(Alcohol(List(Band("B",List(Item("ALC/A1/CIDER", "91.23",None,Some(5), Calculation("2.00","0.30","18.70","21.00"),Metadata("5 litres cider", "Cider", "120.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))), Calculation("2.00","0.30","18.70","21.00"))), Calculation("2.00","0.30","18.70","21.00"))),
      Some(Tobacco(List(Band("B",List(Item("TOB/A1/CIGRT","304.11",Some(250),None, Calculation("74.00","79.06","91.43","244.49"),Metadata("250 cigarettes", "Ciagerettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))), Item("TOB/A1/HAND","152.05",Some(0),Some(0.12), Calculation("26.54","113.88","58.49","198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))), Calculation("100.54","192.94","149.92","443.40"))), Calculation("100.54","192.94","149.92","443.40"))),
      Some(OtherGoods(List(Band("C",List(Item("OGD/DIGI/TV","1140.42",None,None,
      Calculation("0.00","159.65","260.01","419.66"),Metadata("Televisions", "Televisions","1500.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))), Item("OGD/DIGI/TV","1300.00",None,None,
      Calculation("0.00","182.00","296.40","478.40"),Metadata("Televisions", "Televisions","1300.00",Currency("GBP", "British Pound (GBP)", None), Country("United Kingdom of Great Britain and Northern Ireland (the)", "GB", isEu = true, None), ExchangeRate("1.20", "2018-10-29")))),
      Calculation("0.00","341.65","556.41","898.06"))),
      Calculation("0.00","341.65","556.41","898.06"))),Calculation("102.54","534.89","725.03","1362.46"))))



  "Calling DeclarationMessageService.declarationFromJourneyData" should {



    "fail if it does not match the schema format rules (acknowledgement reference too long)" in {
      val journeyData = JourneyData(
        userInformation = Some(UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-05-31"))),
        calculatorResponse = Some(CalculatorResponse(
          alcohol = None,
          otherGoods = None,
          tobacco = Some(Tobacco(
            List(
              Band("A",
                List(
                  Item("TOB/A1/CIGRT", "250.10", Some(200), None, Calculation("0.00","0.00","0.00","0.00"), Metadata("200 cigarettes", "Cigarettes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              ),
              Band("B",
                List(
                  Item("TOB/A1/CIGRT","304.11",Some(250),None, Calculation("74.00","79.06","91.43","244.49"),Metadata("250 cigarettes", "Ciagarettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))),
                  Item("TOB/A1/HAND","152.05",Some(0),Some(BigDecimal("0.12")), Calculation("26.54","113.88","58.49","198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("100.54","192.94","149.92","443.40")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          calculation = Calculation("102.54", "192.94", "149.92", "443.40")
        ))
      )

      val dm = declarationMessageService.declarationMessage(
        ChargeReference("XAPR0123456789"),
        journeyData,
        DateTime.parse("2018-05-31T13:14:08"),
        "e6469aeacf754dc4bd2d2d6800b37d9487hd289"
      )

      val validator = new SchemaValidator("/PAS02 Request 02-11-2018.json")

      validator.validate(dm).errors.map(_.toString) should contain ("$.simpleDeclarationRequest.requestCommon.acknowledgementReference: may only be 32 characters long")
    }

    "fail if there is any required information missing (empty passport number)" in {
      val journeyData = JourneyData(
        userInformation = Some(UserInformation("Harry", "Potter", "", "Heathrow", LocalDate.parse("2018-05-31"))),
        calculatorResponse = Some(CalculatorResponse(
          alcohol = None,
          otherGoods = None,
          tobacco = Some(Tobacco(
            List(
              Band("A",
                List(
                  Item("TOB/A1/CIGRT", "250.10", Some(200), None, Calculation("0.00","0.00","0.00","0.00"), Metadata("200 cigarettes", "Cigarettes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              ),
              Band("B",
                List(
                  Item("TOB/A1/CIGRT","304.11",Some(250),None, Calculation("74.00","79.06","91.43","244.49"),Metadata("250 cigarettes", "Cigarettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))),
                  Item("TOB/A1/HAND","152.05",Some(0),Some(BigDecimal("0.12")), Calculation("26.54","113.88","58.49","198.91"), Metadata("120g rolling tobacco", "Rolling tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("100.54","192.94","149.92","443.40")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          calculation = Calculation("102.54", "192.94", "149.92", "443.40")
        ))
      )

      val dm = declarationMessageService.declarationMessage(
        ChargeReference("XAPR0123456789"),
        journeyData,
        DateTime.parse("2018-05-31T13:14:08"),
        "e6469aeacf754dc4bd2d2d6800b37d94"
      )

      val validator = new SchemaValidator("/PAS02 Request 02-11-2018.json")

      validator.validate(dm).errors.map(_.toString) should contain ("$.simpleDeclarationRequest.requestDetail.customerReference.passport: must be at least 1 characters long")
    }


    "populate the data for journey data containing the collected user information" in {

      lazy val journeyData = JourneyData(
        userInformation = Some(UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-10-31")))
      )

      declarationMessageService.declarationMessage(
        ChargeReference("XAPR0123456789"),
        journeyData,
        DateTime.parse("2018-05-31T13:14:08"),
        "e6469aeacf754dc4bd2d2d6800b37d94" //UUID.randomUUID().toString.filter(_ != '-')
      ) shouldEqual Json.obj(
        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "acknowledgementReference" -> "e6469aeacf754dc4bd2d2d6800b37d94",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("passport" -> "123456789"),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "declarationHeader" -> Json.obj("chargeReference" -> "XAPR0123456789", "portOfEntry" -> "Heathrow", "expectedDateOfArrival" -> "2018-10-31")
          )
        )
      )
    }

    "generate the correct payload and adhere to the schema when journeyData contains required data for only tobacco products" in {

      val journeyData = JourneyData(
        userInformation = Some(UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-05-31"))),
        calculatorResponse = Some(CalculatorResponse(
          alcohol = None,
          otherGoods = None,
          tobacco = Some(Tobacco(
            List(
              Band("A",
                List(
                  Item("TOB/A1/CIGRT", "250.10", Some(200), None, Calculation("0.00","0.00","0.00","0.00"), Metadata("200 cigarettes", "Cigarettes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              ),
              Band("B",
                List(
                  Item("TOB/A1/CIGRT","304.11",Some(250),None, Calculation("74.00","79.06","91.43","244.49"),Metadata("250 cigarettes", "Cigarettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))),
                  Item("TOB/A1/HAND","152.05",Some(0),Some(BigDecimal("0.12")), Calculation("26.54","113.88","58.49","198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("100.54","192.94","149.92","443.40")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          calculation = Calculation("102.54", "192.94", "149.92", "443.40")
        ))
      )

      val dm = declarationMessageService.declarationMessage(
        ChargeReference("XAPR0123456789"),
        journeyData,
        DateTime.parse("2018-05-31T13:14:08"),
        "e6469aeacf754dc4bd2d2d6800b37d94" //UUID.randomUUID().toString.filter(_ != '-')
      )


      val validator = new SchemaValidator("/PAS02 Request 02-11-2018.json")
      validator.validate(dm).errors shouldBe 'empty

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "acknowledgementReference" -> "e6469aeacf754dc4bd2d2d6800b37d94",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("passport" -> "123456789"),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "declarationHeader" -> Json.obj("chargeReference" -> "XAPR0123456789", "portOfEntry" -> "Heathrow", "expectedDateOfArrival" -> "2018-05-31"),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco" -> "100.54",
              "totalCustomsTobacco" -> "192.94",
              "totalVATTobacco" -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "goodsValue" -> "300.00",
                  "quantity" -> "200",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "goodsValue" -> "400.00",
                  "quantity" -> "250",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight" -> "120.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

    "truncate a product description to 40 characters if the product description is too big in the metadata." in {

      val journeyData = JourneyData(
        userInformation = Some(UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-05-31"))),
        calculatorResponse = Some(CalculatorResponse(
          alcohol = Some(Alcohol(
            List(
              Band("A",
                List(
                  Item("ALC/A1/CIDER", "250.10", None, Some(BigDecimal("2.00")), Calculation("0.00","0.00","0.00","0.00"), Metadata("2 litres cider", "Cider but for some reason has a really long product description", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          otherGoods = None,
          tobacco = None,
          calculation = Calculation("102.54", "192.94", "149.92", "443.40")
        ))
      )

      val dm = declarationMessageService.declarationMessage(
        ChargeReference("XAPR0123456789"),
        journeyData,
        DateTime.parse("2018-05-31T13:14:08"),
        "e6469aeacf754dc4bd2d2d6800b37d94" //UUID.randomUUID().toString.filter(_ != '-')
      )


      val validator = new SchemaValidator("/PAS02 Request 02-11-2018.json")
      validator.validate(dm).errors shouldBe 'empty

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "acknowledgementReference" -> "e6469aeacf754dc4bd2d2d6800b37d94",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("passport" -> "123456789"),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "declarationHeader" -> Json.obj("chargeReference" -> "XAPR0123456789", "portOfEntry" -> "Heathrow", "expectedDateOfArrival" -> "2018-05-31"),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol" -> "100.54",
              "totalCustomsAlcohol" -> "192.94",
              "totalVATAlcohol" -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider but for some reason has a really l",
                  "volume" -> "2.00",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

    "generate the correct payload and adhere to the schema when journeyData contains required data for only alcohol products" in {

      val journeyData = JourneyData(
        userInformation = Some(UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-05-31"))),
        calculatorResponse = Some(CalculatorResponse(
          alcohol = Some(Alcohol(
            List(
              Band("A",
                List(
                  Item("ALC/A1/CIDER", "250.10", None, Some(BigDecimal("2.00")), Calculation("0.00","0.00","0.00","0.00"), Metadata("2 litres cider", "Cider", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              ),
              Band("B",
                List(
                  Item("ALC/A2/BEER","304.11", None, Some(BigDecimal("3.00")), Calculation("74.00","79.06","91.43","244.49"),Metadata("3 litres beer", "Beer", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))),
                  Item("ALC/A3/WINE","152.05", None, Some(BigDecimal("4.00")), Calculation("26.54","113.88","58.49","198.91"), Metadata("4 litres wine", "Wine", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("100.54","192.94","149.92","443.40")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          otherGoods = None,
          tobacco = None,
          calculation = Calculation("102.54", "192.94", "149.92", "443.40")
        ))
      )

      val dm = declarationMessageService.declarationMessage(
        ChargeReference("XAPR0123456789"),
        journeyData,
        DateTime.parse("2018-05-31T13:14:08"),
        "e6469aeacf754dc4bd2d2d6800b37d94" //UUID.randomUUID().toString.filter(_ != '-')
      )


      val validator = new SchemaValidator("/PAS02 Request 02-11-2018.json")
      validator.validate(dm).errors shouldBe 'empty

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "acknowledgementReference" -> "e6469aeacf754dc4bd2d2d6800b37d94",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("passport" -> "123456789"),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "declarationHeader" -> Json.obj("chargeReference" -> "XAPR0123456789", "portOfEntry" -> "Heathrow", "expectedDateOfArrival" -> "2018-05-31"),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol" -> "100.54",
              "totalCustomsAlcohol" -> "192.94",
              "totalVATAlcohol" -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider",
                  "volume" -> "2.00",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Beer",
                  "volume" -> "3.00",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Wine",
                  "volume" -> "4.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

    "generate the correct payload and adhere to the schema when journeyData contains required data for only other-goods products" in {

      val journeyData = JourneyData(
        userInformation = Some(UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-05-31"))),
        calculatorResponse = Some(CalculatorResponse(
          alcohol = None,
          otherGoods = Some(OtherGoods(
            List(
              Band("A",
                List(
                  Item("OGD/CLTHS/CHILD", "250.10", None, None, Calculation("0.00","0.00","0.00","0.00"), Metadata("children's clothes", "Children's Clothes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              ),
              Band("B",
                List(
                  Item("OGD/BKS/MISC","304.11", None, None, Calculation("74.00","79.06","91.43","244.49"),Metadata("books or publications", "Books or Publications", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))),
                  Item("OGD/BKS/MISC","152.05", None, None, Calculation("26.54","113.88","58.49","198.91"), Metadata("books or publications", "Books or Publications", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("100.54","192.94","149.92","443.40")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          tobacco = None,
          calculation = Calculation("102.54", "192.94", "149.92", "443.40")
        ))
      )

      val dm = declarationMessageService.declarationMessage(
        ChargeReference("XAPR0123456789"),
        journeyData,
        DateTime.parse("2018-05-31T13:14:08"),
        "e6469aeacf754dc4bd2d2d6800b37d94"
      )


      val validator = new SchemaValidator("/PAS02 Request 02-11-2018.json")
      validator.validate(dm).errors shouldBe 'empty

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "acknowledgementReference" -> "e6469aeacf754dc4bd2d2d6800b37d94",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("passport" -> "123456789"),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "declarationHeader" -> Json.obj("chargeReference" -> "XAPR0123456789", "portOfEntry" -> "Heathrow", "expectedDateOfArrival" -> "2018-05-31"),
            "declarationOther" -> Json.obj(
              "totalExciseOther" -> "100.54",
              "totalCustomsOther" -> "192.94",
              "totalVATOther" -> "149.92",
              "declarationItemOther" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Children's Clothes",
                  "quantity" -> "1",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity" -> "1",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity" -> "1",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

    "generate the correct payload and adhere to the schema when journeyData a calculation with all product categories in" in {

      val journeyData = JourneyData(
        userInformation = Some(UserInformation("Harry", "Potter", "123456789", "Heathrow", LocalDate.parse("2018-05-31"))),
        calculatorResponse = Some(CalculatorResponse(
          alcohol = Some(Alcohol(
            List(
              Band("A",
                List(
                  Item("ALC/A1/CIDER", "250.10", None, Some(BigDecimal("2.00")), Calculation("0.00","0.00","0.00","0.00"), Metadata("2 litres cider", "Cider", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              ),
              Band("B",
                List(
                  Item("ALC/A2/BEER","304.11", None, Some(BigDecimal("3.00")), Calculation("74.00","79.06","91.43","244.49"),Metadata("3 litres beer", "Beer", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))),
                  Item("ALC/A3/WINE","152.05", None, Some(BigDecimal("4.00")), Calculation("26.54","113.88","58.49","198.91"), Metadata("4 litres wine", "Wine", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("100.54","192.94","149.92","443.40")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          otherGoods = Some(OtherGoods(
            List(
              Band("A",
                List(
                  Item("OGD/CLTHS/CHILD", "250.10", None, None, Calculation("0.00","0.00","0.00","0.00"), Metadata("children's clothes", "Children's Clothes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              ),
              Band("B",
                List(
                  Item("OGD/BKS/MISC","304.11", None, None, Calculation("74.00","79.06","91.43","244.49"),Metadata("books or publications", "Books or Publications", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))),
                  Item("OGD/BKS/MISC","152.05", None, None, Calculation("26.54","113.88","58.49","198.91"), Metadata("books or publications", "Books or Publications", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("100.54","192.94","149.92","443.40")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          tobacco = Some(Tobacco(
            List(
              Band("A",
                List(
                  Item("TOB/A1/CIGRT", "250.10", Some(200), None, Calculation("0.00","0.00","0.00","0.00"), Metadata("200 cigarettes", "Cigarettes", "300.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("0.00","0.00","0.00","0.00")
              ),
              Band("B",
                List(
                  Item("TOB/A1/CIGRT","304.11",Some(250),None, Calculation("74.00","79.06","91.43","244.49"),Metadata("250 cigarettes", "Cigarettes", "400.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29"))),
                  Item("TOB/A1/HAND","152.05",Some(0),Some(BigDecimal("0.12")), Calculation("26.54","113.88","58.49","198.91"), Metadata("120g rolling tobacco", "Rolling Tobacco", "200.00",Currency("USD", "USA Dollar (USD)", Some("USD")), Country("United States of America (the)", "US", isEu = false, Some("USD")), ExchangeRate("1.20", "2018-10-29")))
                ),
                Calculation("100.54","192.94","149.92","443.40")
              )
            ),
            Calculation("100.54","192.94","149.92","443.40")
          )),
          calculation = Calculation("102.54", "192.94", "149.92", "443.40")
        ))
      )

      val dm = declarationMessageService.declarationMessage(
        ChargeReference("XAPR0123456789"),
        journeyData,
        DateTime.parse("2018-05-31T13:14:08"),
        "e6469aeacf754dc4bd2d2d6800b37d94" //UUID.randomUUID().toString.filter(_ != '-')
      )


      val validator = new SchemaValidator("/PAS02 Request 02-11-2018.json")
      validator.validate(dm).errors shouldBe 'empty

      dm shouldEqual Json.obj(

        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "receiptDate" -> "2018-05-31T12:14:08Z",
            "acknowledgementReference" -> "e6469aeacf754dc4bd2d2d6800b37d94",
            "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
          ),
          "requestDetail" -> Json.obj(
            "customerReference" -> Json.obj("passport" -> "123456789"),
            "personalDetails" -> Json.obj("firstName" -> "Harry", "lastName" -> "Potter"),
            "declarationHeader" -> Json.obj("chargeReference" -> "XAPR0123456789", "portOfEntry" -> "Heathrow", "expectedDateOfArrival" -> "2018-05-31"),
            "declarationTobacco" -> Json.obj(
              "totalExciseTobacco" -> "100.54",
              "totalCustomsTobacco" -> "192.94",
              "totalVATTobacco" -> "149.92",
              "declarationItemTobacco" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity" -> "200",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Cigarettes",
                  "quantity" -> "250",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Rolling Tobacco",
                  "weight" -> "120.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "declarationAlcohol" -> Json.obj(
              "totalExciseAlcohol" -> "100.54",
              "totalCustomsAlcohol" -> "192.94",
              "totalVATAlcohol" -> "149.92",
              "declarationItemAlcohol" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Cider",
                  "volume" -> "2.00",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Beer",
                  "volume" -> "3.00",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Wine",
                  "volume" -> "4.00",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "declarationOther" -> Json.obj(
              "totalExciseOther" -> "100.54",
              "totalCustomsOther" -> "192.94",
              "totalVATOther" -> "149.92",
              "declarationItemOther" -> Seq(
                Json.obj(
                  "commodityDescription" -> "Children's Clothes",
                  "quantity" -> "1",
                  "goodsValue" -> "300.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "250.10",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "0.00",
                  "customsGBP" -> "0.00",
                  "vatGBP" -> "0.00"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity" -> "1",
                  "goodsValue" -> "400.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "304.11",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "74.00",
                  "customsGBP" -> "79.06",
                  "vatGBP" -> "91.43"
                ),
                Json.obj(
                  "commodityDescription" -> "Books or Publications",
                  "quantity" -> "1",
                  "goodsValue" -> "200.00",
                  "valueCurrency" -> "USD",
                  "originCountry" -> "US",
                  "exchangeRate" -> "1.20",
                  "exchangeRateDate" -> "2018-10-29",
                  "customsValueGBP" -> "152.05",
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> "26.54",
                  "customsGBP" -> "113.88",
                  "vatGBP" -> "58.49"
                )
              )
            ),
            "liabilityDetails" -> Json.obj(
              "totalExciseGBP" -> "102.54",
              "totalCustomsGBP" -> "192.94",
              "totalVATGBP" -> "149.92",
              "grandTotalGBP" -> "443.40"
            )
          )
        )
      )
    }

  }
}
