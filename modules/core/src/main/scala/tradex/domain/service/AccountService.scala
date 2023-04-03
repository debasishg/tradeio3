package tradex.domain
package service

import zio.Task
import zio.stream.ZStream
import model.account.*
import skunk.*
import repository.live.AccountRepositorySQL
import zio.interop.catz.*
import zio.stream.interop.fs2z.*

/** Pattern for implementing service APIs that return streams */
trait AccountService:
  def allAccounts: ZStream[Any, Throwable, ClientAccount]

object AccountService:
  def fromSession(s: Session[Task]): Task[AccountService] =
    s.prepare(AccountRepositorySQL.selectAll).map { pq =>
      // Our service implementation. Note that we are preparing the query on construction, so
      // our service can run it many times without paying the planning cost again.
      new AccountService:
        def allAccounts: ZStream[Any, Throwable, ClientAccount] = pq.stream(Void, 512).toZStream()
    }
