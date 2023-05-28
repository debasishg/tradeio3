ThisBuild / scalaVersion := "3.2.2"
ThisBuild / version := "0.0.1"
ThisBuild / organization := "dev.tradex"
ThisBuild / organizationName := "tradex"

ThisBuild / evictionErrorLevel := Level.Warn
ThisBuild / IntegrationTest / parallelExecution := false

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val root = (project in file("."))
  .settings(
    name := "tradeio3"
  )
  .aggregate(core, tests, it)

lazy val core = (project in file("modules/core")).settings(
  name := "tradeio-core",
  commonSettings,
  consoleSettings,
  dependencies
)

lazy val tests = (project in file("modules/tests"))
  .settings(commonSettings: _*)
  .configs(IntegrationTest)
  .settings(
    name           := "tradeio3-test-suite",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    testDependencies
  )
  .dependsOn(core)

lazy val it = (project in file("modules/it"))
  .settings(commonSettings: _*)
  .configs(IntegrationTest)
  .settings(
    name           := "tradeio3-it-suite",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    scalafmtOnCompile := true,
    Defaults.itSettings,
    itDependencies
  )
  .dependsOn(core % "it->test")

lazy val commonSettings = Seq(
  scalafmtOnCompile := true,
  resolvers ++= Resolver.sonatypeOssRepos("snapshots")
)

lazy val consoleSettings = Seq(
  Compile / console / scalacOptions --= Seq("-Ywarn-unused", "-Ywarn-unused-import")
)

lazy val compilerOptions = {
  val commonOptions = Seq(
    "-unchecked",
    "-deprecation",
    "-encoding",
    "utf8",
    "-target:jvm-1.8",
    "-feature",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps",
    "-Ywarn-value-discard",
    "-Ymacro-annotations",
    "-Ywarn-unused:imports",
    "-Xmax-inlines=64"
  )

  scalacOptions ++= commonOptions
}

lazy val dependencies =
  libraryDependencies ++= Dependencies.tradeioDependencies

lazy val testDependencies =
  libraryDependencies ++= Dependencies.testDependencies

lazy val itDependencies =
  libraryDependencies ++= Dependencies.testDependencies
