package tradex

import zio.prelude._
import zio.prelude.Assertion.*
import io.circe.{ Decoder, Encoder }
import cats.syntax.all.*
import squants.market.*
import zio.config.magnolia.DeriveConfig
import zio.Config.Secret
import zio.json.*

package object domain {
  given MoneyContext = defaultMoneyContext

  object NonEmptyString extends Subtype[String] {
    override inline def assertion: Assertion[String] = !isEmptyString
    given DeriveConfig[NonEmptyString] =
      DeriveConfig[String].map(NonEmptyString.make(_).fold(_ => NonEmptyString("empty string"), identity))
  }
  type NonEmptyString = NonEmptyString.Type
  given Decoder[NonEmptyString] = Decoder[String].emap(NonEmptyString.make(_).toEither.leftMap(_.head))
  given Encoder[NonEmptyString] = Encoder[String].contramap(NonEmptyString.unwrap(_))
  given DeriveConfig[Secret] =
    DeriveConfig[String].map(Secret(_))
  given JsonDecoder[Currency] =
    JsonDecoder[String].map(Currency.apply(_).get)
  given JsonEncoder[Currency] =
    JsonEncoder[String].contramap(_.toString)
}
