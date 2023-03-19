package tradex.domain
package repository

import java.time.LocalDate
import model.account.*
import zio.Task
import zio.stream.ZStream

trait AccountRepository:

  /** query by account number */
  def query(no: AccountNo): Task[Option[ClientAccount]]

  /** store */
  def store(a: ClientAccount, upsert: Boolean = true): Task[ClientAccount]

  /** query by opened date */
  def query(openedOn: LocalDate): Task[List[ClientAccount]]

  /** all accounts */
  def all: Task[List[ClientAccount]]

  /** all closed accounts, if date supplied then all closed after that date */
  def allClosed(closeDate: Option[LocalDate]): Task[List[ClientAccount]]

  def streamAllAccounts: ZStream[Any, Throwable, ClientAccount]
