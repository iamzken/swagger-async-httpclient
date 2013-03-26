import sbt._
import Keys._
import sbtrelease.ReleasePlugin._
import sbtbuildinfo.Plugin._

object build extends Build {

  val manifestSetting = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor)
  }

  val publishSettings: Seq[Setting[_]] = Seq(
    publishTo <<= (version) { version: String =>
      val res =
        if (version.trim.endsWith("SNAPSHOT"))
          Opts.resolver.sonatypeSnapshots
        else
          Opts.resolver.sonatypeStaging
      Some(res)
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false }
  )

  def versionSpecificSourcesIn(c: Configuration) =
    unmanagedSourceDirectories in c <+= (scalaVersion, sourceDirectory in c) {
      case (v, dir) if v startsWith "2.9" => dir / "scala_2.9"
      case (v, dir) if v startsWith "2.10" => dir / "scala_2.10"
    }

  val projectSettings = Seq(
    organization := "com.wordnik.swagger",
    name := "swagger-async-httpclient",
    scalaVersion := "2.10.0",
    crossScalaVersions := Seq("2.9.1", "2.9.1-1", "2.9.2", "2.9.3", "2.10.0"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xcheckinit", "-encoding", "utf8", "-P:continuations:enable"),
    scalacOptions in Compile <++= scalaVersion map ({
      case v if v startsWith "2.10" => Seq("-language:implicitConversions", "-language:reflectiveCalls")
      case _ => Seq.empty
    }),
    buildInfoPackage := "com.wordnik.swagger.client.async",
    javacOptions in compile ++= Seq("-target", "1.6", "-source", "1.6", "-Xlint:deprecation"),
    manifestSetting,
    autoCompilerPlugins := true,
    libraryDependencies <+= scalaVersion(sv => compilerPlugin("org.scala-lang.plugins" % "continuations" % sv)),
    parallelExecution in Test := false,
    commands += Command.args("s", "<shell command>") { (state, args) =>
      args.mkString(" ") ! state.log
      state
    },
    TaskKey[Unit]("gc", "runs garbage collector") <<= streams map { s =>
      s.log.info("requesting garbage collection")
      System.gc()
    }
  )

  val buildInfoConfig: Seq[Setting[_]] = buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage <<= organization(_ + ".client.async")
  )

  val defaultSettings =
    Defaults.defaultSettings ++ releaseSettings ++ buildInfoConfig ++ projectSettings ++ publishSettings


  lazy val root = Project(
    id = "swagger-async-httpclient",
    base = file("."),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scalatra.rl" %% "rl" % "0.4.3",
        "org.slf4j" % "slf4j-api" % "1.7.3",
        "ch.qos.logback" % "logback-classic" % "1.0.10" % "provided",
        "org.json4s" %% "json4s-jackson" % "3.2.2",
        "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3",
        "eu.medsea.mimeutil" % "mime-util" % "2.1.3" exclude("org.slf4j", "slf4j-log4j12") exclude("log4j", "log4j"),
        "com.ning" % "async-http-client" % "1.7.9"
      ),
      libraryDependencies <+= scalaVersion {
         case v if v startsWith "2.9" => "org.clapper" %% "grizzled-slf4j" % "0.6.10"
         case v => "com.typesafe" %% "scalalogging-slf4j" % "1.0.1"
      },
      libraryDependencies <+= scalaVersion {
         case "2.9.3" => "org.scalatra.rl" % "rl_2.9.2" % "0.4.3"
         case v => "org.scalatra.rl" %% "rl" % "0.4.3"
       },
       libraryDependencies <++= scalaVersion {
        case v if v startsWith "2.9" => Seq("com.typesafe.akka" % "akka-actor" % "2.0.5")
        case v => Seq.empty
      },
      versionSpecificSourcesIn(Compile)
    )
  )
}