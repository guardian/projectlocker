package helpers

import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

@Singleton
class DatabaseHelper @Inject() (dbConfigProvider:DatabaseConfigProvider) {
  def healthcheck:Future[Try[Unit]] = {
    val db = dbConfigProvider.get[PostgresProfile].db

    val query = DBIO.seq(sql"SELECT 1".as[Int])

    db.run(query.asTry)
  }
}
