package tradex.domain
package repository

import java.time.LocalDate
import model.account.*
import zio.UIO

trait AccountRepository:

  /** query by account number */
  def query(no: AccountNo): UIO[Option[ClientAccount]]

  /** store */
  def store(a: ClientAccount, upsert: Boolean = true): UIO[ClientAccount]

  /** query by opened date */
  def query(openedOn: LocalDate): UIO[List[ClientAccount]]

  /** all accounts */
  def all: UIO[List[ClientAccount]]

  /** all closed accounts, if date supplied then all closed after that date */
  def allClosed(closeDate: Option[LocalDate]): UIO[List[ClientAccount]]
