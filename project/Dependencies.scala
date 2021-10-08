import sbt._
import Keys._
import Versions._

object Dependencies {

  object Cats {
    val cats       = "org.typelevel" %% "cats-core"   % catsVersion
    val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  }

  // Runtime
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime

  val commonDependencies: Seq[ModuleID] = Seq(Cats.cats, Cats.catsEffect)

  val tradeioDependencies: Seq[ModuleID] = 
    commonDependencies  
}
