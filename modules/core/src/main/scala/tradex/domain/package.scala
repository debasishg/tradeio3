package tradex

import zio.prelude._
import zio.prelude.Assertion.*
import cats.syntax.all.*
import squants.market.*
import zio.config.magnolia.DeriveConfig
import zio.Config.Secret
import zio.json.*
import java.util.UUID

package object domain {
  given MoneyContext = defaultMoneyContext

  object NonEmptyString extends Subtype[String] {
    override inline def assertion: Assertion[String] = !isEmptyString
    given DeriveConfig[NonEmptyString] =
      DeriveConfig[String].map(NonEmptyString.make(_).fold(_ => NonEmptyString("empty string"), identity))
  }
  type NonEmptyString = NonEmptyString.Type
  given JsonDecoder[NonEmptyString] = JsonDecoder[String].mapOrFail(NonEmptyString.make(_).toEither.leftMap(_.head))
  given JsonEncoder[NonEmptyString] = JsonEncoder[String].contramap(NonEmptyString.unwrap(_))

  given DeriveConfig[Secret] =
    DeriveConfig[String].map(Secret(_))

  given JsonDecoder[Currency] =
    JsonDecoder[String].map(Currency.apply(_).get)
  given JsonEncoder[Currency] =
    JsonEncoder[String].contramap(_.toString)

  given JsonDecoder[Money] =
    JsonDecoder[BigDecimal].map(USD.apply)
  given JsonEncoder[Money] =
    JsonEncoder[BigDecimal].contramap(_.amount)

  given nelDecoder[A: JsonDecoder]: JsonDecoder[NonEmptyList[A]] =
    JsonDecoder[List[A]].map(l => NonEmptyList.apply(l.head, l.tail: _*))
  given nelEncoder[A: JsonEncoder]: JsonEncoder[NonEmptyList[A]] =
    JsonEncoder[List[A]].contramap(_.toList)

  given JsonDecoder[UUID] =
    JsonDecoder[String].map(UUID.fromString(_))
  given JsonEncoder[UUID] =
    JsonEncoder[String].contramap(_.toString())
}
