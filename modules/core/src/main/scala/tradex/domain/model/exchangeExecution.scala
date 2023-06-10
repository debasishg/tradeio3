package tradex.domain
package model

import zio.prelude.*
import zio.{ Clock, Random, Task, ZIO }
import instrument.*
import order.*
import market.*
import account.*
import java.time.LocalDateTime
import java.util.UUID
import java.time.ZoneOffset
import java.time.Instant

object exchangeExecution:

  final case class ExchangeExecution private[domain] (
      exchangeExecutionRefNo: String,
      accountNo: AccountNo,
      orderNo: OrderNo,
      isin: ISINCode,
      market: Market,
      buySell: BuySell,
      unitPrice: UnitPrice,
      quantity: Quantity,
      dateOfExecution: LocalDateTime
  )

  object ExchangeExecution:
    def fromOrder(order: Order, market: Market, date: Instant): Task[NonEmptyList[ExchangeExecution]] =
      Random.nextUUID.flatMap: uuid =>
        val executions = order.items.map { item =>
          ExchangeExecution(
            uuid.toString,
            order.accountNo,
            order.no,
            item.isin,
            market,
            item.buySell,
            item.unitPrice,
            item.quantity,
            LocalDateTime.ofInstant(date, ZoneOffset.UTC)
          )
        }
        ZIO.succeed(executions)
