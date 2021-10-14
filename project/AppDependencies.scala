import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.Keys._
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-play-28"         % "0.55.0",
    "uk.gov.hmrc"            %% "play-frontend-hmrc"         % "0.88.0-play-28",
    ws,
    "uk.gov.hmrc"            %% "bootstrap-frontend-play-28" % "5.16.0",
    "com.typesafe.play"      %% "play-json-joda"             % "2.6.14",
    "uk.gov.hmrc"            %% "play-language"              % "5.1.0-play-28",
    "org.mindrot"            % "jbcrypt"                     % "0.4",
    "org.webjars.npm"        % "accessible-autocomplete"     % "2.0.3",
    "ai.x"                   %% "play-json-extensions"       % "0.10.0"
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"                  % "3.2.10"             % "test,it",
    "org.pegdown"            % "pegdown"                     % "1.6.0"             % "test,it",
    "org.jsoup"              % "jsoup"                       % "1.13.1"            % "test,it",
    "com.typesafe.play"      %% "play-test"                  % PlayVersion.current % "test,it",
    "org.mockito"            % "mockito-all"                 % "2.0.2-beta"        % "test",  //required??
    "org.scalatestplus"      %% "mockito-3-4"                % "3.2.10.0",
    "org.scalatestplus.play" %% "scalatestplus-play"         % "5.1.0"             % "test,it",
    "com.networknt"          % "json-schema-validator"       % "1.0.63" exclude("org.slf4j", "slf4j-nop"),
    "ai.x"                   %% "play-json-extensions"       % "0.10.0",
    "com.vladsch.flexmark"   %  "flexmark-all"               % "0.36.8",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"    % "0.55.0"
  )
}

