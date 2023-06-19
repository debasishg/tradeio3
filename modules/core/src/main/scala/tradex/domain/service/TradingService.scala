package tradex.domain
package service

import java.time.LocalDate
import model.trade.*
import model.user.*
import model.account.AccountNo
import zio.{ Task, UIO }
import zio.stream.ZStream

trait TradingService:
  def generateTrades(
      date: LocalDate,
      userId: UserId
  ): ZStream[Any, Throwable, Trade]

  def queryTradesForDate(accountNo: AccountNo, date: LocalDate): UIO[List[Trade]]
