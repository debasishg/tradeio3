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
import repository.live.TradeRepositoryLive
import service.{ ExchangeExecutionParsingService, FrontOfficeOrderParsingService, TradingService }
import service.live.{ ExchangeExecutionParsingServiceLive, FrontOfficeOrderParsingServiceLive, TradingServiceLive }
import java.io.IOException
import java.io.Reader

object TradeApp extends ZIOAppDefault:
  import TradeAppConfig.*
  import TradeAppComponents.*

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =

    val tradingCycle =
      ZIO.logInfo(s"Parsing front office orders ..") *>
        parseFrontOfficeOrders *>
        ZIO.logInfo(s"Parsing exchange executions ..") *>
        parseExchangeExecutions *>
        ZIO.logInfo(s"Generating trades ..") *>
        generateTrades

    setupDB.provide(config.appConfig)
      *> tradingCycle.provide(live)

object TradeAppConfig:
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

  val live: ZLayer[
    Any,
    Throwable,
    TradingService & FrontOfficeOrderParsingService & ExchangeExecutionParsingService
  ] = ZLayer
    .make[TradingService & FrontOfficeOrderParsingService & ExchangeExecutionParsingService](
      TradingServiceLive.layer,
      OrderRepositoryLive.layer,
      ExecutionRepositoryLive.layer,
      TradeRepositoryLive.layer,
      FrontOfficeOrderParsingServiceLive.layer,
      ExchangeExecutionParsingServiceLive.layer,
      appResourcesL.project(_.postgres),
      appResourcesL,
      config.appConfig
    )

object TradeAppComponents:
  val tradeDate = LocalDate.of(2023, 5, 28)
  val generateTrades = for
    now  <- zio.Clock.instant
    uuid <- zio.Random.nextUUID
    trades <- ZIO
      .serviceWithZIO[TradingService](
        _.generateTrades(tradeDate, UserId(uuid)).runCollect
      )
    _ <- ZIO.logInfo(s"Done generating ${trades.size} trades")
    _ <- ZIO.logInfo(s"$trades")
  yield ()

  private def reader(name: String): ZIO[Scope, IOException, Reader] =
    ZStream
      .fromResource(name)
      .via(ZPipeline.decodeCharsWith(StandardCharsets.UTF_8))
      .toReader

  private def withCSV(name: String): ZIO[Scope, IOException, Reader] =
    ZIO
      .acquireRelease(reader(name))(rdr => ZIO.succeedBlocking(rdr.close()))

  val parseFrontOfficeOrders: ZIO[FrontOfficeOrderParsingService, Throwable, Unit] =
    ZIO.scoped(
      withCSV("forders.csv")
        .flatMap(reader => ZIO.serviceWithZIO[FrontOfficeOrderParsingService](_.parse(reader)))
    )

  val parseExchangeExecutions: ZIO[ExchangeExecutionParsingService, Throwable, Unit] =
    ZIO.scoped(
      withCSV("executions.csv")
        .flatMap(reader => ZIO.serviceWithZIO[ExchangeExecutionParsingService](_.parse(reader)))
    )
