package tradex.domain

import zio.{ Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer }
import zio.interop.catz.*
import cats.effect.std.Console
import natchez.Trace.Implicits.noop
import java.time.{ LocalDate, ZoneOffset }
import java.util.UUID

import config.AppConfig
import resources.AppResources
import model.user.UserId
import repository.live.OrderRepositoryLive
import service.TradingService
import service.live.TradingServiceLive

object TradeApp extends ZIOAppDefault:

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    val setupDB = for {
      dbConf <- ZIO.serviceWith[AppConfig](_.postgreSQL)
      _      <- FlywayMigration.migrate(dbConf)
    } yield ()

    given Console[Task] = Console.make[Task]

    val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
      for
        config <- ZIO.service[AppConfig]
        res    <- AppResources.make(config).toScopedZIO
      yield res
    )

    val live: ZLayer[Any, Throwable, TradingService] = ZLayer
      .make[TradingService](
        TradingServiceLive.layer,
        OrderRepositoryLive.layer,
        appResourcesL.project(_.postgres),
        appResourcesL,
        config.appConfig
      )

    val genTrades = for
      service <- ZIO.service[TradingService]
      now     <- zio.Clock.instant
      uuid    <- zio.Random.nextUUID
      trades  <- service.generateTrades(LocalDate.ofInstant(now, ZoneOffset.UTC), UserId(uuid)).runCollect
    yield trades

    setupDB.provide(config.appConfig)
      *> genTrades.provide(live)
