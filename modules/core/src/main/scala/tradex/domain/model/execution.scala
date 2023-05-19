package tradex.domain
package model

import zio.prelude.*
import zio.{ Task, ZIO }
import instrument.*
import order.*
import market.*
import account.*
import java.time.LocalDateTime
import java.util.UUID
import zio.Clock
import java.time.ZoneOffset
import zio.Random

object execution:

  object ExecutionRefNo extends Newtype[UUID]:
    implicit val ExecutionRefNoEqual: Equal[ExecutionRefNo] =
      Equal.default

  type ExecutionRefNo = ExecutionRefNo.Type

  final case class Execution private[domain] (
      executionRefNo: ExecutionRefNo,
      accountNo: AccountNo,
      orderNo: OrderNo,
      isin: ISINCode,
      market: Market,
      buySell: BuySell,
      unitPrice: UnitPrice,
      quantity: Quantity,
      dateOfExecution: LocalDateTime,
      exchangeExecutionRefNo: Option[String] = None
  )

  object Execution:
    def fromOrder(order: Order, market: Market): Task[NonEmptyList[Execution]] =
      Clock.instant.flatMap { now =>
        Random.nextUUID.flatMap { uuid =>
          val executions = order.items.map { item =>
            Execution(
              ExecutionRefNo(uuid),
              order.accountNo,
              order.no,
              item.isin,
              market,
              item.buySell,
              item.unitPrice,
              item.quantity,
              LocalDateTime.ofInstant(now, ZoneOffset.UTC)
            )
          }
          ZIO.succeed(executions)
        }
      }
