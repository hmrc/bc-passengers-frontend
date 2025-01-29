import uk.gov.hmrc.DefaultBuildSettings.itSettings

val appName = "bc-passengers-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "3.5.1"
ThisBuild / excludeDependencies ++= Seq(
  // As of Play 3.0, groupId has changed to org.playframework; exclude transitive dependencies to the old artifacts
  // Specifically affects play-json-extensions dependency
  ExclusionRule(organization = "com.typesafe.play")
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SbtWeb)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(CodeCoverageSettings.settings)
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

Concat.groups := Seq(
  "javascripts/application.js" ->
    group(
      Seq(
        "javascripts/jquery.min.js",
        "javascripts/autocomplete.js"
      )
    )
)

Assets / pipelineStages := Seq(concat)

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt")
