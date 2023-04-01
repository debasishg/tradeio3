package tradex.domain

import zio.*
import zio.logging.backend.SLF4J
import repository.AccountRepository
import repository.live.AccountRepositoryLive
import tradex.domain.{ config => tradeconfig }

object application:
  // add more repositories here with ++
  type RepositoryLayerEnv = AccountRepository

  private val logger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  // compose layers for prod
  object prod:
    // config layer
    val configLayer = tradeconfig.live ++ logger

    // repository layer
    val repositoryLayer =
      AccountRepositoryLive.layer

    // final application layer for prod
    val appLayer =
      configLayer >+>
        repositoryLayer
