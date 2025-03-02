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

package services

import audit.AuditingTools
import connectors.Cache
import models.*
import models.UserInformation.getPreUser
import play.api.Logger
import play.api.http.Status.*
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.Reads.*
import util.*
import play.api.libs.json.*
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeclarationService @Inject() (
  val cache: Cache,
  val portsOfArrivalService: PortsOfArrivalService,
  httpClient: HttpClientV2,
  servicesConfig: ServicesConfig,
  auditConnector: AuditConnector,
  auditingTools: AuditingTools,
  implicit val ec: ExecutionContext
) {

  private lazy val passengersDeclarationsBaseUrl: String = servicesConfig.baseUrl("bc-passengers-declarations")

  private val logger = Logger(this.getClass)

  def retrieveDeclaration(
    previousDeclarationDetails: PreviousDeclarationRequest
  )(implicit hc: HeaderCarrier): Future[DeclarationServiceResponse] = {

    def constructJourneyDataFromDeclarationResponse(declarationResponse: JsValue): JourneyData = {
      val userInformationOpt: Option[UserInformation] = (declarationResponse \ "userInformation").asOpt[UserInformation]
      val preUserInfo: Option[PreUserInformation]     = userInformationOpt.map(userInformation =>
        PreUserInformation(
          WhatIsYourNameForm(userInformation.firstName, userInformation.lastName),
          Some(IdentificationForm(userInformation.identificationType, Some(userInformation.identificationNumber))),
          Some(userInformation.emailAddress),
          Some(
            ArrivalForm(
              userInformation.selectPlaceOfArrival,
              userInformation.enterPlaceOfArrival,
              userInformation.dateOfArrival,
              userInformation.timeOfArrival
            )
          )
        )
      )

      JourneyData(
        prevDeclaration = Some(true),
        euCountryCheck = (declarationResponse \ "euCountryCheck").asOpt[String],
        arrivingNICheck = (declarationResponse \ "arrivingNI").asOpt[Boolean],
        ageOver17 = (declarationResponse \ "isOver17").asOpt[Boolean],
        isUKResident = (declarationResponse \ "isUKResident").asOpt[Boolean],
        privateCraft = (declarationResponse \ "isPrivateTravel").asOpt[Boolean],
        preUserInformation = preUserInfo,
        previousDeclarationRequest = Some(previousDeclarationDetails),
        declarationResponse = Some(
          DeclarationResponse(
            (declarationResponse \ "calculation").as[Calculation],
            (declarationResponse \ "liabilityDetails").as[LiabilityDetails],
            (declarationResponse \ "oldPurchaseProductInstances").as[List[PurchasedProductInstance]],
            (declarationResponse \ "amendmentCount").asOpt[Int]
          )
        ),
        amendState = (declarationResponse \ "amendState").asOpt[String],
        deltaCalculation =
          if ((declarationResponse \ "amendState").asOpt[String].getOrElse("").equals("pending-payment")) {
            (declarationResponse \ "deltaCalculation").asOpt[Calculation]
          } else { None }
      )
    }

    val url: String = s"$passengersDeclarationsBaseUrl/bc-passengers-declarations/retrieve-declaration"

    httpClient
      .post(url"$url")
      .withBody(Json.toJson(previousDeclarationDetails))
      .execute[HttpResponse]
      .map {
        case HttpResponse(OK, declarationResponse, _) =>
          DeclarationServiceRetrieveSuccessResponse(
            constructJourneyDataFromDeclarationResponse(Json.parse(declarationResponse))
          )
        case HttpResponse(BAD_REQUEST, _, _)          =>
          logger.error(
            "[DeclarationService][retrieveDeclaration] DECLARATION_RETRIEVAL_FAILURE BAD_REQUEST received from bc-passengers-declarations "
          )
          DeclarationServiceFailureResponse
        case HttpResponse(NOT_FOUND, _, _)            =>
          logger.error(
            "[DeclarationService][retrieveDeclaration] DECLARATION_RETRIEVAL_FAILURE NOT_FOUND received from bc-passengers-declarations"
          )
          DeclarationServiceFailureResponse
        case HttpResponse(status, body, _)            =>
          logger.error(
            s"[DeclarationService][retrieveDeclaration] DECLARATION_RETRIEVAL_FAILURE Unexpected status of $status received from bc-passengers-declarations, unable to proceed: $body"
          )
          DeclarationServiceFailureResponse
        case _                                        =>
          logger.error(
            """DECLARATION_SUBMIT_FAILURE [DeclarationService][extractChargeReference]
              | Unhandled declaration service response""".stripMargin
          )
          DeclarationServiceFailureResponse
      }
  }

  def submitDeclaration(
    userInformation: UserInformation,
    calculatorResponse: CalculatorResponse,
    journeyData: JourneyData,
    receiptDateTime: LocalDateTime,
    correlationId: String
  )(implicit hc: HeaderCarrier, messages: MessagesApi): Future[DeclarationServiceResponse] = {

    val rd =
      receiptDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK))

    val partialDeclarationMessage =
      buildPartialDeclarationOrAmendmentMessage(userInformation, calculatorResponse, journeyData, rd)

    val auditDeclarationMessage =
      formatDeclarationMessage(userInformation.identificationType, userInformation.identificationNumber, rd)

    partialDeclarationMessage.transform(auditDeclarationMessage) match {
      case JsSuccess(auditJson, _) =>
        auditConnector.sendExtendedEvent(auditingTools.buildDeclarationSubmittedDataEvent(auditJson))
      case JsError(errors)         =>
        logger.error(s"[DeclarationService][submitDeclaration] Transforming declaration message with errors : $errors")
    }

    def extractChargeReference(declaration: JsValue) =
      ChargeReference(
        (declaration \ "simpleDeclarationRequest" \ "requestDetail" \ "declarationHeader" \ "chargeReference")
          .as[JsString]
          .value
      )

    val url: String = s"$passengersDeclarationsBaseUrl/bc-passengers-declarations/submit-declaration"

    httpClient
      .post(url"$url")
      .setHeader("X-Correlation-ID" -> correlationId)
      .withBody(partialDeclarationMessage)
      .execute[HttpResponse]
      .map {
        case HttpResponse(ACCEPTED, declaration, _) =>
          DeclarationServiceSuccessResponse(extractChargeReference(Json.parse(declaration)))
        case HttpResponse(BAD_REQUEST, _, _)        =>
          logger.error(
            "[DeclarationService][extractChargeReference] DECLARATION_SUBMIT_FAILURE BAD_REQUEST received from bc-passengers-declarations, invalid declaration submitted"
          )
          DeclarationServiceFailureResponse
        case HttpResponse(status, _, _)             =>
          logger.error(
            s"[DeclarationService][extractChargeReference] DECLARATION_SUBMIT_FAILURE Unexpected status of $status received from bc-passengers-declarations, unable to proceed"
          )
          DeclarationServiceFailureResponse
        case _                                      =>
          logger.error(
            """[DeclarationService][extractChargeReference] DECLARATION_SUBMIT_FAILURE Unhandled declaration service response"""
          )
          DeclarationServiceFailureResponse
      }
  }

  def submitAmendment(
    userInformation: UserInformation,
    calculatorResponse: CalculatorResponse,
    journeyData: JourneyData,
    receiptDateTime: LocalDateTime,
    correlationId: String
  )(implicit hc: HeaderCarrier, messages: MessagesApi): Future[DeclarationServiceResponse] = {

    val rd =
      receiptDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK))

    val partialDeclarationMessage =
      buildPartialDeclarationOrAmendmentMessage(userInformation, calculatorResponse, journeyData, rd)

    val auditDeclarationMessage =
      formatDeclarationMessage(userInformation.identificationType, userInformation.identificationNumber, rd)

    partialDeclarationMessage.transform(auditDeclarationMessage) match {
      case JsSuccess(auditJson, _) =>
        auditConnector.sendExtendedEvent(auditingTools.buildDeclarationSubmittedDataEvent(auditJson))
      case JsError(errors)         =>
        logger.error(s"[DeclarationService][submitAmendment] Transforming declaration message with errors : $errors")
    }

    def extractChargeReference(declaration: JsValue) =
      ChargeReference(
        (declaration \ "simpleDeclarationRequest" \ "requestDetail" \ "declarationHeader" \ "chargeReference")
          .as[JsString]
          .value
      )

    val url: String = s"$passengersDeclarationsBaseUrl/bc-passengers-declarations/submit-amendment"

    httpClient
      .post(url"$url")
      .setHeader("X-Correlation-ID" -> correlationId)
      .withBody(partialDeclarationMessage)
      .execute[HttpResponse]
      .map {
        case HttpResponse(ACCEPTED, declaration, _) =>
          DeclarationServiceSuccessResponse(extractChargeReference(Json.parse(declaration)))
        case HttpResponse(BAD_REQUEST, _, _)        =>
          logger.error(
            """[DeclarationService][extractChargeReference] DECLARATION_SUBMIT_FAILURE BAD_REQUEST received from bc-passengers-declarations, invalid declaration submitted"""
          )
          DeclarationServiceFailureResponse
        case HttpResponse(status, _, _)             =>
          logger.error(
            s"""[DeclarationService][extractChargeReference] DECLARATION_SUBMIT_FAILURE Unexpected status of $status received from bc-passengers-declarations, unable to proceed"""
          )
          DeclarationServiceFailureResponse
        case _                                      =>
          logger.error(
            """[DeclarationService][extractChargeReference] DECLARATION_SUBMIT_FAILURE Unhandled declaration service response"""
          )
          DeclarationServiceFailureResponse
      }
  }

  def buildPartialDeclarationOrAmendmentMessage(
    userInformation: UserInformation,
    calculatorResponse: CalculatorResponse,
    journeyData: JourneyData,
    rd: String
  )(implicit messages: MessagesApi): JsObject = {

    given lang: Lang = Lang("en")
    val ni           = "NI"
    val gb           = "GB"

    def getBooleanValue(value: Option[Boolean]): Boolean =
      value match {
        case Some(true) => true
        case _          => false
      }

    val customerReference: JsValue = Json.toJson(userInformation) { (o: UserInformation) =>
      def getIdValue: String =
        o.identificationType match {
          case "telephone" => s"${servicesConfig.getString("declarations.telephonePrefix")}${o.identificationNumber}"
          case _           => o.identificationNumber
        }

      Json.obj(
        "idType"     -> o.identificationType,
        "idValue"    -> getIdValue.toUpperCase,
        "ukResident" -> getBooleanValue(journeyData.isUKResident)
      )
    }

    val personalDetails = Json.toJson(userInformation)((o: UserInformation) =>
      Json.obj("firstName" -> o.firstName, "lastName" -> o.lastName)
    )

    val contactDetails: JsValue = Json.toJson(userInformation) { (o: UserInformation) =>
      if (o.emailAddress.nonEmpty) {
        Json.obj("emailAddress" -> o.emailAddress)
      } else {
        Json.obj()
      }
    }

    val declarationHeader: JsValue = Json.toJson(userInformation) { (o: UserInformation) =>
      def formattedTwoDecimals(timeSegment: Int): String =
        timeSegment match {
          case ts if ts < 10 => "0" + ts
          case ts            => ts.toString
        }

      def getPlaceOfArrivalCode: String =
        if (o.selectPlaceOfArrival.isEmpty) o.enterPlaceOfArrival else o.selectPlaceOfArrival

      def getPlaceOfArrival: String =
        if (o.selectPlaceOfArrival.isEmpty) { o.enterPlaceOfArrival }
        else { messages(portsOfArrivalService.getDisplayNameByCode(o.selectPlaceOfArrival).getOrElse("")) }

      def getTravellingFrom: String =
        journeyData.euCountryCheck match {
          case Some("euOnly")    => servicesConfig.getString("declarations.euOnly")
          case Some("nonEuOnly") => servicesConfig.getString("declarations.nonEuOnly")
          case _                 => servicesConfig.getString("declarations.greatBritain")
        }

      def getOnwardTravel: String =
        journeyData.arrivingNICheck match {
          case Some(true) => ni
          case _          => gb
        }

      def getMessageTypes: String = if (journeyData.declarationResponse.isDefined) {
        servicesConfig.getString("declarations.amend")
      } else {
        servicesConfig.getString("declarations.create")
      }

      Json.obj(
        "portOfEntry"           -> getPlaceOfArrivalCode,
        "portOfEntryName"       -> getPlaceOfArrival,
        "expectedDateOfArrival" -> o.dateOfArrival,
        "timeOfEntry"           ->
          s"${formattedTwoDecimals(o.timeOfArrival.getHour)}:${formattedTwoDecimals(o.timeOfArrival.getMinute)}",
        "messageTypes"          -> Json.obj("messageType" -> getMessageTypes),
        "travellingFrom"        -> getTravellingFrom,
        "onwardTravelGBNI"      -> getOnwardTravel,
        "uccRelief"             -> getBooleanValue(journeyData.isUccRelief),
        "ukVATPaid"             -> getBooleanValue(journeyData.isUKVatPaid),
        "ukExcisePaid"          -> getBooleanValue(journeyData.isUKVatExcisePaid)
      )
    }

    def getDeclarationHeader: JsValue =
      if (journeyData.previousDeclarationRequest.isDefined) {
        declarationHeader
          .as[JsObject]
          .++(
            Json.obj("chargeReference" -> journeyData.previousDeclarationRequest.get.referenceNumber)
          )
      } else {
        declarationHeader
      }

    def getLiabilityDetails: JsValue =
      Json.toJson(calculatorResponse.calculation)((o: Calculation) =>
        Json.obj(
          "totalExciseGBP"  -> o.excise,
          "totalCustomsGBP" -> o.customs,
          "totalVATGBP"     -> o.vat,
          "grandTotalGBP"   -> o.allTax
        )
      )

    def getAmendmentLiability: JsValue =
      if (journeyData.declarationResponse.isDefined) {
        val liability = journeyData.declarationResponse.get.liabilityDetails
        Json.toJson(journeyData.calculatorResponse.get.calculation)((total: Calculation) =>
          Json.obj(
            "additionalExciseGBP"  -> (BigDecimal(total.excise) - BigDecimal(liability.totalExciseGBP))
              .setScale(2)
              .toString,
            "additionalCustomsGBP" -> (BigDecimal(total.customs) - BigDecimal(liability.totalCustomsGBP))
              .setScale(2)
              .toString,
            "additionalVATGBP"     -> (BigDecimal(total.vat) - BigDecimal(liability.totalVATGBP)).setScale(2).toString,
            "additionalTotalGBP"   -> (BigDecimal(total.allTax) - BigDecimal(liability.grandTotalGBP))
              .setScale(2)
              .toString
          )
        )
      } else {
        Json.toJson(JsNull)
      }

    val declarationTobacco =
      calculatorResponse.tobacco match {
        case Some(tobacco) =>
          Json.obj(
            "totalExciseTobacco"     -> tobacco.calculation.excise,
            "totalCustomsTobacco"    -> tobacco.calculation.customs,
            "totalVATTobacco"        -> tobacco.calculation.vat,
            "declarationItemTobacco" -> tobacco.bands.flatMap { band =>
              band.items.map { item =>
                Json.obj(
                  "commodityDescription" -> messages(item.metadata.name).take(40),
                  "quantity"             -> item.noOfUnits.filter(_ != 0).fold[JsValue](JsNull)(x => JsString(x.toString)),
                  "weight"               -> item.weightOrVolume.fold[JsValue](JsNull)(x => JsString((x * 1000).toString)),
                  "goodsValue"           -> item.metadata.cost,
                  "valueCurrency"        -> item.metadata.currency.code,
                  "valueCurrencyName"    -> messages(item.metadata.currency.displayName),
                  "originCountry"        -> item.metadata.country.alphaTwoCode,
                  "originCountryName"    -> messages(item.metadata.country.countryName),
                  "exchangeRate"         -> {
                    val exchangeRate = BigDecimal(item.metadata.exchangeRate.rate)
                    if (exchangeRate.scale < 2) exchangeRate.setScale(2).toString else exchangeRate.toString
                  },
                  "exchangeRateDate"     -> item.metadata.exchangeRate.date,
                  "goodsValueGBP"        -> item.purchaseCost,
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> item.calculation.excise,
                  "customsGBP"           -> item.calculation.customs,
                  "vatGBP"               -> item.calculation.vat,
                  "ukVATPaid"            -> item.isVatPaid,
                  "ukExcisePaid"         -> item.isExcisePaid,
                  "madeIn"               -> item.metadata.originCountry.map(_.alphaTwoCode),
                  "euCustomsRelief"      -> item.isCustomPaid
                )
              }
            }
          )
        case None          => JsNull
      }

    val declarationAlcohol =
      calculatorResponse.alcohol match {
        case Some(alcohol) =>
          Json.obj(
            "totalExciseAlcohol"     -> alcohol.calculation.excise,
            "totalCustomsAlcohol"    -> alcohol.calculation.customs,
            "totalVATAlcohol"        -> alcohol.calculation.vat,
            "declarationItemAlcohol" -> alcohol.bands.flatMap { band =>
              band.items.map { item =>
                Json.obj(
                  "commodityDescription" -> messages(item.metadata.name).take(40),
                  "volume"               -> item.weightOrVolume.fold[JsValue](JsNull)(x => JsString(x.toString)),
                  "goodsValue"           -> item.metadata.cost,
                  "valueCurrency"        -> item.metadata.currency.code,
                  "valueCurrencyName"    -> messages(item.metadata.currency.displayName),
                  "originCountry"        -> item.metadata.country.alphaTwoCode,
                  "originCountryName"    -> messages(item.metadata.country.countryName),
                  "exchangeRate"         -> {
                    val exchangeRate = BigDecimal(item.metadata.exchangeRate.rate)
                    if (exchangeRate.scale < 2) exchangeRate.setScale(2).toString else exchangeRate.toString
                  },
                  "exchangeRateDate"     -> item.metadata.exchangeRate.date,
                  "goodsValueGBP"        -> item.purchaseCost,
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> item.calculation.excise,
                  "customsGBP"           -> item.calculation.customs,
                  "vatGBP"               -> item.calculation.vat,
                  "ukVATPaid"            -> item.isVatPaid,
                  "ukExcisePaid"         -> item.isExcisePaid,
                  "madeIn"               -> item.metadata.originCountry.map(_.alphaTwoCode),
                  "euCustomsRelief"      -> item.isCustomPaid
                )
              }
            }
          )
        case None          => JsNull
      }

    val declarationOther =
      calculatorResponse.otherGoods match {
        case Some(other) =>
          Json.obj(
            "totalExciseOther"     -> other.calculation.excise,
            "totalCustomsOther"    -> other.calculation.customs,
            "totalVATOther"        -> other.calculation.vat,
            "declarationItemOther" -> other.bands.flatMap { band =>
              band.items.map { item =>
                Json.obj(
                  "commodityDescription" -> messages(item.metadata.name).take(40),
                  "quantity"             -> "1",
                  "goodsValue"           -> item.metadata.cost,
                  "valueCurrency"        -> item.metadata.currency.code,
                  "valueCurrencyName"    -> messages(item.metadata.currency.displayName),
                  "originCountry"        -> item.metadata.country.alphaTwoCode,
                  "originCountryName"    -> messages(item.metadata.country.countryName),
                  "exchangeRate"         -> {
                    val exchangeRate = BigDecimal(item.metadata.exchangeRate.rate)
                    if (exchangeRate.scale < 2) exchangeRate.setScale(2).toString else exchangeRate.toString
                  },
                  "exchangeRateDate"     -> item.metadata.exchangeRate.date,
                  "goodsValueGBP"        -> item.purchaseCost,
                  "VATRESClaimed"        -> false,
                  "exciseGBP"            -> item.calculation.excise,
                  "customsGBP"           -> item.calculation.customs,
                  "vatGBP"               -> item.calculation.vat,
                  "ukVATPaid"            -> item.isVatPaid,
                  "uccRelief"            -> item.isUccRelief,
                  "madeIn"               -> item.metadata.originCountry.map(_.alphaTwoCode),
                  "euCustomsRelief"      -> item.isCustomPaid
                )
              }
            }
          )
        case None        => JsNull
      }

    def getJourneyData = {

      val amendmentCount: Int =
        if (journeyData.declarationResponse.isDefined && journeyData.declarationResponse.get.amendmentCount.isDefined) {
          journeyData.declarationResponse.get.amendmentCount.get + 1
        } else { 0 }

      val cumulativePurchasedProductInstances = journeyData.purchasedProductInstances ++
        journeyData.declarationResponse.map(_.oldPurchaseProductInstances).getOrElse(Nil)

      val finalJourneyData = journeyData.copy(
        prevDeclaration = None,
        previousDeclarationRequest = None,
        preUserInformation = Some(getPreUser(userInformation)),
        purchasedProductInstances = cumulativePurchasedProductInstances,
        declarationResponse = None,
        deltaCalculation = journeyData.deltaCalculation,
        amendmentCount = Some(amendmentCount)
      )

      JourneyDataDownstream(finalJourneyData)
    }

    def getRequestCommon: JsValue = {

      val requestCommon = Json.obj(
        "receiptDate"       -> rd,
        "requestParameters" -> Json.arr(Json.obj("paramName" -> "REGIME", "paramValue" -> "PNGR"))
      )

      if (journeyData.previousDeclarationRequest.isDefined && journeyData.declarationResponse.isDefined) {
        requestCommon.++(
          Json.obj(
            "acknowledgementReference" -> (journeyData.previousDeclarationRequest.get.referenceNumber + getJourneyData.amendmentCount.get)
          )
        )
      } else {
        requestCommon
      }
    }

    Json
      .obj(
        "journeyData"              -> Json.toJsObject(getJourneyData),
        "simpleDeclarationRequest" -> Json.obj(
          "requestCommon" -> getRequestCommon,
          "requestDetail" -> Json.obj(
            "customerReference"         -> customerReference,
            "personalDetails"           -> personalDetails,
            "contactDetails"            -> contactDetails,
            "declarationHeader"         -> getDeclarationHeader,
            "declarationTobacco"        -> declarationTobacco,
            "declarationAlcohol"        -> declarationAlcohol,
            "declarationOther"          -> declarationOther,
            "liabilityDetails"          -> getLiabilityDetails,
            "amendmentLiabilityDetails" -> getAmendmentLiability
          )
        )
      )
      .stripNulls
  }

  def storeChargeReference(journeyData: JourneyData, userInformation: UserInformation, chargeReference: String)(implicit
    hc: HeaderCarrier
  ): Future[JourneyData] = {

    val updatedJourneyData =
      journeyData.copy(chargeReference = Some(chargeReference), preUserInformation = Some(getPreUser(userInformation)))

    cache.store(updatedJourneyData).map(_ => updatedJourneyData)
  }

  def updateDeclaration(reference: String)(implicit hc: HeaderCarrier): Future[DeclarationServiceResponse] = {
    val url: String = s"$passengersDeclarationsBaseUrl/bc-passengers-declarations/update-payment"

    httpClient
      .post(url"$url")
      .withBody(Json.toJson(PaymentNotification("Successful", reference)))
      .execute[HttpResponse]
      .map {
        case HttpResponse(ACCEPTED, _, _)    =>
          DeclarationServiceSuccessResponse
        case HttpResponse(BAD_REQUEST, _, _) =>
          logger.error(
            "[DeclarationService][updateDeclaration] ZERO_DECLARATION_UPDATE_FAILURE BAD_REQUEST received from bc-passengers-declarations "
          )
          DeclarationServiceFailureResponse
        case HttpResponse(NOT_FOUND, _, _)   =>
          logger.error(
            "[DeclarationService][updateDeclaration] ZERO_DECLARATION_UPDATE_FAILURE NOT_FOUND received from bc-passengers-declarations"
          )
          DeclarationServiceFailureResponse
        case HttpResponse(status, _, _)      =>
          logger.error(
            s"""[DeclarationService][updateDeclaration] ZERO_DECLARATION_UPDATE_FAILURE Unexpected status of $status received from bc-passengers-declarations, unable to proceed"""
          )
          DeclarationServiceFailureResponse
        case _                               =>
          logger.error(
            """[DeclarationService][extractChargeReference] ZERO_DECLARATION_UPDATE_FAILURE Unhandled declaration service response"""
          )
          DeclarationServiceFailureResponse
      }
  }

  private def formatDeclarationMessage(idType: String, idValue: String, receiptDateTime: String): Reads[JsObject] = {

    val localPath: JsPath = __ \ Symbol("simpleDeclarationRequest")

    def getIdValue: String =
      idType match {
        case "telephone" => s"${servicesConfig.getString("declarations.telephonePrefix")}$idValue"
        case _           => idValue
      }

    (localPath.json
      .copyFrom((localPath \ Symbol("requestDetail")).json.pick) andThen
      (localPath \ Symbol("requestCommon")).json.prune andThen
      (localPath \ Symbol("requestDetail")).json.prune) andThen
      (localPath \ Symbol("customerReference") \ Symbol("idType")).json.prune andThen
      (localPath \ Symbol("customerReference") \ Symbol("idValue")).json.prune andThen
      localPath.json
        .update(
          __.read[JsObject].map(o => o ++ Json.obj("receiptDate" -> receiptDateTime))
        ) andThen
      localPath.json
        .update(
          __.read[JsObject].map(o => o ++ Json.obj("REGIME" -> "PNGR"))
        ) andThen
      (localPath \ Symbol("customerReference")).json
        .update(
          __.read[JsObject].map(o => o ++ Json.obj(idType -> getIdValue))
        )

  }

}

trait DeclarationServiceResponse

case object DeclarationServiceFailureResponse extends DeclarationServiceResponse

case class DeclarationServiceSuccessResponse(chargeReference: ChargeReference) extends DeclarationServiceResponse

case class DeclarationServiceRetrieveSuccessResponse(jd: JourneyData) extends DeclarationServiceResponse

case object DeclarationServiceSuccessResponse extends DeclarationServiceResponse
