package services.http

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.LoadAuditingConfig
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws._

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with HttpPatch with WSPatch

@Singleton
class WsAllMethods @Inject() (
  override val auditConnector: AuditConnector,
  environment: Environment,
  config: Configuration
) extends WSHttp with HttpAuditing with AppName with RunMode {

  override val hooks = Seq(AuditingHook)

  override val mode = environment.mode
  override protected val appNameConfiguration = config
  override protected val runModeConfiguration = config
}