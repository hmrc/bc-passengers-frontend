import uk.gov.hmrc.DefaultBuildSettings.itSettings

val appName = "bc-passengers-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.14"
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
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=views/.*:s"
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

Assets / pipelineStages := Seq(concat, uglify)
uglify / includeFilter := GlobFilter("application.js")

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt A11y/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle it/Test/scalastyle A11y/scalastyle")
