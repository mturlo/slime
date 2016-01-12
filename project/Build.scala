import sbt._
import Keys._

object SlimeBuild extends Build {

  object V {

    val javaWebsockets = "1.3.0"
    val akka = "2.3.4"
    val play = "2.4.4"

  }

  val projectName         = "slime"
  val projectVersion      = "0.1.2-SNAPSHOT"

  val projectDependencies = Seq(
    "org.java-websocket" % "Java-WebSocket" % V.javaWebsockets withSources(),
    "com.typesafe.akka" %% "akka-actor" % V.akka withSources(),
    "com.typesafe.play" %% "play-json" % V.play withSources(),
    "com.typesafe.play" %% "play-ws" % V.play withSources(),
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )

  val projectSettings: Seq[Setting[_]] = Seq(
    version := projectVersion,
    organization := "com.cyberdolphins",
    libraryDependencies ++= projectDependencies,
    scalaVersion        := "2.11.4",
    resolvers           ++= Seq(Resolver.mavenLocal, "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"),
    scalacOptions       := Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions", "-language:postfixOps"),
    fork in test := true,
    javaOptions in test += "-XX:MaxPermSize=512M -Xmx1024M -Xms1024M -Duser.timezone=UTC -Djava.library.path=/usr/local/lib",
    resourceDirectory in Test <<= baseDirectory apply {(baseDir: File) => baseDir / "test" / "resources"},
    mainClass in (Compile, run) := Some("com.cyberdolphins.slime.Slime")
  )

  val main = Project(projectName, file("."))
    .settings(projectSettings: _*)
}
