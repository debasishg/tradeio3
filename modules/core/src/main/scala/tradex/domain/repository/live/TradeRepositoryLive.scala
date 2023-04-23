package tradex.domain
package repository
package live

import zio.{ Task, ZIO }
import cats.effect.kernel.Resource
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import model.market.*
import model.trade.*
import model.account.*
import java.time.LocalDate
import zio.prelude.NonEmptyList
import codecs.{ given, * }
import zio.interop.catz.*
import zio.prelude.Associative
import zio.ZLayer

final case class TradeRepositoryLive(postgres: Resource[Task, Session[Task]]) extends TradeRepository:
  import TradeRepositorySQL.*

  // semigroup that combines trades with same reference number
  // used in combining join records between trades and taxFees tables
  // NOT a generic semigroup that combines all trades - only specific
  // to this query - hence not added in the companion object
  implicit val tradeConcatSemigroup: Associative[Trade] =
    new Associative[Trade] {
      def combine(x: => Trade, y: => Trade): Trade =
        x.copy(taxFees = x.taxFees ++ y.taxFees)
    }

  override def store(trades: NonEmptyList[Trade]): Task[Unit] =
    postgres.use { session =>
      ZIO
        .foreach(trades.toList)(trade => storeTradeAndTaxFees(trade, session))
        .unit
    }

  override def all: Task[List[Trade]] =
    postgres.use { session =>
      session.prepare(selectAll).flatMap { ps =>
        ps.stream(skunk.Void, 1024)
          .compile
          .toList
          .map(_.groupBy(_.tradeRefNo))
          .map {
            _.map { case (_, trades) =>
              trades.reduce(Associative[Trade].combine(_, _))
            }.toList
          }
      }
    }

  override def store(trd: Trade): Task[Trade] =
    postgres.use { session =>
      storeTradeAndTaxFees(trd, session)
    }

  private def storeTradeAndTaxFees(
      t: Trade,
      session: Session[Task]
  ): Task[Trade] = {
    val r = for {
      p1 <- session.prepare(insertTrade)
      p2 <- session.prepare(insertTaxFees(t.tradeRefNo, t.taxFees))
    } yield (p1, p2)
    r.flatMap { case (p1, p2) =>
      session.transaction.use { _ =>
        for {
          _ <- p1.execute(t)
          _ <- p2.execute(t.taxFees)
        } yield ()
      }
    }.map(_ => t)
  }

  override def query(accountNo: AccountNo, date: LocalDate): Task[List[Trade]] =
    postgres.use { session =>
      session.prepare(selectByAccountNoAndDate).flatMap { ps =>
        ps.stream(accountNo ~ date, 1024)
          .compile
          .toList
          .map(_.groupBy(_.tradeRefNo))
          .map {
            _.map { case (_, trades) =>
              trades.reduce(Associative[Trade].combine(_, _))
            }.toList
          }
      }
    }

  override def queryByMarket(market: Market): Task[List[Trade]] =
    postgres.use { session =>
      session.prepare(selectByMarket).flatMap { ps =>
        ps.stream(market, 1024)
          .compile
          .toList
          .map(_.groupBy(_.tradeRefNo))
          .map {
            _.map { case (_, trades) =>
              trades.reduce(Associative[Trade].combine(_, _))
            }.toList
          }
      }
    }

private[domain] object TradeRepositorySQL:
  val tradeTaxFeeDecoder: Decoder[Trade] =
    (tradeRefNo ~ accountNo ~ isinCode ~ market ~ buySell ~ unitPrice ~ quantity ~ timestamp ~ timestamp.opt ~ userId.opt ~ money.opt ~ taxFeeId ~ money)
      .map { case ref ~ ano ~ isin ~ mkt ~ bs ~ up ~ qty ~ td ~ vdOpt ~ uidOpt ~ naOpt ~ tx ~ amt =>
        (
          Trade(
            ref,
            ano,
            isin,
            mkt,
            bs,
            up,
            qty,
            td,
            vdOpt,
            uidOpt,
            List(TradeTaxFee(tx, amt)),
            naOpt
          )
        )
      }

  val tradeEncoder: Encoder[Trade] =
    (tradeRefNo ~ accountNo ~ isinCode ~ market ~ buySell ~ unitPrice ~ quantity ~ timestamp ~ timestamp.opt ~ userId.opt ~ money.opt).values
      .contramap((t: Trade) =>
        t.tradeRefNo ~ t.accountNo ~ t.isin ~ t.market ~ t.buySell ~ t.unitPrice ~ t.quantity ~ t.tradeDate ~ t.valueDate ~ t.userId ~ t.netAmount
      )

  def taxFeeEncoder(refNo: TradeRefNo): Encoder[TradeTaxFee] =
    (tradeRefNo ~ taxFeeId ~ money).values
      .contramap((t: TradeTaxFee) => refNo ~ t.taxFeeId ~ t.amount)

  val insertTrade: Command[Trade] =
    sql"""
      INSERT INTO trades 
      (
        tradeRefNo,
        accountNo,
        isinCode,
        market,
        buySellFlag,
        unitPrice,
        quantity,
        tradeDate,
        valueDate,
        userId,
        netAmount
      )
      VALUES $tradeEncoder
    """.command

  def insertTaxFee(tradeRefNo: TradeRefNo): Command[TradeTaxFee] =
    sql"INSERT INTO tradeTaxFees (tradeRefNo, taxFeeId, amount) VALUES ${taxFeeEncoder(tradeRefNo)}".command

  def insertTaxFees(
      tradeRefNo: TradeRefNo,
      taxFees: List[TradeTaxFee]
  ): Command[taxFees.type] = {
    val es = taxFeeEncoder(tradeRefNo).list(taxFees)
    sql"INSERT INTO tradeTaxFees (tradeRefNo, taxFeeId, amount) VALUES $es".command
  }

  def insertTrades(trades: List[Trade]): Command[trades.type] = {
    val enc = tradeEncoder.list(trades)
    sql"""
        INSERT INTO trades
        (
          tradeRefNo
          , accountNo
          , isinCode
          , market
          , buySellFlag
          , unitPrice
          , quantity
          , tradeDate
          , valueDate
          , userId
          , netAmount
        )
        VALUES $enc""".command
  }

  val selectByAccountNoAndDate =
    sql"""
        SELECT t.accountNo, 
               t.isinCode, 
               t.market, 
               t.buySellFlag, 
               t.unitPrice, 
               t.quantity, 
               t.tradeDate, 
               t.valueDate, 
               t.userId,
               t.netAmount,
               f.taxFeeId,
               f.amount,
               t.tradeRefNo
        FROM trades t, tradeTaxFees f
        WHERE t.accountNo = $accountNo
          AND DATE(t.tradeDate) = $date
          AND t.tradeRefNo = f.tradeRefNo
    """.query(tradeTaxFeeDecoder)

  val selectByMarket =
    sql"""
        SELECT t.accountNo, 
               t.isinCode, 
               t.market, 
               t.buySellFlag, 
               t.unitPrice, 
               t.quantity, 
               t.tradeDate, 
               t.valueDate, 
               t.userId,
               t.netAmount,
               f.taxFeeId,
               f.amount,
               t.tradeRefNo
        FROM trades t, tradeTaxFees f
        WHERE t.market = $market
          AND t.tradeRefNo = f.tradeRefNo
    """.query(tradeTaxFeeDecoder)

  val selectAll =
    sql"""
        SELECT t.accountNo, 
               t.isinCode, 
               t.market, 
               t.buySellFlag, 
               t.unitPrice, 
               t.quantity, 
               t.tradeDate, 
               t.valueDate, 
               t.userId,
               t.netAmount,
               f.taxFeeId,
               f.amount,
               t.tradeRefNo
        FROM trades t, tradeTaxFees f
        WHERE t.tradeRefNo = f.tradeRefNo
    """.query(tradeTaxFeeDecoder)

object TradeRepositoryLive:
  val layer = ZLayer.fromFunction(TradeRepositoryLive.apply _)
