import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val hmrcMongoVersion = "0.71.0"
  val compile          = Seq(
    ws,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "3.23.0-play-28",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % "7.1.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.13.3",
    "com.typesafe.play"            %% "play-json-joda"             % "2.9.2",
    "uk.gov.hmrc"                  %% "play-language"              % "5.3.0-play-28",
    "org.mindrot"                   % "jbcrypt"                    % "0.4",
    "org.webjars.npm"               % "accessible-autocomplete"    % "2.0.4",
    "ai.x"                         %% "play-json-extensions"       % "0.42.0"
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"               % "3.2.13"            % "test,it",
    "org.jsoup"               % "jsoup"                   % "1.15.3"            % "test,it",
    "com.typesafe.play"      %% "play-test"               % PlayVersion.current % "test,it",
    "org.mockito"             % "mockito-all"             % "2.0.2-beta"        % "test", //required??
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0"          % "test,it",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"             % "test,it",
    "com.networknt"           % "json-schema-validator"   % "1.0.72"            % "test,it" exclude ("org.slf4j", "slf4j-nop"),
    "ai.x"                   %% "play-json-extensions"    % "0.42.0"            % "test,it",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.62.2"            % "test,it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion    % "test,it"
  )
}
