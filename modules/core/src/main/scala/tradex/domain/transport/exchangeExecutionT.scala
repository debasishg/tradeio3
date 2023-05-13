package tradex.domain
package transport

import kantan.csv.RowDecoder
import kantan.csv.java8.*
import model.exchangeExecution.*
import cellCodecs.{ given, * }

object exchangeExecutionT:
  given RowDecoder[ExchangeExecution] = RowDecoder.decoder(0, 1, 2, 3, 4, 5, 6, 7, 8)(ExchangeExecution.apply)
