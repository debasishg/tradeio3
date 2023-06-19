package tradex.domain
package service
package live

import zio.UIO
import model.account.*
import repository.AccountRepository

final case class AccountServiceLive(
    repository: AccountRepository
) extends AccountService:

  override def query(accountNo: AccountNo): UIO[Option[ClientAccount]] =
    repository.query(accountNo)
