import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.Keys._
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "govuk-template" % "5.26.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "7.38.0-play-26",
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.32.0",
    "uk.gov.hmrc" %% "http-caching-client" % "8.0.0",
    "com.typesafe.play" %% "play-json-joda" % "2.6.0",
    "org.mindrot" % "jbcrypt" % "0.4"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.3.0" % "test,it",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test,it",
    "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
    "org.jsoup" % "jsoup" % "1.11.3" % "test,it",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
    "org.mockito" % "mockito-all" % "2.0.2-beta" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test",
    "com.networknt" % "json-schema-validator" % "0.1.24" exclude("org.slf4j", "slf4j-nop")
  )
}

