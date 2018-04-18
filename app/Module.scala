import com.google.inject.AbstractModule
import helpers.JythonRunner
import play.api.Logger
import play.api.libs.concurrent.AkkaGuiceSupport
import services.actors.MessageProcessorActor
import services._

class Module extends AbstractModule with AkkaGuiceSupport {
  private val logger = Logger(getClass)

  override def configure(): Unit = {
    JythonRunner.initialise

    bind(classOf[TestModeWarning]).asEagerSingleton()

    if(!sys.env.contains("CI")) {
      bind(classOf[PostrunActionScanner]).asEagerSingleton()
      bind(classOf[PlutoWGCommissionScanner]).asEagerSingleton()
      bind(classOf[PlutoProjectTypeScanner]).asEagerSingleton()
      bind(classOf[StorageScanner]).asEagerSingleton()
    }
    //this makes the actor instance accessible via injection
    bindActor[MessageProcessorActor]("message-processor-actor")

  }
}
