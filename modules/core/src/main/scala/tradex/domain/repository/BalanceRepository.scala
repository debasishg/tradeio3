package tradex.domain
package repository

import zio.Task
import model.account.*
import model.balance.*
import java.time.LocalDate

trait BalanceRepository:

  /** query by account number */
  def query(no: AccountNo): Task[Option[Balance]]

  /** store */
  def store(b: Balance): Task[Balance]

  /** store many balances */
  // def store(balances: NonEmptyList[Balance]): M[Unit]

  /** query all balances that have amount as of this date */
  /** asOf date <= this date */
  def query(date: LocalDate): Task[List[Balance]]

  /** all balances */
  def all: Task[List[Balance]]
