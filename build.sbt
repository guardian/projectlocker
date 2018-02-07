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
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)

enablePlugins(UniversalPlugin)

enablePlugins(LinuxPlugin)

enablePlugins(RpmPlugin)

//Generic Linux package build configuration

packageSummary in Linux := "A system to manage, backup and archive multimedia project files"

packageDescription in Linux := "A system to manage, backup and archive multimedia project files"

//RPM build configuration
rpmVendor in Rpm := "Andy Gallagher <andy.gallagher@theguardian.com>"

rpmUrl in Rpm := Some("https://github/fredex42/projectlocker")

packageName in Rpm := "projectlocker"

version in Rpm := "1.0"

rpmRelease in Rpm := sys.props.getOrElse("build.number","DEV")

packageArchitecture in Rpm := "noarch"

rpmLicense in Rpm := Some("custom")
