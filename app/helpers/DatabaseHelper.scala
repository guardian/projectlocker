package helpers

import javax.inject.{Inject, Singleton}

import models.{FileEntry, _}
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Injected Helper class containing methods for setting up and removing the database
  * @param configuration (provided by DI) - instance of [[play.api.Configuration]]
  * @param dbConfigProvider (provided by DI) - instance of [[play.api.db.slick.DatabaseConfigProvider]]
  */
@Singleton
class DatabaseHelper @Inject()(configuration: Configuration, dbConfigProvider: DatabaseConfigProvider) {

  private val dbConfig = dbConfigProvider.get[PostgresProfile]
  private val logger: Logger = Logger(this.getClass)

  /**
    * Set up the database by installing the schema and test data
    * @return a [[Try]] representing whether the operation was successful, wrapped in a [[Future]]
    */
  def setUpDB():Future[Try[Unit]] = {
    logger.warn("In setUpDB")
    dbConfig.db.run(
      DBIO.seq(
        (TableQuery[FileAssociationRow].schema ++
          TableQuery[FileEntryRow].schema ++
          TableQuery[ProjectEntryRow].schema ++
          TableQuery[ProjectTemplateRow].schema ++
          TableQuery[ProjectTypeRow].schema ++
          TableQuery[StorageEntryRow].schema
        ).create,
        TableQuery[StorageEntryRow] += StorageEntry(None,Some("/tmp"),Some("/tmp"),"Local",Some("me"),None,None,None),
        TableQuery[StorageEntryRow] += StorageEntry(None,None,None,"omms",Some("you"),None,None,None),
        TableQuery[FileEntryRow] += FileEntry(None,"/path/to/a/video.mxf",1,"me",1,new Timestamp(12345678),new Timestamp(12345678),new Timestamp(12345678),hasContent=false,hasLink=true),
        TableQuery[FileEntryRow] += FileEntry(None,"/path/to/secondtestfile",1,"tstuser",1,new Timestamp(123456789),new Timestamp(123456789),new Timestamp(123456789),hasContent = false,hasLink = false),
        //"""{"name": "Premiere test template 1","projectTypeId": 1,"filepath", "storageId": 1}"""
        //"{"name":,"opensWith":"AdobePremierePro.app","targetVersion":"14.0"}"
        TableQuery[ProjectTypeRow] += ProjectType(None,"Premiere 2014 test","AdobePremierePro.app","14.0", Some(".prproj"), None, None),
        TableQuery[ProjectTypeRow] += ProjectType(None,"Cubase 7.0 test","Cubase.app","7.0", Some(".cpr"), None,None),
        TableQuery[ProjectTemplateRow] += ProjectTemplate(Some(1),"Premiere test template 1",1,1)

      ).asTry
    )
  }

  /**
    * Deletes the schema from the database and shuts down the connection
    * @return a [[Try]] indicating whether the operation succeeded or not, wrapped in a [[Future]]
    */
  def teardownDB():Future[Try[Unit]] = {
    logger.warn("In teardownDB")
    val result = dbConfig.db.run(
      DBIO.seq(
        (
          TableQuery[FileAssociationRow].schema ++
            TableQuery[FileEntryRow].schema ++
            TableQuery[ProjectEntryRow].schema ++
            TableQuery[ProjectTemplateRow].schema ++
            TableQuery[ProjectTypeRow].schema ++
            TableQuery[StorageEntryRow].schema
        ).drop
      ).asTry
    )
    //dbConfig.db.close()
    Await.result(dbConfig.db.shutdown,30.seconds)
    result
  }
}
