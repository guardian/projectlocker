import com.google.inject.AbstractModule
import helpers.JythonRunner
import play.api.Logger
import services.{PlutoWGCommissionScanner, PostrunActionScanner}

class Module extends AbstractModule{
  override def configure(): Unit = {
    JythonRunner.initialise
    bind(classOf[PostrunActionScanner]).asEagerSingleton()
    bind(classOf[PlutoWGCommissionScanner]).asEagerSingleton()
  }
}
