package testHelpers

import java.sql.SQLTransientConnectionException

import javax.inject.Inject
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
import org.postgresql.util.PSQLException

/**
  * Created by localhome on 19/01/2017.
  */
object TestDatabase {
  val logger: Logger = Logger(this.getClass)

  class testDbProvider @Inject() (app:Application) extends DatabaseConfigProvider {
    def get[P <: BasicProfile]: DatabaseConfig[P] = {

      DatabaseConfigProvider.get("test")(app)

    }
  }

}
