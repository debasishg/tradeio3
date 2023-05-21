import sbt._
import Keys._
import Versions._

object Dependencies {
  def circe(artifact: String): ModuleID = "io.circe" %% s"circe-$artifact" % circeVersion

  object Zio {
    val zio                   = "dev.zio" %% "zio"                      % zioVersion
    val zioStreams            = "dev.zio" %% "zio-streams"              % zioVersion
    val zioPrelude            = "dev.zio" %% "zio-prelude"              % zioPreludeVersion
    val zioInteropCats        = "dev.zio" %% "zio-interop-cats"         % zioInteropCatsVersion
    val zioConfig             = "dev.zio" %% "zio-config"               % zioConfigVersion
    val zioConfigTypesafe     = "dev.zio" %% "zio-config-typesafe"      % zioConfigVersion
    val zioConfigMagnolia     = "dev.zio" %% "zio-config-magnolia"      % zioConfigVersion
    val zioLogging            = "dev.zio" %% "zio-logging-slf4j"        % zioLoggingVersion
    val zioLoggingSlf4j       = "dev.zio" %% "zio-logging-slf4j"        % zioLoggingVersion
    val zioLoggingSlf4jBridge = "dev.zio" %% "zio-logging-slf4j-bridge" % zioLoggingVersion
    val zioJson               = "dev.zio" %% "zio-json"                 % zioJsonVersion
    val zioTest               = "dev.zio" %% "zio-test"                 % zioVersion % "it,test"
    val zioTestSbt            = "dev.zio" %% "zio-test-sbt"             % zioVersion % "it,test"
  }
  object Cats {
    val cats       = "org.typelevel" %% "cats-core"   % catsVersion
    val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  }
  object Circe {
    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
  }
  object Skunk {
    val skunkCore  = "org.tpolecat" %% "skunk-core"  % skunkVersion
    val skunkCirce = "org.tpolecat" %% "skunk-circe" % skunkVersion
  }

  object Tapir {
    val tapirZioHttpServer = "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"   % tapirVersion
    val tapirJsonZio       = "com.softwaremill.sttp.tapir" %% "tapir-json-zio"          % tapirVersion
    val tapirSwagger       = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
  }
  val squants           = "org.typelevel"              %% "squants"               % squantsVersion
  val monocleCore       = "dev.optics"                 %% "monocle-core"          % monocleVersion
  val quickLens         = "com.softwaremill.quicklens" %% "quicklens"             % quickLensVersion
  val flywayDb          = "org.flywaydb"                % "flyway-core"           % flywayDbVersion
  val kantanCSV         = "com.nrinaudo"                % "kantan.csv_2.13"       % kantanCsvVersion
  val kantanCSVDateTime = "com.nrinaudo"                % "kantan.csv-java8_2.13" % kantanCsvVersion

  // Runtime
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime

  val commonDependencies: Seq[ModuleID] =
    Seq(
      Cats.cats,
      Cats.catsEffect,
      Zio.zio,
      Zio.zioPrelude,
      Zio.zioInteropCats,
      Zio.zioConfig,
      Zio.zioConfigMagnolia,
      Zio.zioConfigTypesafe,
      Zio.zioLogging,
      Zio.zioLoggingSlf4j,
      Zio.zioLoggingSlf4jBridge,
      Zio.zioJson,
      quickLens,
      kantanCSV,
      kantanCSVDateTime
    )

  val tradeioDependencies: Seq[ModuleID] =
    commonDependencies ++ Seq(squants) ++ Seq(flywayDb) ++
      Seq(Circe.circeCore, Circe.circeGeneric, Circe.circeParser) ++ Seq(monocleCore) ++
      Seq(Skunk.skunkCore, Skunk.skunkCirce) ++
      Seq(Tapir.tapirZioHttpServer, Tapir.tapirJsonZio, Tapir.tapirSwagger)

  val testDependencies: Seq[ModuleID] =
    Seq(Zio.zioTest, Zio.zioTestSbt)
}
