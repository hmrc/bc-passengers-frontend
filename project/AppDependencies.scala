import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.Keys._
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.30.0-play-26",
    "uk.gov.hmrc" %% "govuk-template" % "5.58.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.13.0-play-26",
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "2.0.0",
    "com.typesafe.play" %% "play-json-joda" % "2.6.14",
    "uk.gov.hmrc" %% "play-language" % "4.4.0-play-26",
    "org.mindrot" % "jbcrypt" % "0.4",
    "org.webjars.npm" % "accessible-autocomplete" % "2.0.3"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % "test,it", //deprecated
    "org.scalatest" %% "scalatest" % "3.0.9" % "test,it",
    "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
    "org.jsoup" % "jsoup" % "1.13.1" % "test,it",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
    "org.mockito" % "mockito-all" % "2.0.2-beta" % "test",  //required??
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % "test,it",
    "com.networknt" % "json-schema-validator" % "1.0.44" exclude("org.slf4j", "slf4j-nop")
  )
}

