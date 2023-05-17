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
import tradex.domain.model.exchangeExecution.ExchangeExecution
import tradex.domain.model.market.Market
import java.time.LocalDateTime
import java.time.ZoneOffset

object generators:
  val posIntGen =
    fromZIOSample(Random.nextIntBetween(0, Int.MaxValue).map(Sample.shrinkIntegral(0)))

  val nonEmptyStringGen: Gen[Random with Sized, String]  = Gen.alphaNumericStringBounded(21, 40)
  val accountNoStringGen: Gen[Random with Sized, String] = Gen.alphaNumericStringBounded(5, 12)
  val orderNoStringGen: Gen[Random with Sized, String]   = Gen.alphaNumericStringBounded(5, 50)

  val orderNoGen: Gen[Random with Sized, OrderNo] =
    orderNoStringGen.map(OrderNo(_))

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

  def frontOfficeOrderGen(orderDate: Instant): Gen[Random & Sized, FrontOfficeOrder] = for
    ano  <- accountNoGen
    isin <- isinGen
    qty  <- quantityGen
    up   <- unitPriceGen
    bs   <- Gen.fromIterable(BuySell.values)
  yield FrontOfficeOrder(ano, orderDate, isin, qty, up, bs)

  def exchangeExecutionGen(date: Instant): Gen[Random & Sized, ExchangeExecution] = for
    erefNo <- nonEmptyStringGen
    ano    <- accountNoGen
    ono    <- orderNoGen
    isin   <- isinGen
    market <- Gen.fromIterable(Market.values)
    bs     <- Gen.fromIterable(BuySell.values)
    up     <- unitPriceGen
    qty    <- quantityGen
  yield ExchangeExecution(erefNo, ano, ono, isin, market, bs, up, qty, LocalDateTime.ofInstant(date, ZoneOffset.UTC))

  def lineItemGen(ono: OrderNo): Gen[Any, LineItem] = for
    isin <- isinGen
    qty  <- quantityGen
    up   <- unitPriceGen
    bs   <- Gen.fromIterable(BuySell.values)
  yield LineItem.make(ono, isin, qty, up, bs)

  def orderGen(date: Instant): Gen[Random & Sized, Order] = for
    ono   <- orderNoGen
    ano   <- accountNoGen
    items <- Gen.listOfN(5)(lineItemGen(ono))
  yield Order.make(
    ono,
    LocalDateTime.ofInstant(date, ZoneOffset.UTC),
    ano,
    NonEmptyList.fromIterable(items.head, items.tail)
  )
