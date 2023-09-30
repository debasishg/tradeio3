package tradex.domain
package service
package live

import java.time.LocalDate
import zio.{ Chunk, Task, UIO, ZIO, ZLayer }
import zio.stream.{ ZPipeline, ZStream }
import zio.interop.catz.*
import zio.stream.interop.fs2z.*
import skunk.Session

import model.account.*
import model.trade.*
import model.execution.*
import model.order.*
import model.user.*
import repository.{ ExecutionRepository, OrderRepository, TradeRepository }
import repository.live.ExecutionRepositorySQL
import resources.AppResources

final case class TradingServiceLive(
    session: Session[Task],
    orderRepository: OrderRepository,
    tradeRepository: TradeRepository
) extends TradingService:

  override def queryTradesForDate(accountNo: AccountNo, date: LocalDate): UIO[List[Trade]] =
    tradeRepository.query(accountNo, date)

  override def queryExecutionsForDate(date: LocalDate): ZStream[Any, Throwable, Execution] =
    ZStream
      .fromZIO(session.prepare(ExecutionRepositorySQL.selectByExecutionDate))
      .flatMap: preparedQuery =>
        preparedQuery
          .stream(date, 512)
          .toZStream()

  override def getAccountNoFromExecution: ZPipeline[Any, Throwable, Execution, (Execution, AccountNo)] =
    ZPipeline.mapChunksZIO((inputs: Chunk[Execution]) =>
      ZIO.foreach(inputs):
        case exe =>
          orderRepository
            .query(exe.orderNo)
            .someOrFail(new Throwable(s"Order not found for order no ${exe.orderNo}"))
            .map(order => (exe, order.accountNo))
    )

  override def allocateTradeToClientAccount(userId: UserId): ZPipeline[Any, Throwable, (Execution, AccountNo), Trade] =
    ZPipeline.mapChunksZIO((inputs: Chunk[(Execution, AccountNo)]) =>
      ZIO.foreach(inputs):
        case (exe, accountNo) =>
          Trade
            .trade(
              accountNo,
              exe.isin,
              exe.market,
              exe.buySell,
              exe.unitPrice,
              exe.quantity,
              exe.dateOfExecution,
              valueDate = None,
              userId = Some(userId)
            )
            .map(Trade.withTaxFee)
    )

  override def storeTrades: ZPipeline[Any, Throwable, Trade, Trade] =
    ZPipeline.mapChunksZIO((trades: Chunk[Trade]) => tradeRepository.store(trades).as(trades))

object TradingServiceLive:
  val layer =
    ZLayer.scoped(for
      orderRepository <- ZIO.service[OrderRepository]
      tradeRepository <- ZIO.service[TradeRepository]
      appResources    <- ZIO.service[AppResources]
      session         <- appResources.postgres.toScopedZIO
    yield TradingServiceLive(session, orderRepository, tradeRepository))
