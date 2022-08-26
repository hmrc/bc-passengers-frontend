/*
 * Copyright 2022 HM Revenue & Customs
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

package services.http

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.http.ws._

trait WSHttp
    extends HttpGet
    with WSGet
    with HttpPut
    with WSPut
    with HttpPost
    with WSPost
    with HttpDelete
    with WSDelete
    with HttpPatch
    with WSPatch

@Singleton
class WsAllMethods @Inject() (
  override val auditConnector: AuditConnector,
  override val actorSystem: ActorSystem,
  environment: Environment,
  val wsClient: WSClient,
  config: Configuration
) extends WSHttp
    with HttpAuditing {

  override lazy val appName                    = AppName.fromConfiguration(config)
  override val hooks                           = Seq(AuditingHook)
  override protected val configuration: Config = config.underlying
}
