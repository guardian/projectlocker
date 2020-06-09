logLevel := Level.Info

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.2")

// sbt native packager

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.3")
