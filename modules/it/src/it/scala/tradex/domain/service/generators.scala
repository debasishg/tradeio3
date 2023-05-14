package tradex.domain
package service

import zio.prelude._
import zio.Random
import zio.test._
import zio.test.Gen.fromZIOSample

import model.account.*
import model.instrument.*
import model.order.*
import model.frontOfficeOrder.*
import java.time.Instant

object generators:
  val posIntGen =
    fromZIOSample(Random.nextIntBetween(0, Int.MaxValue).map(Sample.shrinkIntegral(0)))

  val nonEmptyStringGen: Gen[Random with Sized, String]  = Gen.alphaNumericStringBounded(21, 40)
  val accountNoStringGen: Gen[Random with Sized, String] = Gen.alphaNumericStringBounded(5, 12)
  val accountNoGen: Gen[Random with Sized, AccountNo] =
    val accs = List("ibm-123", "ibm-124", "nri-654").map(str =>
      AccountNo(str).validateNo
        .fold(errs => throw new Exception(errs.toString), identity)
    )
    Gen.fromIterable(accs)

  def isinGen: Gen[Any, ISINCode] =
    val appleISINStr = "US0378331005"
    val baeISINStr   = "GB0002634946"
    val ibmISINStr   = "US4592001014"

    val isins = List(appleISINStr, baeISINStr, ibmISINStr)
      .map(str =>
        ISINCode
          .make(str)
          .toEitherAssociative
          .leftMap(identity)
          .fold(err => throw new Exception(err), identity)
      )
    Gen.fromIterable(isins)

  val unitPriceGen: Gen[Any, UnitPrice] =
    val ups = List(BigDecimal(12.25), BigDecimal(51.25), BigDecimal(55.25))
      .map(n =>
        UnitPrice
          .make(n)
          .toEitherAssociative
          .leftMap(identity)
          .fold(err => throw new Exception(err), identity)
      )
    Gen.fromIterable(ups)

  val quantityGen: Gen[Any, Quantity] =
    val qtys = List(BigDecimal(100), BigDecimal(200), BigDecimal(300))
      .map(n =>
        Quantity
          .make(n)
          .toEitherAssociative
          .leftMap(identity)
          .fold(err => throw new Exception(err), identity)
      )
    Gen.fromIterable(qtys)

  def frontOfficeOrderGen(orderDate: Instant) = for
    ano <- accountNoGen
    // dt   <- Gen.fromIterable(List(Instant.now, Instant.now.plus(2, java.time.temporal.ChronoUnit.DAYS)))
    isin <- isinGen
    qty  <- quantityGen
    up   <- unitPriceGen
    bs   <- Gen.fromIterable(BuySell.values)
  yield FrontOfficeOrder(ano, orderDate, isin, qty, up, bs)
