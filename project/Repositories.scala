import sbt._
import sbt.Keys._
import uk.gov.hmrc.NexusPublishing._
import uk.gov.hmrc.PublishingSettings._

object Repositories {
  lazy val playPublishingSettings : Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++ Seq(
    credentials += SbtCredentials,

    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in(Compile, packageSrc) := false
  ) ++
    publishAllArtefacts ++
    nexusPublishingSettings
}
