package tradex.domain
package repository

import zio.UIO
import model.account.*
import model.balance.*
import java.time.LocalDate

trait BalanceRepository:

  /** query by account number */
  def query(no: AccountNo): UIO[Option[Balance]]

  /** store */
  def store(b: Balance): UIO[Balance]

  /** query all balances that have amount as of this date */
  /** asOf date <= this date */
  def query(date: LocalDate): UIO[List[Balance]]

  /** all balances */
  def all: UIO[List[Balance]]
