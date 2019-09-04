import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.Keys._
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "govuk-template" % "5.38.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.0.0-play-26",
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.46.0",
    "uk.gov.hmrc" %% "http-caching-client" % "8.5.0-play-26",
    "com.typesafe.play" %% "play-json-joda" % "2.6.13",
    "org.mindrot" % "jbcrypt" % "0.4"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % "test,it", //deprecated
    "org.scalatest" %% "scalatest" % "3.0.8" % "test,it",
    "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
    "org.jsoup" % "jsoup" % "1.12.1" % "test,it",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
    "org.mockito" % "mockito-all" % "2.0.2-beta" % "test",  //required??
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test",
    "com.networknt" % "json-schema-validator" % "1.0.19" exclude("org.slf4j", "slf4j-nop")
  )
}

