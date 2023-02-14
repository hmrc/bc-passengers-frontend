import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val hmrcMongoVersion = "0.74.0"
  val bootstrapVersion = "7.13.0"
  val compile          = Seq(
    ws,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "6.4.0-play-28",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.14.2",
    "com.typesafe.play"            %% "play-json-joda"             % "2.9.4",
    "org.mindrot"                   % "jbcrypt"                    % "0.4",
    "org.webjars.npm"               % "accessible-autocomplete"    % "2.0.4",
    "ai.x"                         %% "play-json-extensions"       % "0.42.0"
  )

  val test                   = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapVersion,
    "org.scalatest"          %% "scalatest"               % "3.2.15",
    "org.jsoup"               % "jsoup"                   % "1.15.3",
    "com.typesafe.play"      %% "play-test"               % PlayVersion.current,
    "org.mockito"            %% "mockito-scala-scalatest" % "1.17.12",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
    "com.networknt"           % "json-schema-validator"   % "1.0.76" exclude ("org.slf4j", "slf4j-nop"),
    "ai.x"                   %% "play-json-extensions"    % "0.42.0",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.62.2",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
