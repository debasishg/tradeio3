package tradex

import zio.prelude._
import zio.prelude.Assertion._
import io.circe.{ Decoder, Encoder }
import cats.syntax.all._
import squants.market._
import zio.config.magnolia.DeriveConfig
import zio.Config.Secret

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
}
