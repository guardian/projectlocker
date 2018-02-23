package testHelpers

import javax.inject.Inject
import play.api.{Application, Logger}
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.{DatabaseConfig,BasicProfile}


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
