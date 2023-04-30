package tradex.domain
package model

object market:

  enum Market(val entryName: NonEmptyString):
    case NewYork extends Market(NonEmptyString("New York"))
    case Tokyo extends Market(NonEmptyString("Tokyo"))
    case Singapore extends Market(NonEmptyString("Singapore"))
    case HongKong extends Market(NonEmptyString("Hong Kong"))
    case Other extends Market(NonEmptyString("Other"))
