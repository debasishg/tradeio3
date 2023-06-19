package tradex.domain
package repository

import zio.UIO
import model.execution.*
import zio.prelude.NonEmptyList
import java.time.LocalDate
import zio.stream.ZStream

trait ExecutionRepository:

  /** store */
  def store(exe: Execution): UIO[Execution]

  /** store many executions */
  def store(executions: NonEmptyList[Execution]): UIO[Unit]

  /** query all executions for the day */
  def query(dateOfExecution: LocalDate): UIO[List[Execution]]

  /** delete all executions */
  def cleanAllExecutions: UIO[Unit]

  /** stream all executions for the day for all orders group by orderNo */
  def streamExecutions(
      executionDate: LocalDate
  ): ZStream[Any, Throwable, Execution] = ???
