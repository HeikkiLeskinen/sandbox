name := "sandbox"
version := "1.0"
scalaVersion := "2.11.7"

fork in run := true
cancelable in Global := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.2"
)