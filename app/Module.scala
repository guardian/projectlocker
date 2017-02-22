import com.google.inject.AbstractModule
import com.google.inject.name.Names
import actors._
import akka.actor.ActorRef
import play.libs.akka.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    println("in module::configure")
    bindActor(classOf[ProjectCreationActor],"project-creation-actor")
  }
}