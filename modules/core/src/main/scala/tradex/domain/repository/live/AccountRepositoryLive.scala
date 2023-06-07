package tradex.domain
package repository
package live

import java.time.LocalDate
import cats.syntax.all.*
import cats.effect.Resource
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import model.account.*
import codecs.{ *, given }
import zio.{ Chunk, Task, ZIO, ZLayer }
import zio.stream.ZStream
import zio.interop.catz.*
import zio.stream.interop.fs2z.*

final case class AccountRepositoryLive(postgres: Resource[Task, Session[Task]]) extends AccountRepository:
  import AccountRepositorySQL._

  def query(no: AccountNo): Task[Option[ClientAccount]] =
    postgres.use(session => session.prepare(selectByAccountNo).flatMap(ps => ps.option(no)))

  def store(a: ClientAccount, upsert: Boolean = true): Task[ClientAccount] =
    postgres.use: session =>
      session
        .prepare(if (upsert) upsertAccount else insertAccount)
        .flatMap: cmd =>
          cmd.execute(a).void.map(_ => a)

  def query(openedOn: LocalDate): Task[List[ClientAccount]] =
    postgres.use: session =>
      session
        .prepare(selectByOpenedDate)
        .flatMap: ps =>
          ps.stream(openedOn, 1024).compile.toList

  def all: Task[List[ClientAccount]] = postgres.use: session =>
    session.execute(selectAll)

  def allClosed(closeDate: Option[LocalDate]): Task[List[ClientAccount]] =
    postgres.use: session =>
      closeDate
        .map: cd =>
          session
            .prepare(selectClosedAfter)
            .flatMap: ps =>
              ps.stream(cd, 1024).compile.toList
        .getOrElse:
          session.execute(selectAllClosed)

private[domain] object AccountRepositorySQL:

  val accountEncoder: Encoder[ClientAccount] =
    (
      accountNo ~ accountName ~ timestamp ~ timestamp.opt ~ currency ~ currency.opt ~ currency.opt
    ).values.contramap:
      case TradingAccount(AccountBase(no, nm, dop, doc, bc), tc) =>
        no ~ nm ~ dop ~ doc ~ bc ~ Some(tc.tradingCurrency) ~ None

      case SettlementAccount(AccountBase(no, nm, dop, doc, bc), sc) =>
        no ~ nm ~ dop ~ doc ~ bc ~ None ~ Some(sc.settlementCurrency)

      case TradingAndSettlementAccount(AccountBase(no, nm, dop, doc, bc), tsc) =>
        no ~ nm ~ dop ~ doc ~ bc ~ Some(tsc.tradingCurrency) ~ Some(tsc.settlementCurrency)

  val accountDecoder: Decoder[ClientAccount] =
    (accountNo ~ accountName ~ timestamp ~ timestamp.opt ~ currency ~ currency.opt ~ currency.opt)
      .map:
        case no ~ nm ~ dp ~ dc ~ bc ~ tc ~ None =>
          TradingAccount
            .tradingAccount(
              no,
              nm,
              Some(dp),
              dc,
              bc,
              tc.get
            )
            .fold(errs => throw new Exception(errs.mkString), identity)
        case no ~ nm ~ dp ~ dc ~ bc ~ None ~ sc =>
          SettlementAccount
            .settlementAccount(
              no,
              nm,
              Some(dp),
              dc,
              bc,
              sc.get
            )
            .fold(errs => throw new Exception(errs.mkString), identity)
        case no ~ nm ~ dp ~ dc ~ bc ~ tc ~ sc =>
          TradingAndSettlementAccount
            .tradingAndSettlementAccount(
              no,
              nm,
              Some(dp),
              dc,
              bc,
              tc.get,
              sc.get
            )
            .fold(errs => throw new Exception(errs.mkString), identity)

  val selectByAccountNo: Query[AccountNo, ClientAccount] =
    sql"""
        SELECT a.no, a.name, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE a.no = $accountNo
       """.query(accountDecoder)

  val selectByOpenedDate: Query[LocalDate, ClientAccount] =
    sql"""
        SELECT a.no, a.name, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE DATE(a.dateOfOpen) = $date
       """.query(accountDecoder)

  val selectAll: Query[Void, ClientAccount] =
    sql"""
        SELECT a.no, a.name, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
       """.query(accountDecoder)

  val selectClosedAfter: Query[LocalDate, ClientAccount] =
    sql"""
        SELECT a.no, a.name, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE a.dateOfClose >= $date
       """.query(accountDecoder)

  val selectAllClosed: Query[Void, ClientAccount] =
    sql"""
        SELECT a.no, a.name, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE a.dateOfClose IS NOT NULL
       """.query(accountDecoder)

  val insertAccount: Command[ClientAccount] =
    sql"""
        INSERT INTO accounts
        VALUES $accountEncoder
       """.command

  val upsertAccount: Command[ClientAccount] =
    sql"""
        INSERT INTO accounts
        VALUES $accountEncoder
        ON CONFLICT(no) DO UPDATE SET
          name                 = EXCLUDED.name,
          dateOfOpen           = EXCLUDED.dateOfOpen,
          dateOfClose          = EXCLUDED.dateOfClose,
          baseCurrency         = EXCLUDED.baseCurrency,
          tradingCurrency      = EXCLUDED.tradingCurrency,
          settlementCurrency   = EXCLUDED.settlementCurrency
       """.command

object AccountRepositoryLive:
  val layer = ZLayer.fromFunction(AccountRepositoryLive.apply _)

  /** stream based computation pattern. Need a session which will be passed from the call site using `AppResources` */
  def streamAccountsAndDoStuff(
      session: Session[Task]
  ): Task[Long] =
    session
      .prepare(AccountRepositorySQL.selectAll)           // prepare the query once
      .flatMap(_.stream(Void, 512).toZStream().runCount) // run the streaming logic within the flatMap
