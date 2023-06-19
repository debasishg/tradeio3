package tradex.domain
package service

import zio.UIO
import model.account.*

trait AccountService:
  def query(accountNo: AccountNo): UIO[Option[ClientAccount]]
