package helpers

import com.google.inject.Inject
import models._
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.PostgresDriver.api._

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
        (
          TableQuery[FileAssociationRow].schema ++
            TableQuery[FileEntryRow].schema ++
            TableQuery[ProjectEntryRow].schema ++
            TableQuery[ProjectTemplateRow].schema ++
            TableQuery[ProjectTypeRow].schema ++
            TableQuery[StorageEntryRow].schema
          ).create
      ).asTry
    )
  }

  def teardownDB():Future[Try[Unit]] = {
    logger.warn("In teardownDB")
    dbConfig.db.run(
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
  }
}
