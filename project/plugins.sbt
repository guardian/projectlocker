logLevel := Level.Info

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.10")

// sbt native packager

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.4")
