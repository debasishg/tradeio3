package tradex.domain
package repository

import zio.UIO
import model.instrument.*

trait InstrumentRepository:

  /** query by isin code */
  def query(isin: ISINCode): UIO[Option[Instrument]]

  /** query by instrument type Equity / FI / CCY */
  def queryByInstrumentType(instrumentType: InstrumentType): UIO[List[Instrument]]

  /** store */
  def store(ins: Instrument): UIO[Instrument]
