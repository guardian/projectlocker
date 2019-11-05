package services.storagemirror

import akka.Done
import akka.actor.Props
import akka.stream.{ActorMaterializer, ClosedShape, Graph, Materializer}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import akka.testkit.TestProbe
import models.{StorageEntry, StorageMirror}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import services.storagemirror.MirrorScanActor._
import utils.AkkaTestkitSpecs2Support
import akka.pattern.ask
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcBackend, JdbcProfile}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class MirrorScanActorSpec extends Specification with Mockito {
  "MirrorScanActor ! MirrorScanStorage" should {
    "build/run no streams and return NoMirrorTargets if sent for a storage that has no mirror targets configured" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)
      implicit val ec:ExecutionContext = system.dispatcher
      val mockedConfig = Configuration.empty
      val mockedDb = mock[DatabaseConfig[JdbcProfile]]
      mockedDb.db returns mock[JdbcProfile#Backend#Database]
      val mockedDbConfigProvider = mock[DatabaseConfigProvider]
      mockedDbConfigProvider.get[JdbcProfile] returns mockedDb

      val mockedBuildStream = mock[(StorageEntry,StorageEntry,Int,Int)=>Graph[ClosedShape.type, Future[Done]]]
      val selfTestProbe = TestProbe()
      implicit val timeout = akka.util.Timeout(10 seconds)


      val toTest = system.actorOf(Props(new MirrorScanActor(mockedDbConfigProvider, mockedConfig) {
        override protected val ownRef = selfTestProbe.ref
        override protected def mirrorTargetsForStorage(storageId: Int): Future[Seq[StorageMirror]] = Future(Seq())
        override protected def buildStream(sourceStorage: StorageEntry, destStorage: StorageEntry, paralellism: Int, dbParalellism: Int): Graph[ClosedShape.type, Future[Done]] = mockedBuildStream(sourceStorage, destStorage, paralellism, dbParalellism)
      }))

      val sourceStorageRef = StorageEntry(Some(123),Some("test entry"),None,None,"TestStorage",None,None,None,None,None,false,None)

      val result = Await.result((toTest ? MirrorScanStorage(sourceStorageRef)).mapTo[MSMsg], timeout.duration)

      there was one(mockedDb).db
      result mustEqual NoMirrorTargets
      there were no(mockedBuildStream).apply(any,any,any,any)
    }

    "build/run a stream for each StorageMirrorTarget configured for a given source" in new AkkaTestkitSpecs2Support {
      implicit val mat:Materializer = ActorMaterializer.create(system)
      implicit val ec:ExecutionContext = system.dispatcher
      val mockedConfig = Configuration.empty
      val mockedDb = mock[DatabaseConfig[JdbcProfile]]
      mockedDb.db returns mock[JdbcProfile#Backend#Database]
      val mockedDbConfigProvider = mock[DatabaseConfigProvider]
      mockedDbConfigProvider.get[JdbcProfile] returns mockedDb

      val mockedBuildStream = mock[(StorageEntry,StorageEntry,Int,Int)=>Graph[ClosedShape.type, Future[Done]]]
      val selfTestProbe = TestProbe()
      implicit val timeout = akka.util.Timeout(10 seconds)


      val toTest = system.actorOf(Props(new MirrorScanActor(mockedDbConfigProvider, mockedConfig) {
        override protected val ownRef = selfTestProbe.ref
        override protected def mirrorTargetsForStorage(storageId: Int): Future[Seq[StorageMirror]] = Future(Seq())
        override protected def buildStream(sourceStorage: StorageEntry, destStorage: StorageEntry, paralellism: Int, dbParalellism: Int): Graph[ClosedShape.type, Future[Done]] = mockedBuildStream(sourceStorage, destStorage, paralellism, dbParalellism)
      }))

      val sourceStorageRef = StorageEntry(Some(123),Some("test entry"),None,None,"TestStorage",None,None,None,None,None,false,None)

      val result = Await.result((toTest ? MirrorScanStorage(sourceStorageRef)).mapTo[MSMsg], timeout.duration)

      there was one(mockedDb).db
      result mustEqual NoMirrorTargets
      there were no(mockedBuildStream).apply(any,any,any,any)
    }
  }
}
