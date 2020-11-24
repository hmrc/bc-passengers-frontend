/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package audit

import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import util.BaseSpec

class AuditingToolsSpec extends BaseSpec{

  class Setup() {

    val service: AuditingTools = app.injector.instanceOf[AuditingTools]

    val json: JsObject = Json.obj(
      "identificationNumber" -> "1234",
      "firstName" -> "Jack",
      "lastName" -> "Jill",
      "emailaddress" -> "jackjill@myemail.com",
      "portOfEntry" -> "LHR",
      "expectedDateOfArrival" -> "2020-08-12",
      "timeOfEntry" -> "08:00",
      "totalExciseAlcohol" -> "80.00",
      "totalCustomsAlcohol" -> "0.00",
      "totalVATAlcohol" -> "1820.23",
      "commodityDescription" -> "Beer",
      "volume" -> "100",
      "goodsValue" -> "10000.00",
      "valueCurrency" -> "EUR",
      "originCountry" -> "ES",
      "exchangeRate" -> "1.1085",
      "exchangeRateDate" -> "2020-11-10",
      "goodsValueGBP" -> "9021.19",
      "VATRESClaimed" -> false,
      "exciseGBP" -> "80.00",
      "customsGBP" -> "0.00",
      "vatGBP" -> "1820.23",
      "totalExciseGBP" -> "80.00",
      "totalCustomsGBP" -> "0.00",
      "totalVATGBP" -> "1820.23",
      "grandTotalGBP" -> "1900.23"
    )
  }

  "buildDeclarationSubmittedDataEvent" should {
    "generate the right audit event details" in new Setup() {
      val result: ExtendedDataEvent = service.buildDeclarationSubmittedDataEvent(json)

      result.auditSource mustBe "bc-passengers-stride-frontend"
      result.auditType mustBe "PassengerDeclarations"
      result.tags("transactionName") mustBe "passenger-declarations-submission"
      result.detail mustBe Json.parse(
        """
          |{
          |  "identificationNumber" : "1234",
          |      "firstName" : "Jack",
          |      "lastName" : "Jill",
          |      "emailaddress" : "jackjill@myemail.com",
          |      "portOfEntry" : "LHR",
          |      "expectedDateOfArrival" : "2020-08-12",
          |      "timeOfEntry" : "08:00",
          |      "totalExciseAlcohol" : "80.00",
          |      "totalCustomsAlcohol" : "0.00",
          |      "totalVATAlcohol" : "1820.23",
          |      "commodityDescription" : "Beer",
          |      "volume" : "100",
          |      "goodsValue" : "10000.00",
          |      "valueCurrency" : "EUR",
          |      "originCountry" : "ES",
          |      "exchangeRate" : "1.1085",
          |      "exchangeRateDate" : "2020-11-10",
          |      "goodsValueGBP" : "9021.19",
          |      "VATRESClaimed" : false,
          |      "exciseGBP" : "80.00",
          |      "customsGBP" : "0.00",
          |      "vatGBP" : "1820.23",
          |      "totalExciseGBP" : "80.00",
          |      "totalCustomsGBP" : "0.00",
          |      "totalVATGBP" : "1820.23",
          |      "grandTotalGBP" : "1900.23"
          |}
          |""".stripMargin)
    }
  }

}
