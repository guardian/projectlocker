import com.google.inject.AbstractModule
import helpers.JythonRunner
import play.api.Logger
import services.{PlutoMessengerProcesser, PlutoQueueListener, PlutoWGCommissionScanner, PostrunActionScanner}

class Module extends AbstractModule{
  private val logger = Logger(getClass)

  override def configure(): Unit = {
    JythonRunner.initialise
    bind(classOf[PostrunActionScanner]).asEagerSingleton()
    bind(classOf[PlutoWGCommissionScanner]).asEagerSingleton()
    try {
      bind(classOf[PlutoQueueListener]).asEagerSingleton()
    } catch {
      case ex:Throwable=>
        logger.error("Could not intialise PlutoMessenger subscriber: ", ex)
    }
  }
}
