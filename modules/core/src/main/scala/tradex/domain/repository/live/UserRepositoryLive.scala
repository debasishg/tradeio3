package tradex.domain
package repository
package live

import zio.{ Random, Task }
import cats.effect.Resource
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import model.user.*
import codecs.{ given, * }
import zio.interop.catz.*

final case class UserRepositoryLive(postgres: Resource[Task, Session[Task]]) extends UserRepository:
  import UserRepositorySQL.*

  override def query(userName: UserName): Task[Option[User]] =
    postgres.use { session =>
      session.prepare(selectByUserName).flatMap { ps =>
        ps.option(userName)
      }
    }

  override def store(userName: UserName, password: EncryptedPassword): Task[UserId] =
    postgres.use { session =>
      session.prepare(upsertUser).flatMap { cmd =>
        Random.nextUUID.flatMap { id =>
          cmd
            .execute(User(UserId(id), userName, password))
            .as(UserId(id))
        }
      }
    }

private[domain] object UserRepositorySQL:
  val decoder: Decoder[User] =
    (userId ~ userName ~ encPassword)
      .gmap[User]

  val selectByUserName: Query[UserName, User] =
    sql"""
        SELECT u.id, u.name, u.password
        FROM users AS u
        WHERE u.name = $userName
       """.query(decoder)

  val upsertUser: Command[User] =
    sql"""
        INSERT INTO users (id, name, password)
        VALUES ($userId, $userName, $encPassword)
        ON CONFLICT(name) DO UPDATE SET
          password = EXCLUDED.password
       """.command.gcontramap[User]
