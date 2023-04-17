import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val hmrcMongoVersion     = "0.74.0"
  private val bootstrapPlayVersion = "7.15.0"

  private val compile        = Seq(
    ws,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "7.3.0-play-28",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.14.2",
    "com.typesafe.play"            %% "play-json-joda"             % "2.9.4",
    "org.mindrot"                   % "jbcrypt"                    % "0.4",
    "org.webjars.npm"               % "accessible-autocomplete"    % "2.0.4",
    "ai.x"                         %% "play-json-extensions"       % "0.42.0"
  )
  private val test           = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"  % bootstrapPlayVersion,
    "org.scalatest"       %% "scalatest"               % "3.2.15",
    "org.jsoup"            % "jsoup"                   % "1.15.4",
    "org.mockito"         %% "mockito-scala-scalatest" % "1.17.14",
    "com.networknt"        % "json-schema-validator"   % "1.0.79" exclude ("org.slf4j", "slf4j-nop"),
    "ai.x"                %% "play-json-extensions"    % "0.42.0",
    "com.vladsch.flexmark" % "flexmark-all"            % "0.64.0",
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % hmrcMongoVersion
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
