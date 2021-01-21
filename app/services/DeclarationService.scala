/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services


import audit.AuditingTools
import connectors.Cache
import javax.inject.{Inject, Singleton}
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.libs.json.JodaWrites._
import play.api.libs.json.Reads._
import util._
import play.api.libs.json._
import services.http.WsAllMethods
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class DeclarationService @Inject()(
  val cache: Cache,
  val wsAllMethods: WsAllMethods,
  val portsOfArrivalService: PortsOfArrivalService,
  servicesConfig: ServicesConfig,
  auditConnector: AuditConnector,
  auditingTools: AuditingTools,
  implicit val ec: ExecutionContext
) {

  lazy val passengersDeclarationsBaseUrl: String = servicesConfig.baseUrl("bc-passengers-declarations")


  def submitDeclaration(userInformation: UserInformation, calculatorResponse: CalculatorResponse, journeyData: JourneyData, receiptDateTime: DateTime, correlationId: String)(implicit hc: HeaderCarrier, messages: Messages): Future[DeclarationServiceResponse] = {

    val rd = receiptDateTime.withZone(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")

    val partialDeclarationMessage = buildPartialDeclarationMessage(userInformation, calculatorResponse, journeyData, rd)

    val auditDeclarationMessage = formatDeclarationMessage(userInformation.identificationType, userInformation.identificationNumber, rd)

    partialDeclarationMessage.transform(auditDeclarationMessage) match {
      case JsSuccess(auditJson, _) => auditConnector.sendExtendedEvent(auditingTools.buildDeclarationSubmittedDataEvent(auditJson))
      case JsError(errors) => Logger.error(s"[DeclarationService][submitDeclaration] Transforming declaration message with errors : $errors")
    }

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

      def getIdValue: String = {
        o.identificationType match {
          case "telephone" => s"${servicesConfig.getString("declarations.telephonePrefix")}${o.identificationNumber}"
          case _ => o.identificationNumber
        }
      }

      Json.obj("idType" -> o.identificationType,
        "idValue" -> getIdValue.toUpperCase,
        "ukResident" -> getBooleanValue(journeyData.isUKResident))
    })

    val personalDetails = Json.toJson(userInformation)((o: UserInformation) => Json.obj("firstName" -> o.firstName, "lastName" -> o.lastName))

    val contactDetails: JsValue = Json.toJson(userInformation)((o: UserInformation) => {

      if(o.emailAddress.nonEmpty) {
        Json.obj("emailAddress" -> o.emailAddress)
      } else {
        Json.obj()
      }
    })

    val declarationHeader: JsValue = Json.toJson(userInformation)((o: UserInformation) => {

      def formattedTwoDecimals(timeSegment: Int): String = {
        timeSegment match {
          case ts if ts < 10 => "0" + ts
          case ts => ts.toString
        }
      }

      def getPlaceOfArrivalCode: String = {
        if (o.selectPlaceOfArrival.isEmpty) o.enterPlaceOfArrival else o.selectPlaceOfArrival
      }

      def getPlaceOfArrival: String = {
        if (o.selectPlaceOfArrival.isEmpty) o.enterPlaceOfArrival else messages(portsOfArrivalService.getDisplayNameByCode(o.selectPlaceOfArrival).getOrElse(""))
      }

      def getTravellingFrom: String = {
        journeyData.euCountryCheck match {
          case Some("euOnly") => servicesConfig.getString("declarations.euOnly")
          case Some("nonEuOnly") => servicesConfig.getString("declarations.nonEuOnly")
          case _ => servicesConfig.getString("declarations.greatBritain")
        }
      }

      def getOnwardTravel: String = {
        journeyData.arrivingNICheck match {
          case Some(true) => ni
          case _ => gb
        }
      }

      Json.obj("portOfEntry" -> getPlaceOfArrivalCode,
                      "portOfEntryName" -> getPlaceOfArrival,
                      "expectedDateOfArrival" -> o.dateOfArrival,
                      "timeOfEntry" -> s"${formattedTwoDecimals(o.timeOfArrival.getHourOfDay)}:${formattedTwoDecimals(o.timeOfArrival.getMinuteOfHour)}",
                      "messageTypes" -> Json.obj("messageType" -> servicesConfig.getString("declarations.create")),
                      "travellingFrom" -> getTravellingFrom,
                      "onwardTravelGBNI" -> getOnwardTravel,
                      "uccRelief" -> getBooleanValue(journeyData.isUccRelief),
                      "ukVATPaid" -> getBooleanValue(journeyData.isUKVatPaid),
                      "ukExcisePaid" -> getBooleanValue(journeyData.isUKVatExcisePaid)
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
                "valueCurrencyName" -> messages(item.metadata.currency.displayName),
                "originCountry" -> item.metadata.country.alphaTwoCode,
                "originCountryName" -> messages(item.metadata.country.countryName),
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
                "valueCurrencyName" -> messages(item.metadata.currency.displayName),
                "originCountry" -> item.metadata.country.alphaTwoCode,
                "originCountryName" -> messages(item.metadata.country.countryName),
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
                "valueCurrencyName" -> messages(item.metadata.currency.displayName),
                "originCountry" -> item.metadata.country.alphaTwoCode,
                "originCountryName" -> messages(item.metadata.country.countryName),
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

  def updateDeclaration(reference: String)(implicit hc: HeaderCarrier): Future[DeclarationServiceResponse] = {
    wsAllMethods.POST[PaymentNotification, Unit](passengersDeclarationsBaseUrl + "/bc-passengers-declarations/update-payment", PaymentNotification("Successful", reference))
     .map(_ =>  DeclarationServiceSuccessResponse)
    .recover {
      case e => Logger.error(s"Status update failed for $reference in bc-passengers-declarations", e)
        DeclarationServiceFailureResponse
    }
  }

  private def formatDeclarationMessage(idType: String, idValue: String, receiptDateTime: String): Reads[JsObject] = {

    val localPath: JsPath = __ \ 'simpleDeclarationRequest

    def getIdValue: String = {
      idType match {
        case "telephone" => s"${servicesConfig.getString("declarations.telephonePrefix")}$idValue"
        case _ => idValue
      }
    }

    (localPath.json
      .copyFrom((localPath \ 'requestDetail).json.pick) andThen
    (localPath \ 'requestCommon).json.prune andThen
    (localPath \ 'requestDetail).json.prune) andThen
    (localPath \ 'customerReference \ 'idType).json
      .prune andThen
    (localPath \ 'customerReference  \ 'idValue).json
      .prune andThen
    localPath.json
      .update(
        __.read[JsObject].map{ o => o ++ Json.obj("receiptDate" -> receiptDateTime)}
      ) andThen
    localPath.json
      .update(
        __.read[JsObject].map{ o => o ++ Json.obj("REGIME" -> "PNGR")  }
      ) andThen
    (localPath \ 'customerReference).json
      .update(
        __.read[JsObject].map{ o => o ++ Json.obj( idType -> getIdValue)  }
      )

  }

}

trait DeclarationServiceResponse

case object DeclarationServiceFailureResponse extends DeclarationServiceResponse

case class DeclarationServiceSuccessResponse(chargeReference: ChargeReference) extends DeclarationServiceResponse

case object DeclarationServiceSuccessResponse extends DeclarationServiceResponse
