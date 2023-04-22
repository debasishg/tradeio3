package tradex.domain
package repository

import zio.Task
import model.account.*
import model.trade.*
import model.market.*
import java.time.LocalDate
import zio.prelude.NonEmptyList

trait TradeRepository:

  /** query by account number and trade date (compares using the date part only) */
  def query(accountNo: AccountNo, date: LocalDate): Task[List[Trade]]

  /** query by market */
  def queryByMarket(market: Market): Task[List[Trade]]

  /** query all trades */
  def all: Task[List[Trade]]

  /** store */
  def store(trd: Trade): Task[Trade]

  /** store many trades */
  def store(trades: NonEmptyList[Trade]): Task[Unit]
