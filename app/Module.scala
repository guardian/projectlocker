import com.google.inject.AbstractModule
import play.api.Logger
import services.PostrunActionScanner

class Module extends AbstractModule{
  override def configure(): Unit = {
    Logger.debug("in configure()")
    bind(classOf[PostrunActionScanner]).asEagerSingleton()
  }
}
