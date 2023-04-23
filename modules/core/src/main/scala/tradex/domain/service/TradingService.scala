package tradex.domain
package service

import zio.Task
import java.time.LocalDate
import model.trade.*
import zio.stream.ZStream
import zio.Chunk

trait TradingService:
  // generate trades for a given date
  // Steps:
  // 1. stream orders for the date from repository
  // 2. stream executions for the date corresponding to the orders from repository
  // 3. allocate executions to respective client accounts from order and generate `Trade`
  def generateTrades(
      date: LocalDate
  ): Task[Chunk[Trade]]
