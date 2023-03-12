package tradex.domain
package repository

import cats.syntax.all._
import skunk._
import skunk.codec.all._
import squants.market._
import model.newAccount._

object newcodecs:
  given MoneyContext = defaultMoneyContext

  val accountNo: Codec[AccountNo.Type] =
    varchar.eimap[AccountNo.Type] { s =>
      AccountNo(s).validateNo.toEitherAssociative.leftMap(identity)
    }(_.value)

  val accountName: Codec[AccountName.Type] =
    varchar.eimap[AccountName.Type] { s =>
      AccountName(s).validateName.toEitherAssociative.leftMap(identity)
    }(_.value)

  val money: Codec[Money] = numeric.imap[Money](USD(_))(_.amount)

  val currency: Codec[Currency] =
    varchar.eimap[Currency](Currency(_).toEither.leftMap(_.getMessage()))(
      _.code
    )
