package tradex.domain

import zio.test.*
import zio.ZLayer
import zio.logging.slf4j.bridge.Slf4jBridge
import tradex.domain.Fixture.appResourcesL

abstract class ITSpecDefault extends ZIOSpecDefault:

  override val bootstrap: ZLayer[Any, Any, Environment with TestEnvironment] =
    val nullLogger = zio.logging.removeDefaultLoggers >>> Slf4jBridge.initialize
    testEnvironment >+> nullLogger

  val appResourcesL = Fixture.appResourcesL
