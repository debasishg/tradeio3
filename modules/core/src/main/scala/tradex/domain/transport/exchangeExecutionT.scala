package tradex.domain
package transport

import kantan.csv.RowDecoder
import kantan.csv.java8.*
import model.exchangeExecution.*
import cellCodecs.{ *, given }
import kantan.csv.HeaderCodec
import model.account.AccountNo
import model.order.OrderNo
import model.instrument.ISINCode
import model.market.Market
import model.order.BuySell
import model.instrument.UnitPrice
import model.order.Quantity
import java.time.LocalDateTime

object exchangeExecutionT:
  given RowDecoder[ExchangeExecution] = RowDecoder.decoder(0, 1, 2, 3, 4, 5, 6, 7, 8)(ExchangeExecution.apply)
  given HeaderCodec[ExchangeExecution] =
    HeaderCodec.codec[
      String,
      AccountNo,
      OrderNo,
      ISINCode,
      Market,
      BuySell,
      UnitPrice,
      Quantity,
      LocalDateTime,
      ExchangeExecution
    ](
      "Exchange Execution Ref No",
      "Account No",
      "Order No",
      "ISIN Code",
      "Market",
      "Buy/Sell",
      "Unit Price",
      "Quantity",
      "Date of Execution"
    )(ExchangeExecution(_, _, _, _, _, _, _, _, _))(ee =>
      (
        ee.exchangeExecutionRefNo,
        ee.accountNo,
        ee.orderNo,
        ee.isin,
        ee.market,
        ee.buySell,
        ee.unitPrice,
        ee.quantity,
        ee.dateOfExecution
      )
    )
