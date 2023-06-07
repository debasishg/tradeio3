package tradex.domain
package service

import zio.test.*
import zio.test.Assertion.*
import zio.{ Scope, ZIO }
import zio.stream.{ ZPipeline, ZStream }
import java.nio.charset.StandardCharsets
import java.time.{ Instant, LocalDate, ZoneOffset }
import csv.CSV
import model.frontOfficeOrder.FrontOfficeOrder
import model.exchangeExecution.ExchangeExecution
import model.order.Order
import model.market.Market
import transport.frontOfficeOrderT.{ *, given }
import transport.exchangeExecutionT.{ *, given }
import service.live.FrontOfficeOrderParsingServiceLive
import service.live.ExchangeExecutionParsingServiceLive
import repository.live.OrderRepositoryLive
import repository.OrderRepository
import repository.live.ExecutionRepositoryLive
import repository.ExecutionRepository
import Fixture.appResourcesL
import generators.frontOfficeOrderGen

object OrderExecutionParsingSpec extends ZIOSpecDefault:
  val now = Instant.now
  override def spec = suite("OrderExecutionParsingSpec")(
    test("parse front office orders and create orders")(
      check(Gen.listOfN(5)(frontOfficeOrderGen(now)))(frontOfficeOrders =>
        for
          _ <- parseFrontOfficeOrders(frontOfficeOrders)

          s <- ZStream
            .fromIterable(frontOfficeOrders)
            .via(CSV.encode[FrontOfficeOrder])
            .runCollect
            .map(bytes => new String(bytes.toArray))

          _ <- ZIO.logInfo(s)

          ordersInserted <- ZIO
            .serviceWithZIO[OrderRepository](
              _.queryByOrderDate(LocalDate.ofInstant(now, ZoneOffset.UTC))
            )
          _ <- parseExchangeExecutions(ordersInserted)
          _ <- generateExchangeExecutionsCSV(ordersInserted)
          exesInserted <- ZIO.serviceWithZIO[ExecutionRepository](
            _.query(LocalDate.ofInstant(now, ZoneOffset.UTC))
          )
        yield assertTrue(
          ordersInserted.nonEmpty,
          exesInserted.nonEmpty
        )
      )
    ) @@ TestAspect.samples(10) @@ TestAspect.sequential @@ TestAspect.after(clean)
  )
    .provideSome[Scope](
      FrontOfficeOrderParsingServiceLive.layer,
      ExchangeExecutionParsingServiceLive.layer,
      OrderRepositoryLive.layer,
      ExecutionRepositoryLive.layer,
      config.appConfig,
      appResourcesL.project(_.postgres),
      Sized.default,
      TestRandom.deterministic
    )

  private def parseFrontOfficeOrders(frontOfficeOrders: List[FrontOfficeOrder]) = for
    service <- ZIO.service[FrontOfficeOrderParsingService]
    reader <- ZStream
      .fromIterable(frontOfficeOrders)
      .via(CSV.encode[FrontOfficeOrder])
      .via(ZPipeline.decodeCharsWith(StandardCharsets.UTF_8))
      .toReader
    _ <- service.parse(reader)
  yield ()

  private def parseExchangeExecutions(ordersInserted: List[Order]) = for
    exchangeExes <- generateExchangeExecutions(ordersInserted, now)
    reader <- ZStream
      .fromIterable(exchangeExes)
      .via(CSV.encode[ExchangeExecution])
      .via(ZPipeline.decodeCharsWith(StandardCharsets.UTF_8))
      .toReader
    service <- ZIO.service[ExchangeExecutionParsingService]
    _       <- service.parse(reader)
  yield ()

  private def generateExchangeExecutions(orders: List[Order], now: Instant) =
    ZIO
      .foreachPar(orders)(order => ExchangeExecution.fromOrder(order, Market.NewYork, now).map(_.toList))
      .map(_.flatten)

  private def generateExchangeExecutionsCSV(ordersInserted: List[Order]) = for
    exchangeExes <- generateExchangeExecutions(ordersInserted, now)
    csv <- ZStream
      .fromIterable(exchangeExes)
      .via(CSV.encode[ExchangeExecution])
      .runCollect
      .map(bytes => new String(bytes.toArray))
    _ <- ZIO.logInfo(csv)
  yield ()

  private def clean = for
    _ <- ZIO.serviceWithZIO[ExecutionRepository](_.cleanAllExecutions)
    _ <- ZIO.serviceWithZIO[OrderRepository](_.cleanAllOrders)
  yield ()
