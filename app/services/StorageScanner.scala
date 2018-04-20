package services

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import models.{StorageEntry, StorageEntryHelper, StorageStatus}
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class StorageScanner @Inject() (dbConfigProvider:DatabaseConfigProvider, config:Configuration, actorSystem:ActorSystem){
  private val logger = Logger(getClass)
  implicit val db = dbConfigProvider.get[PostgresProfile].db
  implicit val configImplicit = config

  def checkStorage(entry: StorageEntry): Unit ={
    entry.rootpath match {
      case Some(rootpath)=>
        entry.getStorageDriver match {
          case Some (storageDriver) =>
            if (storageDriver.pathExists(rootpath)){
              logger.debug(s"Storage ${entry.storageType} ${entry.rootpath} is online")
              if(entry.status.getOrElse(StorageStatus.UNKNOWN)!=StorageStatus.ONLINE) entry.copy(status=Some(StorageStatus.ONLINE)).save
            } else {
              logger.warn(s"Storage ${entry.storageType} ${entry.rootpath} refers to a path that does not exist")
              if(entry.status.getOrElse(StorageStatus.UNKNOWN)!=StorageStatus.DISAPPEARED) entry.copy(status=Some(StorageStatus.DISAPPEARED)).save
            }
          case None=>
            logger.warn(s"Storage ${entry.storageType} ${entry.rootpath} has no storage driver configured")
            if(entry.status.getOrElse(StorageStatus.UNKNOWN)!=StorageStatus.MISCONFIGURED) entry.copy(status=Some(StorageStatus.MISCONFIGURED)).save
        }
      case None=>
        logger.debug(s"Storage ${entry.storageType} ${entry.rootpath} has no root path set")
        if(entry.storageType=="Local"){
          if(entry.status.getOrElse(StorageStatus.UNKNOWN)!=StorageStatus.MISCONFIGURED) entry.copy(status=Some(StorageStatus.MISCONFIGURED)).save
        }
    }
  }
  val cancellable = actorSystem.scheduler.schedule(1 second, 30 seconds) {
    logger.debug("Scanning storages...")

    StorageEntryHelper.allStorages.map({
      case Success(storageList)=>
        storageList.foreach(entry=>checkStorage(entry))
      case Failure(error)=>
        logger.error("Could not get storage list: ", error)
    })
  }
}
