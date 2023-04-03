package tradex.domain

import zio.prelude.NonEmptyList
import io.circe.{ Decoder, Encoder }
import squants.market.{ Currency, Money, USD }
import java.util.UUID

package object model extends OrphanInstances

// instances for types we don't control
trait OrphanInstances:
  given Decoder[Money] =
    Decoder[BigDecimal].map(USD.apply)

  given Decoder[Currency] =
    Decoder[String].map(Currency.apply(_).get)

  given Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  given Encoder[Currency] =
    Encoder[String].contramap(_.toString)

  given nelDecoder[A: Decoder]: Decoder[NonEmptyList[A]] =
    Decoder[List[A]].map(l => NonEmptyList.apply(l.head, l.tail: _*))

  given Decoder[UUID] =
    Decoder[String].map(UUID.fromString(_))

  given Encoder[UUID] =
    Encoder[String].contramap(_.toString())
