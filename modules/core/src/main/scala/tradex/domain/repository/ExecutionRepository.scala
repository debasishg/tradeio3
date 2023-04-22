package tradex.domain
package repository

import zio.Task
import model.execution.*
import zio.prelude.NonEmptyList

trait ExecutionRepository:

  /** store */
  def store(exe: Execution): Task[Execution]

  /** store many executions */
  def store(executions: NonEmptyList[Execution]): Task[Unit]
