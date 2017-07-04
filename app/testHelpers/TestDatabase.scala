package testHelpers

import com.google.inject.Inject
import models._
import play.api.{Application, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.{Database, Databases}
import play.api.db.evolutions._
import play.api.libs.json.Json
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.profile.BasicProfile

import scala.util.{Failure, Success}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by localhome on 19/01/2017.
  */
object TestDatabase {
  val logger: Logger = Logger(this.getClass)

  class testDbProvider @Inject() (app:Application) extends DatabaseConfigProvider {
    def get[P <: BasicProfile]: DatabaseConfig[P] = {
      DatabaseConfigProvider.get("testDB")(app)
    }
  }

  def withTestDatabase[T](block: Database => T)  = {
    Databases.withInMemory(
      name="testDB",
      urlOptions = Map(
        "MODE" -> "PostgreSQL"
      ),
      config = Map(
        "logStatements"->true
      )
    ){ db =>
       db.asInstanceOf[JdbcProfile#Backend#Database].run(
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
      ).map({
        case Success(result)=>
          logger.info("Database succesfully initialised")
        case Failure(error)=>
          logger.error(error.toString)
      })
      block(db)
    }
  }

}
