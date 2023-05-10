package tradex.domain

import zio.{ Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer }
import zio.stream.{ ZPipeline, ZStream }
import zio.interop.catz.*
import cats.effect.std.Console
import natchez.Trace.Implicits.noop
import java.time.{ LocalDate, ZoneOffset }
import java.util.UUID
import java.nio.charset.StandardCharsets

import config.AppConfig
import resources.AppResources
import model.user.UserId
import repository.live.OrderRepositoryLive
import repository.live.ExecutionRepositoryLive
import service.{ ExchangeExecutionParsingService, FrontOfficeOrderParsingService, TradingService }
import service.live.{ ExchangeExecutionParsingServiceLive, FrontOfficeOrderParsingServiceLive, TradingServiceLive }

object TradeApp extends ZIOAppDefault:

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    val setupDB = for {
      dbConf <- ZIO.serviceWith[AppConfig](_.postgreSQL)
    } yield ()

    given Console[Task] = Console.make[Task]

    val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
      for
        config <- ZIO.service[AppConfig]
        res    <- AppResources.make(config).toScopedZIO
      yield res
    )

    val live: ZLayer[
      Any,
      Throwable,
      TradingService & FrontOfficeOrderParsingService & ExchangeExecutionParsingService
    ] = ZLayer
      .make[TradingService & FrontOfficeOrderParsingService & ExchangeExecutionParsingService](
        TradingServiceLive.layer,
        OrderRepositoryLive.layer,
        ExecutionRepositoryLive.layer,
        FrontOfficeOrderParsingServiceLive.layer,
        ExchangeExecutionParsingServiceLive.layer,
        appResourcesL.project(_.postgres),
        appResourcesL,
        config.appConfig
      )

    def reader(name: String) = ZIO
      .scoped(
        ZIO.fromAutoCloseable(
          ZStream
            .fromResource(name)
            .via(ZPipeline.decodeCharsWith(StandardCharsets.UTF_8))
            .toReader
        )
      )

    val genTrades = for
      service <- ZIO.service[TradingService]
      now     <- zio.Clock.instant
      uuid    <- zio.Random.nextUUID
      trades  <- service.generateTrades(LocalDate.ofInstant(now, ZoneOffset.UTC), UserId(uuid)).runCollect
      _       <- ZIO.logInfo(s"Done generating ${trades.size} trades")
    yield ()

    val tradingCycle = (reader("order.csv") <&> reader("execution.csv")).flatMap { case (order, execution) =>
      (ZIO.serviceWithZIO[FrontOfficeOrderParsingService](_.parse(order)) <&>
        ZIO.serviceWithZIO[ExchangeExecutionParsingService](_.parse(execution))).flatMap(_ => genTrades)
    }

    setupDB.provide(config.appConfig)
      *> tradingCycle.provide(live)
