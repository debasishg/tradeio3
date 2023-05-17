package tradex.domain
package service
package live

import zio.{ Random, Task, UIO, ZIO, ZLayer }
import zio.stream.{ ZPipeline, ZStream }
import zio.prelude.NonEmptyList
import java.io.Reader
import kantan.csv.rfc
import transport.exchangeExecutionT.{ given, * }
import model.exchangeExecution.*
import model.execution.*
import repository.ExecutionRepository
import csv.CSV

final case class ExchangeExecutionParsingServiceLive(
    executionRepo: ExecutionRepository
) extends ExchangeExecutionParsingService:
  def parse(data: Reader): Task[Unit] =
    parseAllRows(data)
      .via(convertToExecution)
      .runForeachChunk(executions =>
        ZIO.when(executions.nonEmpty)(executionRepo.store(NonEmptyList(executions.head, executions.tail.toList: _*)))
      )

  private def parseAllRows(data: Reader): ZStream[Any, Throwable, ExchangeExecution] =
    CSV.decode[ExchangeExecution](data, rfc.withHeader)

  private def convertToExecution: ZPipeline[Any, Nothing, ExchangeExecution, Execution] =
    ZPipeline
      .mapZIO(toExecution(_))

  private def toExecution(exchangeExecution: ExchangeExecution): UIO[Execution] = Random.nextUUID.map { uuid =>
    Execution(
      ExecutionRefNo(uuid),
      exchangeExecution.accountNo,
      exchangeExecution.orderNo,
      exchangeExecution.isin,
      exchangeExecution.market,
      exchangeExecution.buySell,
      exchangeExecution.unitPrice,
      exchangeExecution.quantity,
      exchangeExecution.dateOfExecution,
      Some(exchangeExecution.exchangeExecutionRefNo)
    )
  }

object ExchangeExecutionParsingServiceLive:
  val layer = ZLayer.fromFunction(ExchangeExecutionParsingServiceLive.apply _)
