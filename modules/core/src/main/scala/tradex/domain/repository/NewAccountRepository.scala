package tradex.domain
package repository

import java.time.LocalDate
import cats.syntax.all._
import cats.effect.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import model.newAccount.*
import newcodecs.{ given, * }

trait NewAccountRepository[F[_]]:

  /** query by account number */
  def query(no: AccountNo.Type): F[Option[ClientAccount]]

  /** store */
  def store(a: ClientAccount, upsert: Boolean = true): F[ClientAccount]

  /** query by opened date */
  def query(openedOn: LocalDate): F[List[ClientAccount]]

  /** all accounts */
  def all: F[List[ClientAccount]]

  /** all closed accounts, if date supplied then all closed after that date */
  def allClosed(closeDate: Option[LocalDate]): F[List[ClientAccount]]

object NewAccountRepository:
  def make[F[_]](
      postgres: Resource[F, Session[F]]
  )(using Concurrent[F]): NewAccountRepository[F] =
    new NewAccountRepository[F]:
      import NewAccountRepositorySQL._

      def query(no: AccountNo.Type): F[Option[ClientAccount]] =
        postgres.use { session =>
          session.prepare(selectByAccountNo).use { ps =>
            ps.option(no)
          }
        }

      def store(a: ClientAccount, upsert: Boolean = true): F[ClientAccount] =
        postgres.use { session =>
          session
            .prepare((if (upsert) upsertAccount else insertAccount))
            .use { cmd =>
              cmd.execute(a).void.map(_ => a)
            }
        }

      def query(openedOn: LocalDate): F[List[ClientAccount]] =
        postgres.use { session =>
          session.prepare(selectByOpenedDate).use { ps =>
            ps.stream(openedOn, 1024).compile.toList
          }
        }

      def all: F[List[ClientAccount]] = postgres.use(_.execute(selectAll))

      def allClosed(closeDate: Option[LocalDate]): F[List[ClientAccount]] =
        postgres.use { session =>
          closeDate
            .map { cd =>
              session.prepare(selectClosedAfter).use { ps =>
                ps.stream(cd, 1024).compile.toList
              }
            }
            .getOrElse {
              session.execute(selectAllClosed)
            }
        }

private object NewAccountRepositorySQL:

  val accountEncoder: Encoder[ClientAccount] =
    (
      accountNo ~ accountName ~ timestamp ~ timestamp.opt ~ currency ~ currency.opt ~ currency.opt
    ).values.contramap {
      case TradingAccount(AccountBase(no, nm, dop, doc, bc), tc) =>
        no ~ nm ~ dop ~ doc ~ bc ~ Some(tc.tradingCurrency) ~ None

      case SettlementAccount(AccountBase(no, nm, dop, doc, bc), sc) =>
        no ~ nm ~ dop ~ doc ~ bc ~ None ~ Some(sc.settlementCurrency)

      case TradingAndSettlementAccount(AccountBase(no, nm, dop, doc, bc), tsc) =>
        no ~ nm ~ dop ~ doc ~ bc ~ Some(tsc.tradingCurrency) ~ Some(tsc.settlementCurrency)
    }

  val accountDecoder: Decoder[ClientAccount] =
    (accountNo ~ accountName ~ timestamp ~ timestamp.opt ~ currency ~ currency.opt ~ currency.opt)
      .map {
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
      }

  val selectByAccountNo: Query[AccountNo.Type, ClientAccount] =
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
