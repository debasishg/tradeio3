package tradex.domain
package service

import zio.Task
import model.account.*

trait AccountService:
  def query(accountNo: AccountNo): Task[Option[ClientAccount]]
