import sbt.*

object AppDependencies {

  private val hmrcMongoVersion     = "2.3.0"
  private val bootstrapPlayVersion = "9.5.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc-play-30" % "11.6.0",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-30" % bootstrapPlayVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.18.1",
    "org.webjars.npm"               % "accessible-autocomplete"    % "3.0.0",
    "ai.x"                         %% "play-json-extensions"       % "0.42.0" //ExclusionRule in build.sbt for old play-json version
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test
}
