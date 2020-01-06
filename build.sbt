import NativePackagerHelper._
import RpmConstants._
import com.typesafe.sbt.packager.docker
import com.typesafe.sbt.packager.docker._
name := "projectlocker"

version := "1.0-dev"

//don't use RUNNING_PID file as that causes problems when we switch UIDs in Docker
//https://stackoverflow.com/questions/28351405/restarting-play-application-docker-container-results-in-this-application-is-alr
javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

lazy val `projectlocker` = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(AshScriptPlugin) //needed for alpine-based images
    .settings(
      version := sys.props.getOrElse("build.number","DEV"),
      dockerExposedPorts := Seq(9000),
      dockerUsername  := sys.props.get("docker.username"),
      dockerRepository := Some("andyg42"),
      packageName in Docker := "andyg42/projectlocker",
      packageName := "projectlocker",
      dockerBaseImage := "openjdk:8-jdk-slim",
      dockerPermissionStrategy := DockerPermissionStrategy.Run,
      dockerAlias := docker.DockerAlias(None,sys.props.get("docker.username"),"projectlocker",Some(sys.props.getOrElse("build.number","DEV"))),
      dockerCommands ++= Seq(
        Cmd("USER", "root"),
        Cmd("RUN", "apt-get","-y", "update", "&&", "apt-get", "-y", "install", "sudo", "perl", "iputils-ping"),
        Cmd("RUN", "mkdir -p /etc/projectlocker && mv /opt/docker/postrun/postrun_settings.py /etc/projectlocker && ln -s /etc/projectlocker/postrun_settings.py /opt/docker/postrun/postrun_settings.py"),
        Cmd("RUN", "mv", "/opt/docker/conf/docker-application.conf", "/opt/docker/conf/application.conf"),
        Cmd("RUN", "mkdir", "-p", "/opt/docker/target/persistence", "&&", "chown","demiourgos728", "/opt/docker/target/persistence"),
//        Cmd("RUN", "ls", "-lhd", "/opt/docker/target/persistence"),
        //Cmd("RUN", "chown -R daemon /opt/docker"),
        Cmd("USER", "demiourgos728"),
      )
    )

javaOptions in Test += "-Duser.timezone=UTC"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc, ehcache , ws   , specs2 % Test, guice )

libraryDependencies += evolutions

testOptions in Test ++= Seq( Tests.Argument("junitxml", "junit.outdir", sys.env.getOrElse("SBT_JUNIT_OUTPUT","/tmp")), Tests.Argument("console") )

PlayKeys.devSettings := Seq("play.akka.dev-mode.akka.http.server.request-timeout"->"120 seconds")

unmanagedResourceDirectories in Test +=  (baseDirectory ( _ /"target/web/public/test" )).value

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.5",
  // https://mvnrepository.com/artifact/com.typesafe.play/play-slick
  "com.typesafe.play" %% "play-slick" % "4.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "4.0.2",
  "commons-io" % "commons-io" % "2.6",
  // https://mvnrepository.com/artifact/com.typesafe.play/play-json-joda
  "com.typesafe.play" %% "play-json-joda" % "2.7.4",
  "commons-codec" % "commons-codec" % "1.12",
)
// https://mvnrepository.com/artifact/com.typesafe.slick/slick
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.2"


//authentication
libraryDependencies += "com.unboundid" % "unboundid-ldapsdk" % "4.0.5"

// https://mvnrepository.com/artifact/org.python/jython
libraryDependencies += "org.python" % "jython" % "2.7.1b2"

// upgrade jackson-databind to remove Deserialization of Untrusted Data vuln
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.11.1"

// upgrade guava to remove Deserialization of Untruseted Data vuln
// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "25.1-jre"

val akkaManagementVersion = "0.18.0"
//messaging persistence and clustering
libraryDependencies ++= Seq(
  "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-dns" % akkaManagementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-config" % akkaManagementVersion,
  "com.typesafe.akka" %% "akka-persistence" % "2.5.23",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.23",
  "com.typesafe.akka" %% "akka-cluster-metrics" % "2.5.23",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.23",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.23" % Test
)

//explicit akka upgrades for security
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-http-core" % "10.1.8",
  "com.typesafe.akka" %% "akka-parsing" % "10.1.8",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.8"
)

//Sentry
libraryDependencies += "io.sentry" % "sentry-logback" % "1.7.2"

//Reflections library for scanning classpath
libraryDependencies += "org.reflections" % "reflections" % "0.9.11"

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

