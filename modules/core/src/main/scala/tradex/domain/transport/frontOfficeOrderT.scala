package tradex.domain
package transport

import kantan.csv.RowDecoder
import kantan.csv.java8.*
import model.frontOfficeOrder.FrontOfficeOrder
import cellDecoders.{ given, * }

object frontOfficeOrderT:
  given RowDecoder[FrontOfficeOrder] = RowDecoder.decoder(0, 1, 2, 3, 4, 5)(FrontOfficeOrder.apply)
