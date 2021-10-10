package tradex.domain
package repository

import java.time.LocalDate
import cats.effect.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import model.account.*
import codecs.{ given, * }

trait AccountRepository[F[_]]:

  /** query by account number */
  def query(no: AccountNo): F[Option[Account]]

  /** store */
  def store(a: Account, upsert: Boolean = true): F[Account]

  /** query by opened date */
  def query(openedOn: LocalDate): F[List[Account]]

  /** all accounts */
  def all: F[List[Account]]

  /** all closed accounts, if date supplied then all closed after that date */
  def allClosed(closeDate: Option[LocalDate]): F[List[Account]]

  /** all accounts trading / settlement / both */
  def allAccountsOfType(accountType: AccountType): F[List[Account]]

object AccountRepository:
  def make[F[_]](
      postgres: Resource[F, Session[F]]
  )(using Concurrent[F]): AccountRepository[F] = ???

private object AccountRepositorySQL:
  // A codec that maps Postgres type `accountType` to Scala type `AccountType`
  // val accountType = `enum`(AccountType, Type("accounttype"))

  val accountEncoder: Encoder[Account] =
    (
      accountNo ~ accountName ~ accountType ~ timestamp ~ timestamp.opt ~ currency ~ currency.opt ~ currency.opt
    ).values.contramap {
      case Account(no, nm, dop, doc, AccountType.Both, bc, tc, sc) =>
        no ~ nm ~ AccountType.Both ~ dop ~ doc ~ bc ~ tc ~ sc

      case Account(no, nm, dop, doc, AccountType.Trading, bc, tc, _) =>
        no ~ nm ~ AccountType.Trading ~ dop ~ doc ~ bc ~ tc ~ None

      case Account(no, nm, dop, doc, AccountType.Settlement, bc, _, sc) =>
        no ~ nm ~ AccountType.Settlement ~ dop ~ doc ~ bc ~ None ~ sc
    }

  val accountDecoder: Decoder[Account] =
    (accountNo ~ accountName ~ accountType ~ timestamp ~ timestamp.opt ~ currency ~ currency.opt ~ currency.opt)
      .map { case no ~ nm ~ tp ~ dp ~ dc ~ bc ~ tc ~ sc =>
        tp match
          case AccountType.Trading =>
            Account(
              no,
              nm,
              dp,
              dc,
              AccountType.Trading,
              bc,
              tc,
              None
            )
          case AccountType.Settlement =>
            Account(
              no,
              nm,
              dp,
              dc,
              AccountType.Settlement,
              bc,
              None,
              sc
            )
          case AccountType.Both =>
            Account(
              no,
              nm,
              dp,
              dc,
              AccountType.Both,
              bc,
              tc,
              sc
            )
      }

  val selectByAccountNo: Query[AccountNo.Type, Account] =
    sql"""
        SELECT a.no, a.name, a.type, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE a.no = $accountNo
       """.query(accountDecoder)

  val selectByOpenedDate: Query[LocalDate, Account] =
    sql"""
        SELECT a.no, a.name, a.type, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE DATE(a.dateOfOpen) = $date
       """.query(accountDecoder)

  val selectByAccountType: Query[AccountType, Account] =
    sql"""
        SELECT a.no, a.name, a.type, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE a.type = $accountType
       """.query(accountDecoder)

  val selectAll: Query[Void, Account] =
    sql"""
        SELECT a.no, a.name, a.type, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
       """.query(accountDecoder)

  val selectClosedAfter: Query[LocalDate, Account] =
    sql"""
        SELECT a.no, a.name, a.type, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE a.dateOfClose >= $date
       """.query(accountDecoder)

  val selectAllClosed: Query[Void, Account] =
    sql"""
        SELECT a.no, a.name, a.type, a.dateOfOpen, a.dateOfClose, a.baseCurrency, a.tradingCurrency, a.settlementCurrency
        FROM accounts AS a
        WHERE a.dateOfClose IS NOT NULL
       """.query(accountDecoder)

  val insertAccount: Command[Account] =
    sql"""
        INSERT INTO accounts
        VALUES $accountEncoder
       """.command

  val updateAccount: Command[Account] =
    sql"""
        UPDATE accounts SET
          name                = $accountName,
          type                = $accountType,
          dateOfOpen          = $timestamp,
          dateOfClose         = ${timestamp.opt},
          baseCurrency        = $currency,
          tradingCurrency     = ${currency.opt},
          settlementCurrency  = ${currency.opt}
        WHERE no = $accountNo
       """.command.contramap {
      case Account(no, nm, dop, doc, AccountType.Trading, bc, tc, _) =>
        nm ~ AccountType.Trading ~ dop ~ doc ~ bc ~ tc ~ None ~ no

      case Account(no, nm, dop, doc, AccountType.Settlement, bc, _, sc) =>
        nm ~ AccountType.Settlement ~ dop ~ doc ~ bc ~ None ~ sc ~ no

      case Account(no, nm, dop, doc, AccountType.Both, bc, tc, sc) =>
        nm ~ AccountType.Both ~ dop ~ doc ~ bc ~ tc ~ sc ~ no
    }

  val upsertAccount: Command[Account] =
    sql"""
        INSERT INTO accounts
        VALUES $accountEncoder
        ON CONFLICT(no) DO UPDATE SET
          name                 = EXCLUDED.name,
          type                 = EXCLUDED.type,
          dateOfOpen           = EXCLUDED.dateOfOpen,
          dateOfClose          = EXCLUDED.dateOfClose,
          baseCurrency         = EXCLUDED.baseCurrency,
          tradingCurrency      = EXCLUDED.tradingCurrency,
          settlementCurrency   = EXCLUDED.settlementCurrency
       """.command
