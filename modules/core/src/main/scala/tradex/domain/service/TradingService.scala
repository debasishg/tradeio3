package tradex.domain
package service

import zio.{ Task, UIO }
import zio.stream.{ ZPipeline, ZStream }
import java.time.LocalDate
import model.trade.*
import model.user.*
import model.account.AccountNo
import tradex.domain.model.execution.Execution

trait TradingService:
  /** Generate trades for the day and by a specific user. Here are the steps:
    *
    *   1. Query all executions for the day 2. Group executions by order no 3. For each order no, get the account no
    *      from the order details 4. Allocate the trade to the client account 5. Store the trade
    *
    * @param date
    *   the date the trade is made
    * @param userId
    *   the user who created the trade
    *
    * @return
    *   a stream of trades
    */
  def generateTrades(
      date: LocalDate,
      userId: UserId
  ): ZStream[Any, Throwable, Trade] =
    queryExecutionsForDate(date)
      .groupByKey(_.orderNo):
        case (orderNo, executions) =>
          executions
            .via(getAccountNoFromExecution)
            .via(allocateTradeToClientAccount(userId))
            .via(storeTrades)

  /** Get client account numbers from a stream of executions
    *
    * @return
    *   a stream containing an execution and its associated client account no fetched from the order details
    */
  def getAccountNoFromExecution: ZPipeline[Any, Throwable, Execution, (Execution, AccountNo)]

  /** Generate trades from executions and allocate to the associated client account
    *
    * @param userId
    * @return
    *   the stream of trades generated
    */
  def allocateTradeToClientAccount(userId: UserId): ZPipeline[Any, Throwable, (Execution, AccountNo), Trade]

  /** persist trades to database and return the stored trades
    *
    * @return
    *   a stream of trades stored in the database
    */
  def storeTrades: ZPipeline[Any, Throwable, Trade, Trade]

  def queryTradesForDate(accountNo: AccountNo, date: LocalDate): UIO[List[Trade]]

  /** query all exeutions for the day
    *
    * @param date
    *   the date when executions were done in the exchange
    * @return
    *   the stream of executions
    */
  def queryExecutionsForDate(date: LocalDate): ZStream[Any, Throwable, Execution]
