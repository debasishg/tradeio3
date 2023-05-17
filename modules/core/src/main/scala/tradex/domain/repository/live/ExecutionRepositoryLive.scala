package tradex.domain
package repository
package live

import zio.Task
import cats.effect.kernel.Resource
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import model.execution.*
import zio.interop.catz.*
import codecs.{ given, * }
import zio.prelude.NonEmptyList
import zio.ZLayer
import java.time.LocalDate

final case class ExecutionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends ExecutionRepository:
  import ExecutionRepositorySQL.*

  override def store(execution: Execution): Task[Execution] =
    postgres.use(session =>
      session
        .prepare(insertExecution)
        .flatMap(_.execute(execution))
        .map(_ => execution)
    )

  override def store(executions: NonEmptyList[Execution]): Task[Unit] =
    postgres.use(session =>
      session
        .prepare(insertExecutions(executions.size))
        .flatMap(_.execute(executions.toList))
        .unit
    )

  override def query(dateOfExecution: LocalDate): Task[List[Execution]] =
    postgres.use(session =>
      session
        .prepare(selectByExecutionDate)
        .flatMap(_.stream(dateOfExecution, 1024).compile.toList)
    )

  override def cleanAllExecutions: Task[Unit] =
    postgres.use(session =>
      session
        .execute(deleteAllExecutions)
        .unit
    )

private[domain] object ExecutionRepositorySQL:
  val executionEncoder: Encoder[Execution] =
    (executionRefNo ~ accountNo ~ orderNo ~ isinCode ~ market ~ buySell ~ unitPrice ~ quantity ~ timestamp ~ varchar.opt).values
      .gcontramap[Execution]

  val executionDecoder: Decoder[Execution] =
    (executionRefNo ~ accountNo ~ orderNo ~ isinCode ~ market ~ buySell ~ unitPrice ~ quantity ~ timestamp ~ varchar.opt)
      .gmap[Execution]

  val insertExecution: Command[Execution] =
    sql"""
      INSERT INTO executions 
      (
        executionRefNo,
        accountNo,
        orderNo,
        isinCode,
        market,
        buySellFlag,
        unitPrice,
        quantity,
        dateOfExecution,
        exchangeExecutionRefNo
      )
      VALUES $executionEncoder
    """.command

  def insertExecutions(ps: List[Execution]): Command[ps.type] = {
    val enc = executionEncoder.values.list(ps)
    sql"INSERT INTO executions VALUES $enc".command
  }

  def insertExecutions(n: Int): Command[List[Execution]] = {
    val enc = executionEncoder.list(n)
    sql"INSERT INTO executions VALUES $enc".command
  }

  val selectByExecutionDate: Query[LocalDate, Execution] =
    sql"""
        SELECT e.executionRefNo, e.accountNo, e.orderNo, e.isinCode, e.market, e.buySellFlag, e.unitPrice, e.quantity, e.dateOfExecution, e.exchangeExecutionRefNo
        FROM executions AS e
        WHERE DATE(e.dateOfExecution) = $date
       """.query(executionDecoder)

  val deleteAllExecutions: Command[Void] =
    sql"DELETE FROM executions".command

object ExecutionRepositoryLive:
  val layer = ZLayer.fromFunction(ExecutionRepositoryLive.apply _)
