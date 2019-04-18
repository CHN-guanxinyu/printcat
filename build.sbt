lazy val printcat = preownedKittenProject("printcat", ".").
  settings(
    onLoadMessage ~= (_ + (if ((sys props "java.specification.version") < Version.min.jdk) {
      s"""
         |You seem to not be running Java ${Version.min.jdk}.
         |While the provided code may still work, we recommend that you
         |upgrade your version of Java.
    """.stripMargin
    } else "")), libraryDependencies ++= Lib.akka.all
  ).settings(CommonSetting.projectSettings)



/**
  * 创建通用模板
  */
def preownedKittenProject(
  name : String,
  path : String
) : Project = {
  Project(name, file(path)).
    settings(
      version := "1.0-SNAPSHOT",
      organization := "com.cartury",
      scalaVersion := Version.scala
    )
}