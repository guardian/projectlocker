package helpers

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.ActorMaterializer
import javax.inject.Inject
import play.api.Logger

class InitCluster @Inject()(system:ActorSystem){
  private val logger = Logger(getClass)

  implicit val systemImpl = system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val cluster = Cluster(system)

  logger.info("In InitManagement class")
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()
}
