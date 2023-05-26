package tradex.domain
package service
package live

import zio.Task
import model.account.*
import repository.AccountRepository

final case class AccountServiceLive(
    repository: AccountRepository
) extends AccountService:

  override def query(accountNo: AccountNo): Task[Option[ClientAccount]] =
    repository.query(accountNo)
