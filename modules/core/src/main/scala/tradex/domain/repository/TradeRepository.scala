package tradex.domain
package repository

import zio.{ Chunk, Task, UIO }
import model.account.*
import model.trade.*
import model.market.*
import java.time.LocalDate
import zio.prelude.NonEmptyList

trait TradeRepository:

  /** query by account number and trade date (compares using the date part only) */
  def query(accountNo: AccountNo, date: LocalDate): UIO[List[Trade]]

  /** query by market */
  def queryByMarket(market: Market): UIO[List[Trade]]

  /** query all trades */
  def all: UIO[List[Trade]]

  /** store */
  def store(trd: Trade): UIO[Trade]

  /** store many trades */
  def store(trades: Chunk[Trade]): UIO[Unit]
