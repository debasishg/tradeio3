package tradex.domain
package model

import java.time.Instant
import zio.prelude._

import instrument._
import account._
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*
import cats.implicits.catsSyntaxEither

object order {
  object OrderNo extends Newtype[String]:
    implicit val OrderNoEqual: Equal[OrderNo] =
      Equal.default

  type OrderNo = OrderNo.Type

  extension (ono: OrderNo)
    def validateNo: Validation[String, OrderNo] =
      if (OrderNo.unwrap(ono).size > 12 || OrderNo.unwrap(ono).size < 5)
        Validation.fail(s"OrderNo cannot be more than 12 characters or less than 5 characters long")
      else Validation.succeed(ono)

  object Quantity extends Subtype[BigDecimal]:
    override inline def assertion = Assertion.greaterThan(BigDecimal(0))
    given Decoder[Quantity]       = Decoder[BigDecimal].emap(Quantity.make(_).toEither.leftMap(_.head))
    given Encoder[Quantity]       = Encoder[BigDecimal].contramap(Quantity.unwrap(_))

  type Quantity = Quantity.Type

  enum BuySell(val entryName: NonEmptyString):
    case Buy extends BuySell(NonEmptyString("buy"))
    case Sell extends BuySell(NonEmptyString("sell"))

  object BuySell:
    given Encoder[BuySell] =
      Encoder[String].contramap(_.entryName)

    given Decoder[BuySell] =
      Decoder[String].map(BuySell.valueOf(_))

  private[domain] final case class LineItem private (
      orderNo: OrderNo,
      isin: ISINCode,
      quantity: Quantity,
      unitPrice: UnitPrice,
      buySell: BuySell
  )

  final case class Order private (
      no: OrderNo,
      date: Instant,
      accountNo: AccountNo,
      items: NonEmptyList[LineItem]
  )

  object Order:
    def make(
        no: OrderNo,
        orderDate: Instant,
        accountNo: AccountNo,
        items: NonEmptyList[LineItem]
    ): Order = Order(no, orderDate, accountNo, items)
}
