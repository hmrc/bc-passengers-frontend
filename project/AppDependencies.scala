import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val hmrcMongoVersion     = "1.7.0"
  private val bootstrapPlayVersion = "8.4.0"

  private val compile        = Seq(
    ws,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc-play-30" % "8.3.0",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-30" % bootstrapPlayVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.16.1",
    "org.webjars.npm"               % "accessible-autocomplete"    % "2.0.4",
    "ai.x"                         %% "play-json-extensions"       % "0.42.0",
    "org.typelevel"                %% "cats-core"                  % "2.10.0"
  )
  private val test           = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.mockito"       %% "mockito-scala-scalatest" % "1.17.30"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
