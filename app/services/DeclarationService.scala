/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package services


import connectors.Cache
import javax.inject.{Inject, Singleton}
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.libs.json.JodaWrites._
import util._
import play.api.libs.json._
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class DeclarationService @Inject()(
  val cache: Cache,
  val wsAllMethods: WsAllMethods,
  servicesConfig: ServicesConfig,
  implicit val ec: ExecutionContext
) {

  lazy val passengersDeclarationsBaseUrl: String = servicesConfig.baseUrl("bc-passengers-declarations")


  def submitDeclaration(userInformation: UserInformation, calculatorResponse: CalculatorResponse, journeyData: JourneyData, receiptDateTime: DateTime, correlationId: String)(implicit hc: HeaderCarrier, messages: Messages): Future[DeclarationServiceResponse] = {

    val rd = receiptDateTime.withZone(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")

    val partialDeclarationMessage = buildPartialDeclarationMessage(userInformation, calculatorResponse, journeyData, rd)

    val headers = Seq(
      "X-Correlation-ID" -> correlationId
    )

    def extractChargeReference(declaration: JsValue) =
      ChargeReference((declaration \ "simpleDeclarationRequest" \ "requestDetail" \ "declarationHeader" \ "chargeReference").as[JsString].value)

    //First add correlation id etc
    wsAllMethods.POST[JsObject, HttpResponse](passengersDeclarationsBaseUrl + "/bc-passengers-declarations/submit-declaration", partialDeclarationMessage, headers) map {
      case HttpResponse(ACCEPTED, declaration, _) =>
        DeclarationServiceSuccessResponse(extractChargeReference(Json.parse(declaration)))
      case HttpResponse(BAD_REQUEST, _, _) =>
        Logger.error("BAD_REQUEST received from bc-passengers-declarations, invalid declaration submitted")
        DeclarationServiceFailureResponse
      case HttpResponse(status, _, _) =>
        Logger.error("Unexpected status of " + status + " received from bc-passengers-declarations, unable to proceed")
        DeclarationServiceFailureResponse
    }
  }

  def buildPartialDeclarationMessage(userInformation: UserInformation, calculatorResponse: CalculatorResponse, journeyData: JourneyData, rd: String)(implicit messages: Messages): JsObject = {

    val ni = "NI"
    val gb = "GB"

    def getBooleanValue(value: Option[Boolean]): Boolean = {
      value match {
        case Some(true) => true
        case _ => false
      }
    }

    val customerReference: JsValue = Json.toJson(userInformation)((o: UserInformation) => {

      def getIdValue(idType: String, idValue: String): String = {
        idType match {
          case "telephone" => s"${servicesConfig.getString("declarations.telephonePrefix")}$idValue"
          case _ => idValue
        }
      }

      Json.obj("idType" -> o.identificationType,
        "idValue" -> getIdValue(o.identificationType, o.identificationNumber),
        "ukResident" -> getBooleanValue(journeyData.isUKResident))
    })

    val personalDetails = Json.toJson(userInformation)((o: UserInformation) => Json.obj("firstName" -> o.firstName, "lastName" -> o.lastName))

    val contactDetails = Json.toJson(userInformation)((o: UserInformation) => Json.obj("emailAddress" -> o.emailAddress))

    val declarationHeader: JsValue = Json.toJson(userInformation)((o: UserInformation) => {

      def formattedTwoDecimals(timeSegment: Int): String = {
        timeSegment match {
          case ts if ts < 10 => "0" + ts
          case ts => ts.toString
        }
      }

      def getPlaceOfArrival(userInfo: UserInformation): String = {
        if (userInfo.selectPlaceOfArrival.isEmpty) userInfo.enterPlaceOfArrival else userInfo.selectPlaceOfArrival
      }

      def getTravellingFrom(travellingFrom: Option[String]): String = {
        travellingFrom match {
          case Some("euOnly") => servicesConfig.getString("declarations.euOnly")
          case Some("nonEuOnly") => servicesConfig.getString("declarations.nonEuOnly")
          case _ => servicesConfig.getString("declarations.greatBritain")
        }
      }

      def getOnwardTravel(isArrivingNI: Option[Boolean]): String = {
        isArrivingNI match {
          case Some(true) => ni
          case _ => gb
        }
      }

      Json.obj("portOfEntry" -> getPlaceOfArrival(o),
                      "expectedDateOfArrival" -> o.dateOfArrival,
                      "timeOfEntry" -> s"${formattedTwoDecimals(o.timeOfArrival.getHourOfDay)}:${formattedTwoDecimals(o.timeOfArrival.getMinuteOfHour)}",
                      "messageTypes" -> Json.obj("messageType" -> servicesConfig.getString("declarations.create")),
                      "travellingFrom" -> getTravellingFrom(journeyData.euCountryCheck),
                      "onwardTravelGBNI" -> getOnwardTravel(journeyData.arrivingNICheck),
                      "uccRelief" -> getBooleanValue(journeyData.isUccRelief),
                      "ukVATPaid" -> getBooleanValue(journeyData.isUKVatPaid),
                      "ukExcisePaid" -> getBooleanValue(journeyData.isUKExcisePaid)
              )
    })

    val liabilityDetails = Json.toJson(calculatorResponse.calculation)((o: Calculation) => Json.obj(
      "totalExciseGBP" -> o.excise,
      "totalCustomsGBP" -> o.customs,
      "totalVATGBP" -> o.vat,
      "grandTotalGBP" -> o.allTax
    ))

    val declarationTobacco = {

      calculatorResponse.tobacco match {
        case Some(tobacco) => Json.obj(
          "totalExciseTobacco" -> tobacco.calculation.excise,
          "totalCustomsTobacco" -> tobacco.calculation.customs,
          "totalVATTobacco" -> tobacco.calculation.vat,
          "declarationItemTobacco" -> tobacco.bands.flatMap { band =>
            band.items.map { item =>
              Json.obj(
                "commodityDescription" -> messages(item.metadata.name).take(40),
                "quantity" -> item.noOfUnits.filter(_ != 0).fold[JsValue](JsNull)(x => JsString(x.toString)),
                "weight" -> item.weightOrVolume.fold[JsValue](JsNull)(x => JsString((x * 1000).toString())),
                "goodsValue" -> item.metadata.cost,
                "valueCurrency" -> item.metadata.currency.code,
                "originCountry" -> item.metadata.country.alphaTwoCode,
                "exchangeRate" -> {
                  val exchangeRate = BigDecimal(item.metadata.exchangeRate.rate)
                  if (exchangeRate.scale < 2) exchangeRate.setScale(2).toString() else exchangeRate.toString()
                },
                "exchangeRateDate" -> item.metadata.exchangeRate.date,
                "goodsValueGBP" -> item.purchaseCost,
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
          "declarationItemAlcohol" -> alcohol.bands.flatMap { band =>
            band.items.map { item =>
              Json.obj(
                "commodityDescription" -> messages(item.metadata.name).take(40),
                "volume" -> item.weightOrVolume.fold[JsValue](JsNull)(x => JsString(x.toString())),
                "goodsValue" -> item.metadata.cost,
                "valueCurrency" -> item.metadata.currency.code,
                "originCountry" -> item.metadata.country.alphaTwoCode,
                "exchangeRate" -> {
                  val exchangeRate = BigDecimal(item.metadata.exchangeRate.rate)
                  if (exchangeRate.scale < 2) exchangeRate.setScale(2).toString() else exchangeRate.toString()
                },
                "exchangeRateDate" -> item.metadata.exchangeRate.date,
                "goodsValueGBP" -> item.purchaseCost,
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
          "declarationItemOther" -> other.bands.flatMap { band =>
            band.items.map { item =>
              Json.obj(
                "commodityDescription" -> messages(item.metadata.name).take(40),
                "quantity" -> "1",
                "goodsValue" -> item.metadata.cost,
                "valueCurrency" -> item.metadata.currency.code,
                "originCountry" -> item.metadata.country.alphaTwoCode,
                "exchangeRate" -> {
                  val exchangeRate = BigDecimal(item.metadata.exchangeRate.rate)
                  if (exchangeRate.scale < 2) exchangeRate.setScale(2).toString() else exchangeRate.toString()
                },
                "exchangeRateDate" -> item.metadata.exchangeRate.date,
                "goodsValueGBP" -> item.purchaseCost,
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
          "requestParameters" -> Json.arr(Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR"))
        ),
        "requestDetail" -> Json.obj(
          "customerReference" -> customerReference,
          "personalDetails" -> personalDetails,
          "contactDetails" -> contactDetails,
          "declarationHeader" -> declarationHeader,
          "declarationTobacco" -> declarationTobacco,
          "declarationAlcohol" -> declarationAlcohol,
          "declarationOther" -> declarationOther,
          "liabilityDetails" -> liabilityDetails
        )
      )
    ).stripNulls
  }

  def storeChargeReference(journeyData: JourneyData, userInformation: UserInformation, chargeReference: String)(implicit hc: HeaderCarrier): Future[JourneyData] = {

    val updatedJourneyData = journeyData.copy(chargeReference = Some(chargeReference), userInformation = Some(userInformation))

    cache.store( updatedJourneyData ).map(_ => updatedJourneyData)
  }
}

trait DeclarationServiceResponse

case object DeclarationServiceFailureResponse extends DeclarationServiceResponse

case class DeclarationServiceSuccessResponse(chargeReference: ChargeReference) extends DeclarationServiceResponse
