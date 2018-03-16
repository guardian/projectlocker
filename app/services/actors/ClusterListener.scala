package services.actors

// see https://doc.akka.io/docs/akka/2.5/cluster-usage.html

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor

class ClusterListener extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  //subscrube to cluster changes, re-subscribe when restarting
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case MemberUp(member)=>
      log.info(s"Member ${member.address} came up")
    case UnreachableMember(member)=>
      log.info(s"Member $member became unreachable")
    case MemberRemoved(member, previousStatus)=>
      log.info(s"Member ${member.address} was removed after $previousStatus")
    case _:MemberEvent=>  //ignore other events
  }
}
