name := "plista-challenge"

version := "0.1"

scalaVersion := "2.12.6"

cancelable in Global := true
fork in run := true
javaOptions += "-Dfile.encoding=UTF-8"
javaOptions += "-Xmx6G"
scalacOptions ++= Seq("-encoding", "utf-8")


libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.1.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.1.0" // scalactic seems to depend on this
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"