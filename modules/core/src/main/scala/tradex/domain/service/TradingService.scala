package tradex.domain
package service

import java.time.LocalDate
import model.trade.*
import model.user.*
import zio.stream.ZStream

trait TradingService:
  def generateTrades(
      date: LocalDate,
      userId: UserId
  ): ZStream[Any, Throwable, Trade]
