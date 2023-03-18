import sbt._
import Keys._
import Versions._

object Dependencies {
  def circe(artifact: String): ModuleID     = "io.circe"          %% s"circe-$artifact"     % circeVersion


  object Zio {
    val zio               = "dev.zio" %% "zio"                 % zioVersion
    val zioStreams        = "dev.zio" %% "zio-streams"         % zioVersion
    val zioPrelude        = "dev.zio" %% "zio-prelude"         % zioPreludeVersion
    val zioInteropCats    = "dev.zio" %% "zio-interop-cats"    % zioInteropCatsVersion
  }
  object Cats {
    val cats       = "org.typelevel" %% "cats-core"   % catsVersion
    val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  }
  object Circe {
    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeRefined = circe("refined")
  }
  object Skunk {
    val skunkCore  = "org.tpolecat" %% "skunk-core"  % skunkVersion
    val skunkCirce = "org.tpolecat" %% "skunk-circe" % skunkVersion
  }
  val squants = "org.typelevel" %% "squants"  % squantsVersion
  val monocleCore = "dev.optics"      %% "monocle-core" % monocleVersion

  // Runtime
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime

  val commonDependencies: Seq[ModuleID] = Seq(Cats.cats, Cats.catsEffect, Zio.zio, Zio.zioPrelude, Zio.zioInteropCats)

  val tradeioDependencies: Seq[ModuleID] = 
    commonDependencies ++ Seq(squants) ++
      Seq(Circe.circeCore, Circe.circeGeneric, Circe.circeParser, Circe.circeRefined) ++ Seq(monocleCore) ++
      Seq(Skunk.skunkCore, Skunk.skunkCirce)
}
