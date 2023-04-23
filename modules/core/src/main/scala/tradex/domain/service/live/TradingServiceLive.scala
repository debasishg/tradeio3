package tradex.domain
package service
package live

import java.time.LocalDate
import zio.Task
import model.account.*
import model.trade.*
import model.execution.*
import model.order.*
import repository.ExecutionRepository
import repository.OrderRepository
import repository.live.ExecutionRepositorySQL
import skunk.Session
import zio.stream.ZStream
import zio.interop.catz.*
import zio.stream.interop.fs2z.*
import zio.ZIO
import zio.Chunk
import zio.stream.ZPipeline

final case class TradingServiceLive(session: Session[Task], orderRepository: OrderRepository) extends TradingService:

  override def generateTrades(date: LocalDate): Task[Chunk[Trade]] =
    session.prepare(ExecutionRepositorySQL.selectByExecutionDate).flatMap { pq =>
      pq.stream(date, 512)
        .toZStream()
        .groupByKey(_.orderNo) { case (orderNo, s) => // group executions by orderNo
          ZStream.fromZIO(s.runCollect.flatMap(exes => generateTradesFromExecutions(orderNo, exes)))
        }
        .runCollect
        .map(_.flatten)
    }

  private def generateTradesFromExecutions(
      orderNo: OrderNo,
      executions: Chunk[Execution]
  ): Task[Chunk[Trade]] =
    orderRepository
      .query(orderNo)
      .someOrFail(new Throwable("Order Not found"))
      .flatMap(order => doGenerateTrades(order.accountNo, executions).map(_._2))

  private def doGenerateTrades(accountNo: AccountNo, executions: Chunk[Execution]): Task[(AccountNo, Chunk[Trade])] =
    ???

  val executionsWithAccountNo: ZPipeline[Any, Throwable, (Execution, OrderNo), (Execution, AccountNo)] =
    ZPipeline.mapChunksZIO((inputs: Chunk[(Execution, OrderNo)]) =>
      ZIO.foreach(inputs) { case (exe, orderNo) =>
        orderRepository
          .query(orderNo)
          .someOrFail(new Throwable("Order Not found"))
          .map(order => (exe, order.accountNo))
      }
    )

  val trades: ZPipeline[Any, Throwable, (Execution, AccountNo), Trade] = ???
