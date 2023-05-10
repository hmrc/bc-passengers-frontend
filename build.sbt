import uk.gov.hmrc.DefaultBuildSettings._

val appName = "bc-passengers-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SbtWeb)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true
  )
  .settings(
    pipelineStages := Seq(digest)
  )
  .settings(scalaVersion := "2.13.10")
  // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
  .settings(libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always))
  .settings(defaultSettings(): _*)
  .settings(majorVersion := 1)
  .settings(
    coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*FrontendAuditConnector.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LocalLanguageController;.*testonly.*;",
    coverageMinimumStmtTotal := 97,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )
  .configs(IntegrationTest)
  .settings(
    integrationTestSettings(),
    routesImport ++= Seq("binders.Binders._", "models._")
  )
  .settings(PlayKeys.playDefaultPort := 9008)
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "views.ViewUtils._",
      "controllers.routes._"
    ),
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=html/.*:s"
    )
  )

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

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
