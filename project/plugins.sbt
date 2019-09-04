resolvers ++= Seq(Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns))
resolvers += "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.16.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "1.19.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.10")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.23")

addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "3.11.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.6.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory" % "0.19.0")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.12")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")