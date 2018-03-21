import com.google.inject.AbstractModule
import helpers.JythonRunner
import play.api.Logger
import play.api.libs.concurrent.AkkaGuiceSupport
import services.actors.MessageProcessorActor
import services.{PlutoWGCommissionScanner, PostrunActionScanner}

class Module extends AbstractModule with AkkaGuiceSupport {
  private val logger = Logger(getClass)

  override def configure(): Unit = {
    JythonRunner.initialise

    if(!sys.env.contains("CI")) {
      bind(classOf[PostrunActionScanner]).asEagerSingleton()
      bind(classOf[PlutoWGCommissionScanner]).asEagerSingleton()
    }
    //this makes the actor instance accessible via injection
    bindActor[MessageProcessorActor]("message-processor-actor")

  }
}
