package tradex.domain
package repository
package live

import cats.syntax.all.*
import cats.effect.Resource
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import model.account.*
import codecs.{ *, given }
import zio.{ Task, ZLayer }
import zio.stream.ZStream
import zio.interop.catz.*
import model.instrument.*

final case class InstrumentRepositoryLive(postgres: Resource[Task, Session[Task]]) extends InstrumentRepository:
  import InstrumentRepositorySQL._

  override def queryByInstrumentType(instrumentType: InstrumentType): Task[List[Instrument]] =
    postgres.use(session =>
      session.prepare(selectByInstrumentType).flatMap(ps => ps.stream(instrumentType, 1024).compile.toList)
    )

  override def query(isin: ISINCode): Task[Option[Instrument]] =
    postgres.use(session => session.prepare(selectByISINCode).flatMap(ps => ps.option(isin)))

  override def store(ins: Instrument): Task[Instrument] =
    postgres.use(session => session.prepare(upsertInstrument).flatMap(_.execute(ins).void.map(_ => ins)))

private object InstrumentRepositorySQL:

  val instrumentEncoder: Encoder[Instrument] =
    (
      isinCode ~ instrumentName ~ instrumentType ~ timestamp.opt ~ timestamp.opt ~ lotSize ~ unitPrice.opt ~ money.opt ~ couponFrequency.opt
    ).values
      .contramap:
        case Ccy(InstrumentBase(isin, name, ls)) =>
          isin ~ name ~ InstrumentType.CCY ~ None ~ None ~ ls ~ None ~ None ~ None
        case Equity(InstrumentBase(isin, name, ls), di, up) =>
          isin ~ name ~ InstrumentType.Equity ~ Some(di) ~ None ~ ls ~ Some(up) ~ None ~ None
        case FixedIncome(InstrumentBase(isin, name, ls), di, dm, cr, cf) =>
          isin ~ name ~ InstrumentType.FixedIncome ~ Some(di) ~ dm ~ ls ~ None ~ Some(cr) ~ Some(cf)

  val decoder: Decoder[Instrument] =
    (isinCode ~ instrumentName ~ instrumentType ~ timestamp.opt ~ timestamp.opt ~ lotSize ~ unitPrice.opt ~ money.opt ~ couponFrequency.opt)
      .map:
        case isin ~ nm ~ tp ~ di ~ dm ~ ls ~ up ~ cr ~ cf =>
          tp match
            case InstrumentType.CCY         => Ccy(InstrumentBase(isin, nm, ls))
            case InstrumentType.Equity      => Equity(InstrumentBase(isin, nm, ls), di.get, up.get)
            case InstrumentType.FixedIncome => FixedIncome(InstrumentBase(isin, nm, ls), di.get, dm, cr.get, cf.get)

  val selectByISINCode: Query[ISINCode, Instrument] =
    sql"""
        SELECT i.isinCode, i.name, i.type, i.dateOfIssue, i.dateOfMaturity, i.lotSize, i.unitPrice, i.couponRate, i.couponFrequency
        FROM instruments AS i
        WHERE i.isinCode = $isinCode
       """.query(decoder)

  val selectByInstrumentType: Query[InstrumentType, Instrument] =
    sql"""
        SELECT i.isinCode, i.name, i.type, i.dateOfIssue, i.dateOfMaturity, i.lotSize, i.unitPrice, i.couponRate, i.couponFrequency
        FROM instruments AS i
        WHERE i.type = $instrumentType
       """.query(decoder)

  val upsertInstrument: Command[Instrument] =
    sql"""
        INSERT INTO instruments
        VALUES $instrumentEncoder
        ON CONFLICT(isinCode) DO UPDATE SET
          name                 = EXCLUDED.name,
          type                 = EXCLUDED.type,
          dateOfIssue          = EXCLUDED.dateOfIssue,
          dateOfMaturity       = EXCLUDED.dateOfMaturity,
          lotSize              = EXCLUDED.lotSize,
          unitPrice            = EXCLUDED.unitPrice,
          couponRate           = EXCLUDED.couponRate,
          couponFrequency      = EXCLUDED.couponFrequency
       """.command

object InstrumentRepositoryLive:
  val layer = ZLayer.fromFunction(InstrumentRepositoryLive.apply _)
