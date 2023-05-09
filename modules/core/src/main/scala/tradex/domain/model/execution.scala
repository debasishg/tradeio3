package tradex.domain
package model

import zio.prelude.*
import instrument.*
import order.*
import market.*
import account.*
import java.time.LocalDateTime
import java.util.UUID

object execution {

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
}
