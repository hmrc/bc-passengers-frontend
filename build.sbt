import uk.gov.hmrc.DefaultBuildSettings.itSettings

val appName = "bc-passengers-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "3.5.2"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SbtWeb)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(CodeCoverageSettings())
  .settings(
    libraryDependencies ++= AppDependencies(),
    pipelineStages := Seq(digest),
    PlayKeys.playDefaultPort := 9008
  )
  .settings(
    routesImport ++= Seq("binders.Binders._", "models._")
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "views.ViewUtils._",
      "controllers.routes._"
    ),
    scalacOptions ++= List(
      "-feature",
      "-Wconf:msg=unused import&src=conf/.*:s",
      "-Wconf:msg=unused import&src=views/.*:s",
      "-Wconf:src=routes/.*:s"
    )
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(itSettings())

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt")
