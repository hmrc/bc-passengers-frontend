/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package audit

import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
@Singleton
class AuditingTools @Inject()(
  @Named("appName") val appName: String
) {

  def buildDeclarationSubmittedDataEvent(buildPartialDeclarationMessage: JsObject): ExtendedDataEvent = {
    ExtendedDataEvent(
      auditSource = appName,
      auditType =  "PassengerDeclarations",
      tags = Map("transactionName" -> "passenger-declarations-submission"),
      detail = buildPartialDeclarationMessage

    )
  }
}
