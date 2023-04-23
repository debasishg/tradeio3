package tradex.domain
package repository
package live

import zio.{ Task, ZLayer }
import cats.effect.kernel.Resource
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import model.account.*
import model.balance.*
import java.time.LocalDate
import codecs.{ given, * }
import model.balance.*
import zio.interop.catz.*

final case class BalanceRepositoryLive(postgres: Resource[Task, Session[Task]]) extends BalanceRepository:
  import BalanceRepositorySQL.*

  override def store(b: Balance): Task[Balance] =
    postgres.use { session =>
      session.prepare(upsertBalance).flatMap { cmd =>
        cmd.execute(b).unit.map(_ => b)
      }
    }

  override def all: Task[List[Balance]] = postgres.use(_.execute(selectAll))

  override def query(date: LocalDate): Task[List[Balance]] =
    postgres.use { session =>
      session.prepare(selectByDate).flatMap { ps =>
        ps.stream(date, 1024).compile.toList
      }
    }

  override def query(no: AccountNo): Task[Option[Balance]] =
    postgres.use { session =>
      session.prepare(selectByAccountNo).flatMap { ps =>
        ps.option(no)
      }
    }

private[domain] object BalanceRepositorySQL:
  val decoder: Decoder[Balance] =
    (accountNo ~ money ~ timestamp ~ currency)
      .map { case ano ~ amt ~ asOf ~ ccy =>
        Balance(ano, amt, ccy, asOf)
      }

  val encoder: Encoder[Balance] =
    (accountNo ~ money ~ timestamp ~ currency).values
      .contramap((b: Balance) => b.accountNo ~ b.amount ~ b.asOf ~ b.currency)

  val selectByAccountNo: Query[AccountNo, Balance] =
    sql"""
        SELECT b.accountNo, b.amount, b.asOf, b.currency
        FROM balance AS b
        WHERE b.accountNo = $accountNo
       """.query(decoder)

  val selectByDate: Query[LocalDate, Balance] =
    sql"""
        SELECT b.accountNo, b.amount, b.asOf, b.currency
        FROM balance AS b
        WHERE DATE(b.asOf) <= $date
       """.query(decoder)

  val selectAll: Query[Void, Balance] =
    sql"""
        SELECT b.accountNo, b.amount, b.asOf, b.currency
        FROM balance AS b
       """.query(decoder)

  val insertBalance: Command[Balance] =
    sql"""
        INSERT INTO balance (accountNo, amount, asOf, currency)
        VALUES $encoder
       """.command

  val upsertBalance: Command[Balance] =
    sql"""
        INSERT INTO balance (accountNo, amount, asOf, currency)
        VALUES $encoder
        ON CONFLICT(accountNo) DO UPDATE SET
          amount    = EXCLUDED.amount,
          asOf      = EXCLUDED.asOf,
          currency  = EXCLUDED.currency
       """.command

object BalanceeRepositoryLive:
  val layer = ZLayer.fromFunction(BalanceRepositoryLive.apply _)
