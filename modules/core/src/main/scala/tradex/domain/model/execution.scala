package tradex.domain
package model

import zio.prelude.*
import instrument.*
import order.*
import market.*
import account.*
import java.time.Instant

object execution {

  object ExecutionRefNo extends Newtype[String]:
    implicit val ExecutionRefNoEqual: Equal[ExecutionRefNo] =
      Equal.default

  type ExecutionRefNo = ExecutionRefNo.Type

  extension (eno: ExecutionRefNo)
    def validateNo: Validation[String, ExecutionRefNo] =
      if (ExecutionRefNo.unwrap(eno).size > 12 || ExecutionRefNo.unwrap(eno).size < 5)
        Validation.fail(s"ExecutionRefNo cannot be more than 12 characters or less than 5 characters long")
      else Validation.succeed(eno)

  final case class Execution private (
      accountNo: AccountNo,
      orderNo: OrderNo,
      isin: ISINCode,
      market: Market,
      buySell: BuySell,
      unitPrice: UnitPrice,
      quantity: Quantity,
      dateOfExecution: Instant,
      exchangeExecutionRefNo: Option[String] = None,
      executionRefNo: Option[ExecutionRefNo] = None
  )
}
