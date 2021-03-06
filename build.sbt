import ReleaseTransformations._

enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  configs(IntegrationTest)
  .settings(
    inThisBuild(List(
      organization := "com.pagantis",
      scalaVersion := "2.12.7"
    )),
    name := "tap-s3-json",
    Defaults.itSettings
  )

trapExit := false

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.3.4",
  "io.gatling" % "jsonpath_2.12" % "0.6.14",
  "com.iheart" %% "ficus" % "1.4.5",
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "1.0.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.21",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.21" % "it,test",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.21"% "it,test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "it,test"
)

bashScriptExtraDefines += s"""APP_VERSION=${version.value}"""

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runClean,                               // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion                       // : ReleaseStep
)
