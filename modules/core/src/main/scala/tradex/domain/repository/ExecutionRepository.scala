package tradex.domain
package repository

import zio.Task
import model.execution.*
import zio.prelude.NonEmptyList
import java.time.LocalDate
import zio.stream.ZStream

trait ExecutionRepository:

  /** store */
  def store(exe: Execution): Task[Execution]

  /** store many executions */
  def store(executions: NonEmptyList[Execution]): Task[Unit]

  /** query all executions for the day */
  def query(dateOfExecution: LocalDate): Task[List[Execution]]

  /** delete all executions */
  def cleanAllExecutions: Task[Unit]

  /** stream all executions for the day for all orders group by orderNo */
  def streamExecutions(
      executionDate: LocalDate
  ): ZStream[Any, Throwable, Execution] = ???
