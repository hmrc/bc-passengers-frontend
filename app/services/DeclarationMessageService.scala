package services

import java.text.DecimalFormat
import java.util.UUID

import util._
import javax.inject.Singleton
import models.{Calculation, ChargeReference, JourneyData, UserInformation}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._

import scala.math.BigDecimal.RoundingMode


@Singleton
class DeclarationMessageService {

  def declarationMessage(chargeReference: ChargeReference, journeyData: JourneyData, receiptDateTime: DateTime, acknowledgementReference: String): JsObject = {

    val customerReference = journeyData.userInformation.fold[JsValue](JsNull)(o => Json.toJson(o)(new Writes[UserInformation] {
      override def writes(o: UserInformation): JsValue = Json.obj("passport" -> o.passportNumber)
    }))

    val personalDetails = journeyData.userInformation.fold[JsValue](JsNull)(o => Json.toJson(o)(new Writes[UserInformation] {
      override def writes(o: UserInformation): JsValue = Json.obj("firstName" -> o.firstName, "lastName" -> o.lastName)
    }))

    val declarationHeader = journeyData.userInformation.fold[JsValue](JsNull)(o => Json.toJson(o)(new Writes[UserInformation] {
      override def writes(o: UserInformation): JsValue = Json.obj("chargeReference" -> chargeReference.value, "portOfEntry" -> o.placeOfArrival, "expectedDateOfArrival" -> o.dateOfArrival)
    }))

    val liabilityDetails = journeyData.calculatorResponse.map(_.calculation).fold[JsValue](JsNull)(o => Json.toJson(o)(new Writes[Calculation] {
      override def writes(o: Calculation): JsValue = Json.obj(
        "totalExciseGBP" -> o.excise,
        "totalCustomsGBP" -> o.customs,
        "totalVATGBP" -> o.vat,
        "grandTotalGBP" -> o.allTax
      )
    }))

    val declarationTobacco = {
      val maybeTobacco = journeyData.calculatorResponse.flatMap(_.tobacco)


      maybeTobacco match {
        case Some(tobacco) => Json.obj(
            "totalExciseTobacco" -> tobacco.calculation.excise,
            "totalCustomsTobacco" -> tobacco.calculation.customs,
            "totalVATTobacco" -> tobacco.calculation.vat,
            "declarationItemTobacco" -> tobacco.bands.flatMap{ band =>
              band.items.map { item =>
                Json.obj(
                  "commodityDescription" -> item.metadata.declarationMessageDescription.take(40),
                  "quantity" -> item.noOfUnits.filter(_ != 0).fold[JsValue](JsNull)(x => JsString(x.toString)),
                  "weight" -> item.weightOrVolume.fold[JsValue](JsNull)(x => JsString((x * 1000).toString())),
                  "goodsValue" -> item.metadata.cost,
                  "valueCurrency" -> item.metadata.currency.code,
                  "originCountry" -> item.metadata.country.alphaTwoCode,
                  "exchangeRate" -> item.metadata.exchangeRate.rate,
                  "exchangeRateDate" -> item.metadata.exchangeRate.date,
                  "customsValueGBP" -> item.purchaseCost,
                  "VATRESClaimed" -> false,
                  "exciseGBP" -> item.calculation.excise,
                  "customsGBP" -> item.calculation.customs,
                  "vatGBP" -> item.calculation.vat
                )
              }
            }
          )
        case None => JsNull
      }
    }

    val declarationAlcohol = {
      val maybeAlcohol = journeyData.calculatorResponse.flatMap(_.alcohol)

      maybeAlcohol match {
        case Some(alcohol) => Json.obj(
          "totalExciseAlcohol" -> alcohol.calculation.excise,
          "totalCustomsAlcohol" -> alcohol.calculation.customs,
          "totalVATAlcohol" -> alcohol.calculation.vat,
          "declarationItemAlcohol" -> alcohol.bands.flatMap{ band =>
            band.items.map { item =>
              Json.obj(
                "commodityDescription" -> item.metadata.declarationMessageDescription.take(40),
                "volume" -> item.weightOrVolume.fold[JsValue](JsNull)(x => JsString(x.toString())),
                "goodsValue" -> item.metadata.cost,
                "valueCurrency" -> item.metadata.currency.code,
                "originCountry" -> item.metadata.country.alphaTwoCode,
                "exchangeRate" -> item.metadata.exchangeRate.rate,
                "exchangeRateDate" -> item.metadata.exchangeRate.date,
                "customsValueGBP" -> item.purchaseCost,
                "VATRESClaimed" -> false,
                "exciseGBP" -> item.calculation.excise,
                "customsGBP" -> item.calculation.customs,
                "vatGBP" -> item.calculation.vat
              )
            }
          }
        )
        case None => JsNull
      }
    }

    val declarationOther = {
      val maybeOtherGoods = journeyData.calculatorResponse.flatMap(_.otherGoods)

      maybeOtherGoods match {
        case Some(other) => Json.obj(
          "totalExciseOther" -> other.calculation.excise,
          "totalCustomsOther" -> other.calculation.customs,
          "totalVATOther" -> other.calculation.vat,
          "declarationItemOther" -> other.bands.flatMap{ band =>
            band.items.map { item =>
              Json.obj(
                "commodityDescription" -> item.metadata.declarationMessageDescription.take(40),
                "quantity" -> "1",
                "goodsValue" -> item.metadata.cost,
                "valueCurrency" -> item.metadata.currency.code,
                "originCountry" -> item.metadata.country.alphaTwoCode,
                "exchangeRate" -> item.metadata.exchangeRate.rate,
                "exchangeRateDate" -> item.metadata.exchangeRate.date,
                "customsValueGBP" -> item.purchaseCost,
                "VATRESClaimed" -> false,
                "exciseGBP" -> item.calculation.excise,
                "customsGBP" -> item.calculation.customs,
                "vatGBP" -> item.calculation.vat
              )
            }
          }
        )
        case None => JsNull
      }
    }

    Json.obj(
      "simpleDeclarationRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "receiptDate" -> receiptDateTime.withZone(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH:mm:ss'Z'"),
          "acknowledgementReference" -> acknowledgementReference,
          "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
        ),
        "requestDetail" -> Json.obj(
          "customerReference" -> customerReference,
          "personalDetails" -> personalDetails,
          "declarationHeader" -> declarationHeader,
          "declarationTobacco" -> declarationTobacco,
          "declarationAlcohol" -> declarationAlcohol,
          "declarationOther" -> declarationOther,
          "liabilityDetails" -> liabilityDetails
        )
      )
    ).stripNulls
  }

}
