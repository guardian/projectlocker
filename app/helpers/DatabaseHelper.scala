package helpers

import com.google.inject.Inject
import models.{FileEntry, _}
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._
import java.sql.Timestamp

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class DatabaseHelper @Inject()(configuration: Configuration, dbConfigProvider: DatabaseConfigProvider) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val logger: Logger = Logger(this.getClass)

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
        TableQuery[StorageEntryRow] += StorageEntry(None,None,"filesystem",Some("me"),None,None,None),
        TableQuery[StorageEntryRow] += StorageEntry(None,None,"omms",Some("you"),None,None,None),
        TableQuery[FileEntryRow] += FileEntry(None,"/path/to/a/video.mxf",1,"me",1,new Timestamp(12345678),new Timestamp(12345678),new Timestamp(12345678)),
        TableQuery[FileEntryRow] += FileEntry(None,"/path/to/secondtestfile",1,"tstuser",1,new Timestamp(123456789),new Timestamp(123456789),new Timestamp(123456789)),
        //"""{"name": "Premiere test template 1","projectTypeId": 1,"filepath", "storageId": 1}"""
        //"{"name":,"opensWith":"AdobePremierePro.app","targetVersion":"14.0"}"
        TableQuery[ProjectTypeRow] += ProjectType(None,"Premiere 2014 test","AdobePremierePro.app","14.0"),
        TableQuery[ProjectTypeRow] += ProjectType(None,"Cubase 7.0 test","Cubase.app","7.0"),
        TableQuery[ProjectTemplateRow] += ProjectTemplate(Some(1),"Premiere test template 1",1,"/srv/projectfiles/ProjectTemplatesDev/Premiere/premiere_template_2014.prproj",1)

      ).asTry
    )
  }

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
    dbConfig.db.close()
    result
  }
}
