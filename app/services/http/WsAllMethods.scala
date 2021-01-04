/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services.http

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws._

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with HttpPatch with WSPatch

@Singleton
class WsAllMethods @Inject() (
  override val auditConnector: AuditConnector,
  override val actorSystem: ActorSystem,
  environment: Environment,
  val wsClient: WSClient,
  config: Configuration
) extends WSHttp with HttpAuditing {

  override lazy val appName = AppName.fromConfiguration(config)
  override val hooks = Seq(AuditingHook)
  override protected val configuration: Option[Config] = None
}
