package services.storagemirror

import akka.actor.{Actor, ActorSystem}
import akka.stream.{ClosedShape, Materializer}
import akka.stream.scaladsl.{GraphDSL, Sink}
import javax.inject.{Inject, Singleton}
import models.{FileEntry, FileEntryRow, StorageEntry}
import play.api.db.slick.DatabaseConfigProvider
import services.storagemirror.MirrorScanActor.{MirrorScanStorage, TimedTrigger}
import services.storagemirror.streamcomponents.{FileExistsSwitch, FileMtimeSwitch}
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery

object MirrorScanActor {
  trait MSMsg

  case class MirrorScanStorage(storageRef:StorageEntry)
  case object TimedTrigger
}

@Singleton
class MirrorScanActor @Inject() (dbConfigProvider:DatabaseConfigProvider)(implicit system:ActorSystem, mat:Materializer) extends Actor {
  private implicit val db = dbConfigProvider.get[PostgresProfile].db
//
//  implicit val session = SlickSession.forConfig("slick-h2")
//  system.registerOnTermination(session.close())
//
//  def buildStream(sourceStorage:StorageEntry) = {
//    val finalSinkFac = Sink.ignore
//    GraphDSL.create(finalSinkFac) {implicit builder=> finalSink=>
//      val src=  builder.add(Slick.source(TableQuery[FileEntryRow].filter(_.storage===sourceStorage.id.get).result))
//      val existSwitch = builder.add(new FileExistsSwitch(sourceStorage))
//      val mTimeSwitch = builder.add(new FileMtimeSwitch(sourceStorage))
//
//      src ~> existSwitch
//      existSwitch.out(0) ~> mTimeSwitch //file does exist, check mtime
//      existSwitch.out(1) ~> markLost ~> writerMerge
//
//      mTimeSwitch.out(0) ~> finalSink //mtime matches, don't need to replicate
//
//      ClosedShape
//    }
//  }

  override def receive: Receive = {
    case TimedTrigger=>
      //search database for storages that have replicas set

    case MirrorScanStorage(storageRef)=>

  }
}
