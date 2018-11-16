package services

import controllers.routes
import javax.inject.{Inject, Singleton}
import models.{CalculatorResponse, ChargeReference, UserInformation}
import org.joda.time.DateTime
import play.api.Mode.Mode
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.{Configuration, Environment}
import play.mvc.Http.Status._
import services.http.WsAllMethods
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PayApiService @Inject()(
  val localSessionCache: LocalSessionCache,
  val wsAllMethods: WsAllMethods,
  configuration: Configuration,
  environment: Environment,
  productTreeService: ProductTreeService,
  currencyService: CurrencyService
) extends ServicesConfig with UsesJourneyData {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration = configuration

  lazy val payApiBaseUrl = baseUrl("pay-api")
  lazy val redirectUrl = configuration.getString("bc-passengers-frontend.host").getOrElse("") + routes.TravelDetailsController.checkDeclareGoodsStartPage().url

  def requestPaymentUrl(chargeReference: ChargeReference, userInformation: UserInformation, calculatorResponse: CalculatorResponse, amountPence: Int, receiptDateTime: DateTime)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PayApiServiceResponse] = {

    def items = {
      val allItems = for {
        alcohol <- calculatorResponse.alcohol.toList
        tobacco <- calculatorResponse.tobacco.toList
        otherGoods <- calculatorResponse.otherGoods.toList
        ab <- alcohol.bands
        tb <- tobacco.bands
        ob <- otherGoods.bands
      } yield {
        ab.items ++ tb.items ++ ob.items
      }
      allItems.flatten.filter(x => BigDecimal(x.calculation.allTax) > 0)
    }.zipWithIndex


    val baseEmailTemplateData = Json.obj(
      "NAME" -> s"${userInformation.firstName} ${userInformation.lastName}",
      "DATE" -> receiptDateTime.toString("dd MMMM Y HH:mm:ss z"),
      "PLACEOFARRIVAL" -> userInformation.placeOfArrival,
      "DATEOFARRIVAL" -> userInformation.dateOfArrival,
      "REFERENCE" -> chargeReference.value,
      "TOTAL" -> calculatorResponse.calculation.allTax
    )

    val emailTemplateData = items.foldLeft(baseEmailTemplateData) { case (jsObject, (item, index)) =>
        jsObject ++ Json.obj(s"NAME_$index" -> item.metadata.description, s"CURRENCY_$index" -> item.metadata.currency.displayName, s"COSTGBP_$index" -> item.metadata.cost)
    }

    val requestBody = Json.obj(
      "reference" -> chargeReference.value,
      "amountInPence" -> amountPence,
      "taxType" -> "pngr",
      "showCAWPTPage" -> false,
      "isWelshSupported" -> false,
      "emailTemplateId" -> "passengers_payment_confirmation",
      "emailTemplateData" -> emailTemplateData,
      "description" -> "Customs Declaration Payment",
      "searchScope" -> "pngr",
      "searchTag" -> chargeReference.value,
      "title" -> "Check tax on goods you bring into the UK",
      "returnUrl" -> redirectUrl
    )

    wsAllMethods.POST[JsValue, HttpResponse](payApiBaseUrl + "/pay-api/payment", requestBody) map { r =>
      r.status match {
        case CREATED => PayApiServiceSuccessResponse((r.json \ "links" \ "nextUrl").as[JsString].value)
        case _ => PayApiServiceFailureResponse
      }
    }

  }
}

trait PayApiServiceResponse
case object PayApiServiceFailureResponse extends PayApiServiceResponse
case class PayApiServiceSuccessResponse(url: String) extends PayApiServiceResponse