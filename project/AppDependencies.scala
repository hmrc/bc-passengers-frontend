import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val hmrcMongoVersion = "0.71.0"
  val compile          = Seq(
    ws,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "3.24.0-play-28",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % "7.2.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.13.4",
    "com.typesafe.play"            %% "play-json-joda"             % "2.9.3",
    "uk.gov.hmrc"                  %% "play-language"              % "5.3.0-play-28",
    "org.mindrot"                   % "jbcrypt"                    % "0.4",
    "org.webjars.npm"               % "accessible-autocomplete"    % "2.0.4",
    "ai.x"                         %% "play-json-extensions"       % "0.42.0"
  )

  val test                                        = Seq(
    "org.scalatest"          %% "scalatest"               % "3.2.13",
    "org.jsoup"               % "jsoup"                   % "1.15.3",
    "com.typesafe.play"      %% "play-test"               % PlayVersion.current,
    "org.mockito"             % "mockito-all"             % "2.0.2-beta", //required??
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
    "com.networknt"           % "json-schema-validator"   % "1.0.72" exclude ("org.slf4j", "slf4j-nop"),
    "ai.x"                   %% "play-json-extensions"    % "0.42.0",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.62.2",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion
  ).map(_ % "test, it")

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.9" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.9" % Provided cross CrossVersion.full
  )

  def apply(): Seq[ModuleID] = compile ++ test ++ silencerDependencies
}
