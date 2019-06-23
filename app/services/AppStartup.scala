package services

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.ActorMaterializer
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.Configuration
import play.api.inject.Injector

@Singleton
class AppStartup @Inject()(config:Configuration, injector:Injector)(implicit system:ActorSystem){
  private val logger = Logger(getClass)

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def initialise() = {
    logger.info("In InitCluster class")

    if (config.has("akka.discovery.method")) {
      logger.debug("config has akka discovery set, bootstrapping cluster...")
      implicit val cluster = Cluster(system)
      AkkaManagement(system).start()
      ClusterBootstrap(system).start()
    } else {
      logger.debug("not running automatic cluster bootstrap, config has no discovery set.")
    }

    logger.info("Starting up master timer")
    system.actorOf(ClusterSingletonManager.props(
      singletonProps = Props(injector.instanceOf(classOf[ClockSingleton])),
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system)
    ), name="ClockSingleton"
    )
  }

  initialise()
}
