// https://github.com/sbt/sbt-scalariform
// to format scala source code
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

// enable updating file headers eg. for copyright
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "3.0.2")

libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.17"
libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.11.245"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.7"