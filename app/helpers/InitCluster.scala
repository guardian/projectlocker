package helpers

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.ActorMaterializer
import javax.inject.Inject
import play.api.{Configuration, Logger}

class InitCluster @Inject()(config:Configuration, system:ActorSystem){
  private val logger = Logger(getClass)

  implicit val systemImpl = system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  logger.info("In InitCluster class")

  if(config.has("akka.discovery.method")){
    logger.debug("config has akka discovery set, bootstrapping cluster...")
    implicit val cluster = Cluster(system)
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()
  } else {
    logger.debug("not running automatic cluster bootstrap, config has no discovery set.")
  }
}
