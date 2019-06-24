import com.google.inject.AbstractModule
import helpers.{JythonRunner}
import play.api.Logger
import play.api.libs.concurrent.AkkaGuiceSupport
import services.actors.{MessageProcessorActor, ProjectCreationActor}
import services._

class Module extends AbstractModule with AkkaGuiceSupport {
  private val logger = Logger(getClass)

  override def configure(): Unit = {
    JythonRunner.initialise

    bind(classOf[TestModeWarning]).asEagerSingleton()

    if(!sys.env.contains("CI")) {
      bind(classOf[AppStartup]).asEagerSingleton()
      bind(classOf[StorageScanner]).asEagerSingleton()
    }
    //this makes the actor instance accessible via injection
    bindActor[MessageProcessorActor]("message-processor-actor")
    bindActor[ProjectCreationActor]("project-creation-actor")
    bindActor[PostrunActionScanner]("postrun-action-scanner")
    bindActor[PlutoWGCommissionScanner]("pluto-wg-commission-scanner")
    bindActor[PlutoProjectTypeScanner]("pluto-project-type-scanner")
  }
}
