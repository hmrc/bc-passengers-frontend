package services

import util._
import javax.inject.{Inject, Singleton}
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Mode.Mode
import play.api.libs.json._
import play.api.{Configuration, Environment, Logger}
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class DeclarationService @Inject()(
  val localSessionCache: LocalSessionCache,
  val wsAllMethods: WsAllMethods,
  configuration: Configuration,
  environment: Environment
) extends ServicesConfig with UsesJourneyData {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration = configuration

  lazy val passengersDeclarationsBaseUrl = baseUrl("bc-passengers-declarations")


  def submitDeclaration(userInformation: UserInformation, calculatorResponse: CalculatorResponse, receiptDateTime: DateTime, correlationId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeclarationServiceResponse] = {

    val rd = receiptDateTime.withZone(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")

    val partialDeclarationMessage = buildPartialDeclarationMessage(userInformation, calculatorResponse, rd)

    val headers = Seq(
      "X-Correlation-ID" -> correlationId
    )

    def extractChargeReference(declaration: JsValue) =
      ChargeReference((declaration \ "simpleDeclarationRequest" \ "requestDetail" \ "declarationHeader" \ "chargeReference").as[JsString].value)

    //First add correlation id etc
    wsAllMethods.POST[JsObject, HttpResponse](passengersDeclarationsBaseUrl + "/bc-passengers-declarations/submit-declaration", partialDeclarationMessage, headers) map {
      case HttpResponse(ACCEPTED, declaration, headers, _) =>
        DeclarationServiceSuccessResponse(extractChargeReference(declaration))
      case HttpResponse(BAD_REQUEST, _, _, _) =>
        Logger.error("BAD_REQUEST received from bc-passengers-declarations, invalid declaration submitted")
        DeclarationServiceFailureResponse
      case HttpResponse(status, _, _, _) =>
        Logger.error("Unexpected status of " + status + " received from bc-passengers-declarations, unable to proceed")
        DeclarationServiceFailureResponse
    }
  }

  def buildPartialDeclarationMessage(userInformation: UserInformation, calculatorResponse: CalculatorResponse, rd: String): JsObject = {

    val customerReference: JsValue = Json.toJson(userInformation)(new Writes[UserInformation] {
      override def writes(o: UserInformation): JsValue = Json.obj("passport" -> o.passportNumber)
    })

    val personalDetails = Json.toJson(userInformation)(new Writes[UserInformation] {
      override def writes(o: UserInformation): JsValue = Json.obj("firstName" -> o.firstName, "lastName" -> o.lastName)
    })

    val declarationHeader = Json.toJson(userInformation)(new Writes[UserInformation] {
      override def writes(o: UserInformation): JsValue = Json.obj("portOfEntry" -> o.placeOfArrival, "expectedDateOfArrival" -> o.dateOfArrival)
    })

    val liabilityDetails = Json.toJson(calculatorResponse.calculation)(new Writes[Calculation] {
      override def writes(o: Calculation): JsValue = Json.obj(
        "totalExciseGBP" -> o.excise,
        "totalCustomsGBP" -> o.customs,
        "totalVATGBP" -> o.vat,
        "grandTotalGBP" -> o.allTax
      )
    })

    val declarationTobacco = {

      calculatorResponse.tobacco match {
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

      calculatorResponse.alcohol match {
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

      calculatorResponse.otherGoods match {
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
          "receiptDate" -> rd,
          "requestParameters" -> Json.arr( Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR") )
        ),
        "requestDetail" -> Json.obj(
          "customerReference" -> customerReference,
          "personalDetails" -> personalDetails,
          "contactDetails" -> Json.obj(),
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

trait DeclarationServiceResponse
case object DeclarationServiceFailureResponse extends DeclarationServiceResponse
case class DeclarationServiceSuccessResponse(chargeReference: ChargeReference) extends DeclarationServiceResponse