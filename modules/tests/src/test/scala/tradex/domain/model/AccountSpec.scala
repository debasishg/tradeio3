package tradex.domain
package model

import zio.test._
import zio.test.Assertion._
import squants.market.*
import account._

object AccountSpec extends ZIOSpecDefault {
  val spec = suite("Account")(
    test("successfully creates an account") {
      val ta = TradingAccount
        .tradingAccount(
          no = AccountNo(NonEmptyString("a-123456")),
          name = AccountName("debasish ghosh"),
          baseCurrency = USD,
          tradingCcy = USD,
          dateOfOpen = None,
          dateOfClose = None
        )
        .fold(errs => throw new Exception(errs.mkString), identity)
      assertTrue(ta.tradingCurrency == USD)
    }
  )
}
