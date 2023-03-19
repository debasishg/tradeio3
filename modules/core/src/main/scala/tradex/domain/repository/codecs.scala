package tradex.domain
package repository

import cats.syntax.all.*
import skunk.*
import skunk.codec.all.*
import squants.market.*
import model.account.*

object codecs:
  given MoneyContext = defaultMoneyContext

  val accountNo: Codec[AccountNo] =
    varchar.eimap[AccountNo] { s =>
      AccountNo(s).validateNo.toEitherAssociative.leftMap(identity)
    }(AccountNo.unwrap(_))

  val accountName: Codec[AccountName] =
    varchar.eimap[AccountName] { s =>
      AccountName(s).validateName.toEitherAssociative.leftMap(identity)
    }(AccountName.unwrap(_))

  val money: Codec[Money] = numeric.imap[Money](USD(_))(_.amount)

  val currency: Codec[Currency] =
    varchar.eimap[Currency](Currency(_).toEither.leftMap(_.getMessage()))(
      _.code
    )
