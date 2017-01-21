name := "projectlocker"

version := "1.0"

lazy val `projectlocker` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc, cache , ws   , specs2 % Test )

libraryDependencies += evolutions


unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "postgresql" % "postgresql" % "9.1-901.jdbc3",
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.2",
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)

enablePlugins(UniversalPlugin)

enablePlugins(LinuxPlugin)

enablePlugins(RpmPlugin)

//Generic Linux package build configuration

packageSummary in Linux := "A system to manage, backup and archive multimedia project files"

packageDescription in Linux := ""

//RPM build configuration
rpmVendor in Rpm := "Andy Gallagher <andy.gallagher@theguardian.com>"

rpmUrl in Rpm := Some("https://github/fredex42/projectlocker")

packageName in Rpm := "projectlocker"

version in Rpm := version.toString

rpmRelease in Rpm := "1"  //FIXME: replace with build number

packageArchitecture in Rpm := "noarch"

