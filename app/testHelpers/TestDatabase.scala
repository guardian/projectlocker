package testHelpers

import com.google.inject.Inject
import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.{Database, Databases}
import play.api.db.evolutions._
import slick.backend.DatabaseConfig
import slick.profile.BasicProfile

/**
  * Created by localhome on 19/01/2017.
  */
object TestDatabase {
  class testDbProvider @Inject() (app:Application) extends DatabaseConfigProvider {
    def get[P <: BasicProfile]: DatabaseConfig[P] = {
      DatabaseConfigProvider.get("test")(app)
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
      Evolutions.withEvolutions(db)(block(db))
    }
  }

}
