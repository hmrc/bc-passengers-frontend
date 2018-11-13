package controllers

import java.util.UUID

import config.AppConfig
import javax.inject.Inject
import models.{ChargeReference, ConfirmRemoveDto, ProductPath}
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, MessagesApi}
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class DeclarationMessageController @Inject() (
                                               val declarationMessageService: DeclarationMessageService,
                                               val travelDetailsService: TravelDetailsService,
                                               val currencyService: CurrencyService,
                                               val countriesService: CountriesService,
                                               val productTreeService: ProductTreeService
                                             )(implicit val appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with I18nSupport with ControllerHelpers {

  def declarationMessage() = DashboardAction { implicit context =>

    requireUserInformation { userInfo =>
      val chargeReference = ChargeReference(userInfo.temporaryChargeReference)
      val declarationJson = declarationMessageService.declarationMessage(chargeReference, context.getJourneyData, userInfo.temporaryReceiptDateTime, chargeReference.value + "0")
      Future.successful(Ok(declarationJson).withHeaders("X-Correlation-ID" -> UUID.randomUUID().toString.filter(_ != '-')))
    }
  }
}
