import uk.gov.hmrc.DefaultBuildSettings._

val appName = "bc-passengers-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.12"

// To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SbtWeb)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies(),
    pipelineStages := Seq(digest),
    PlayKeys.playDefaultPort := 9008
  )
  // To resolve dependency clash between flexmark v0.64.4+ and play-language to run accessibility tests, remove when versions align
  .settings(dependencyOverrides += "com.ibm.icu" % "icu4j" % "69.1")
  .settings(
    coverageExcludedFiles := "<empty>;.*components.*;.*Routes.*;",
    coverageMinimumStmtTotal := 99,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
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
        "lib/govuk-frontend/govuk/all.js",
        "javascripts/jquery.min.js",
        "javascripts/autocomplete.js"
      )
    )
)

Assets / pipelineStages := Seq(concat, uglify)
uglify / includeFilter := GlobFilter("application.js")

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt A11y/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle it/Test/scalastyle A11y/scalastyle")
