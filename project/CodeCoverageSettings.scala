import sbt.Setting
import scoverage.ScoverageKeys.*

object CodeCoverageSettings {
  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*definition.*",
    "prod.*",
    "live.*",
    "testOnlyDoNotUseInAppConf.*",
    "app.*",
    "uk.gov.hmrc.BuildInfo"
  )

  val settings: Seq[Setting[?]] = Seq(
    coverageExcludedFiles := "<empty>;.*components.*;.*Routes.*;",
    coverageMinimumStmtTotal := 99,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )
}
