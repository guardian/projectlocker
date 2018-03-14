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

parallelExecution in Test := false

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

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

enablePlugins(UniversalPlugin)

enablePlugins(LinuxPlugin)

enablePlugins(RpmPlugin, JavaServerAppPackaging, SystemdPlugin)

//Generic Linux package build configuration
mappings in Universal ++= directory("postrun/")

packageSummary in Linux := "A system to manage, backup and archive multimedia project files"

packageDescription in Linux := "A system to manage, backup and archive multimedia project files"

//RPM build configuration
rpmVendor := "Andy Gallagher <andy.gallagher@theguardian.com>"

rpmUrl := Some("https://github/fredex42/projectlocker")

serverLoading in Universal := Some(ServerLoader.Systemd)

packageName in Rpm := "projectlocker"

version in Rpm := "1.0"

rpmRelease := sys.props.getOrElse("build.number","DEV")

packageArchitecture := "noarch"

rpmLicense := Some("custom")

maintainerScripts in Rpm := Map(
  Post -> Seq("echo projectlocker   ALL = (root) NOPASSWD: /usr/share/projectlocker/postrun/scripts/mkdir_on_behalf_of.pl > /etc/sudoers.d/projectlocker"),
  Preun -> Seq("rm -f /etc/sudoers.d/projectlocker")
)