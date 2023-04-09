package tradex.domain
package repository

import zio.Task
import model.instrument.*

trait InstrumentRepository:

  /** query by isin code */
  def query(isin: ISINCode): Task[Option[Instrument]]

  /** query by instrument type Equity / FI / CCY */
  def queryByInstrumentType(instrumentType: InstrumentType): Task[List[Instrument]]

  /** store */
  def store(ins: Instrument): Task[Instrument]
