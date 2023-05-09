package tradex.domain
package model

import zio.prelude.*
import instrument.*
import order.*
import market.*
import account.*
import java.time.LocalDateTime
import java.util.UUID

object exchangeExecution {

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
}
