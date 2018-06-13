import TestPhases.oneForkedJvmPerTest
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}

import scala.util.Properties.envOrElse

val appName = "bc-passengers-frontend"
val appVersion = envOrElse("BC_PASSENGERS_FRONTEND_VERSION", "999-SNAPSHOT")

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    retrieveManaged := true,
    version := appVersion
  )
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    shellPrompt := ShellPrompt(appVersion),
    parallelExecution in Test := false,
    fork in Test := false
  )
  .settings(Repositories.playPublishingSettings: _*)
  .settings(SbtBuildInfo(): _*)
  .configs(IntegrationTest)
  .settings(
    inConfig(IntegrationTest)(Defaults.itSettings),
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false
  )
  .settings(
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    )
  )
