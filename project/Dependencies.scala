import sbt._

object Dependencies {
  val core = Seq(Lib.commons)
}

object Version{
  val akka = "2.5.11"
  val akka_http = "10.0.11"
  val commons = "1.10"
  //scala
  val scala = "2.11.8"

  object min{
    val jdk = "1.8"
  }
}

object Lib{
  object akka{
    val http            = "com.typesafe.akka"           %% "akka-http"                  % Version.akka_http
    val http_spray_json = "com.typesafe.akka"           %% "akka-http-spray-json"       % Version.akka_http
    val http_xml        = "com.typesafe.akka"           %% "akka-http-xml"              % Version.akka_http
    val stream          = "com.typesafe.akka"           %% "akka-stream"                % Version.akka
    val remote          = "com.typesafe.akka"           %% "akka-remote"                % Version.akka
    val cluster         = "com.typesafe.akka"           %% "akka-cluster"               % Version.akka
    val all = Seq(http, remote)
  }
  val commons           = "commons-configuration"       % "commons-configuration"       % Version.commons
}
