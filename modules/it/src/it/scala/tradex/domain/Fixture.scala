package tradex.domain

import zio.ZIO
import tradex.domain.config.AppConfig
import zio.{ Task, ZLayer }
import tradex.domain.resources.AppResources
import zio.interop.catz.*
import cats.effect.std.Console
import natchez.Trace.Implicits.noop

object Fixture {

  val setupDB =
    for dbConf <- ZIO.serviceWith[AppConfig](_.postgreSQL)
    yield ()

  given Console[Task] = Console.make[Task]

  val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
    for
      config <- ZIO.service[AppConfig]
      res    <- AppResources.make(config).toScopedZIO
    yield res
  )
}
