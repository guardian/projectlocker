import NativePackagerHelper._
import RpmConstants._

name := "projectlocker"

version := "1.0"

lazy val `projectlocker` = (project in file(".")).enablePlugins(PlayScala)

javaOptions in Test += "-Duser.timezone=UTC"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc, ehcache , ws   , specs2 % Test, guice )

libraryDependencies += evolutions

concurrentRestrictions in Global := Seq(
  Tags.limit(Tags.Test, 1),
  Tags.limitAll(1)
)

PlayKeys.devSettings := Seq("play.akka.dev-mode.akka.http.server.request-timeout"->"120 seconds")


unmanagedResourceDirectories in Test +=  (baseDirectory ( _ /"target/web/public/test" )).value

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "postgresql" % "postgresql" % "9.1-901.jdbc3",
  // https://mvnrepository.com/artifact/com.typesafe.play/play-slick
  "com.typesafe.play" %% "play-slick" % "3.0.3",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3",
  "commons-io" % "commons-io" % "2.6",
  // https://mvnrepository.com/artifact/com.typesafe.play/play-json-joda
  "com.typesafe.play" %% "play-json-joda" % "2.6.9"
)
// https://mvnrepository.com/artifact/com.typesafe.slick/slick
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.2.2"


//authentication
libraryDependencies += "com.unboundid" % "unboundid-ldapsdk" % "2.3.6"

// https://mvnrepository.com/artifact/org.python/jython
libraryDependencies += "org.python" % "jython" % "2.7.1b2"

// upgrade jackson-databind to remove Deserialization of Untrusted Data vuln
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.11.1"

//messaging persistence and clustering
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % "2.5.11",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.11",
  "com.typesafe.akka" %% "akka-cluster-metrics" % "2.5.11",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.11",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
)

//Sentry
libraryDependencies += "io.sentry" % "sentry-logback" % "1.7.2"


enablePlugins(UniversalPlugin)

enablePlugins(LinuxPlugin)

enablePlugins(RpmPlugin, JavaServerAppPackaging, SystemdPlugin, DockerPlugin)

//Generic Linux package build configuration
mappings in Universal ++= directory("postrun/")

packageSummary in Linux := "A system to manage, backup and archive multimedia project files"

packageDescription in Linux := "A system to manage, backup and archive multimedia project files"

//RPM build configuration
rpmVendor := "Andy Gallagher <andy.gallagher@theguardian.com>"

rpmUrl := Some("https://github/fredex42/projectlocker")

rpmRequirements := Seq("libxml2", "gzip")

serverLoading in Universal := Some(ServerLoader.Systemd)

packageName in Rpm := "projectlocker"

version in Rpm := "1.0"

rpmRelease := sys.props.getOrElse("build.number","DEV")

packageArchitecture := "noarch"

rpmLicense := Some("custom")

maintainerScripts in Rpm := Map(
  Post -> Seq("cp -f /usr/share/projectlocker/conf/sudo-trapdoor /etc/sudoers.d/projectlocker")
)