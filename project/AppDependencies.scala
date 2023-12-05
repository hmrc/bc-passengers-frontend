import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val hmrcMongoVersion     = "1.6.0"
  private val bootstrapPlayVersion = "7.23.0"

  private val compile        = Seq(
    ws,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "7.29.0-play-28",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.16.0",
    "com.typesafe.play"            %% "play-json-joda"             % "2.10.3",
    "org.webjars.npm"               % "accessible-autocomplete"    % "2.0.4",
    "ai.x"                         %% "play-json-extensions"       % "0.42.0"
  )
  private val test           = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"  % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % hmrcMongoVersion,
    "org.scalatest"       %% "scalatest"               % "3.2.17",
    "org.jsoup"            % "jsoup"                   % "1.17.1",
    "org.mockito"         %% "mockito-scala-scalatest" % "1.17.30",
    "com.vladsch.flexmark" % "flexmark-all"            % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
