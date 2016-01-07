import sbt._
import Keys._

object SlimeBuild extends Build {

  object V { }

  val projectName         = "slime"
  val projectVersion      = "0.0.1-SNAPSHOT"

  val projectDependencies = Seq()

  val projectSettings: Seq[Setting[_]] = Seq(
    version := projectVersion,
    organization := "com.cyberdolphins",
    libraryDependencies ++= projectDependencies,
    scalaVersion        := "2.11.4",
    resolvers           ++= Seq(Resolver.mavenLocal),
    scalacOptions       := Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions", "-language:postfixOps"),
    fork in test := true,
    javaOptions in test += "-XX:MaxPermSize=512M -Xmx1024M -Xms1024M -Duser.timezone=UTC -Djava.library.path=/usr/local/lib",
    resourceDirectory in Test <<= baseDirectory apply {(baseDir: File) => baseDir / "test" / "resources"}
  )

  val main = Project(projectName, file("."))
    .settings(projectSettings: _*)
}
