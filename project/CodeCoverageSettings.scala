import sbt.Setting
import scoverage.ScoverageKeys.*

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*Routes.*"
  )

  private val settings: Seq[Setting[?]] = Seq(
    coverageExcludedFiles := excludedPackages.mkString(";"),
    coverageMinimumStmtTotal := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )

  def apply(): Seq[Setting[?]] = settings

}
