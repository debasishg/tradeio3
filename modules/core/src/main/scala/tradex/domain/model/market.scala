package tradex.domain
package model

import io.circe.{ Decoder, Encoder }

object market:

  enum Market(val entryName: NonEmptyString):
    case NewYork extends Market(NonEmptyString("New York"))
    case Tokyo extends Market(NonEmptyString("Tokyo"))
    case Singapore extends Market(NonEmptyString("Singapore"))
    case HongKong extends Market(NonEmptyString("Hong Kong"))
    case Other extends Market(NonEmptyString("Other"))

  object Market:
    implicit val marketEncoder: Encoder[Market] =
      Encoder[String].contramap(_.entryName)

    implicit val marketDecoder: Decoder[Market] =
      Decoder[String].map(Market.valueOf(_))
