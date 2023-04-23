package tradex.domain
package repository

import zio.Task
import model.user.*

trait UserRepository {

  /** query by username */
  def query(username: UserName): Task[Option[User]]

  /** store a user * */
  def store(username: UserName, password: EncryptedPassword): Task[UserId]
}
