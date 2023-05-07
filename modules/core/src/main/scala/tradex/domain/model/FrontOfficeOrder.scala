package tradex.domain
package model

import java.time.Instant
import zio.prelude.NonEmptyList
import account.AccountNo
import order.{ BuySell, Order, Quantity }
import instrument.{ ISINCode, UnitPrice }
import zio.stream.ZStream

object frontOfficeOrder:
  final case class FrontOfficeOrder private[domain] (
      accountNo: AccountNo,
      date: Instant,
      isin: ISINCode,
      qty: Quantity,
      unitPrice: UnitPrice,
      buySell: BuySell
  )

  object FrontOfficeOrder:
    def toOrders(foOrders: NonEmptyList[FrontOfficeOrder]): NonEmptyList[Order] = ???
