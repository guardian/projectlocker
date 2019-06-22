package services

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.ActorMaterializer
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.Configuration

@Singleton
class AppStartup @Inject()(config:Configuration)(implicit system:ActorSystem){
  private val logger = Logger(getClass)

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
