package tradex.domain
package model

import zio.prelude.*

object market:

  /*
  enum Market(val entryName: NonEmptyString):
    case NewYork extends Market(NonEmptyString("New York"))
    case Tokyo extends Market(NonEmptyString("Tokyo"))
    case Singapore extends Market(NonEmptyString("Singapore"))
    case HongKong extends Market(NonEmptyString("Hong Kong"))
    case Other extends Market(NonEmptyString("Other"))
   */

  enum Market(val entryName: String):
    case NewYork extends Market("New York")
    case Tokyo extends Market("Tokyo")
    case Singapore extends Market("Singapore")
    case HongKong extends Market("Hong Kong")
    case Other extends Market("Other")

  object Market:

    def withValue(value: String): Validation[String, Market] =
      value match
        case "New York"  => Validation.succeed(NewYork)
        case "Tokyo"     => Validation.succeed(Tokyo)
        case "Singapore" => Validation.succeed(Singapore)
        case "HongKong"  => Validation.succeed(HongKong)
        case "Other"     => Validation.succeed(Other)
        case _           => Validation.fail("Error in value")

  end Market
