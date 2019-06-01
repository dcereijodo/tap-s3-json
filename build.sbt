lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.pagantis",
      scalaVersion := "2.12.7"
    )),
    name := "tap-s3-json",
    // required for the assembly plugin
    // https://github.com/sbt/sbt-assembly
    mainClass in assembly := Some("com.pagantis.singer.taps.TapS3Json")
  )

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.3.4",
  "io.gatling" % "jsonpath_2.12" % "0.6.14",
  "com.iheart" %% "ficus" % "1.4.5",
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "1.0.0",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.21",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.21" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.21" % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

lazy val make = inputKey[Unit]("Create a binary with the tap as a command line util")
make := {
  "cat stub.sh target/scala-2.12/tap-s3-json-assembly-0.1-SNAPSHOT.jar" #> file("tap-s3-json") !;
  "chmod +x tap-s3-json" !
}
lazy val install = inputKey[Unit]("Install tap as a command line util")
install := {
  "cp tap-s3-json /usr/local/bin" !
}