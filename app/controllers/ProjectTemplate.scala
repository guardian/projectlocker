package controllers

import com.google.inject.Inject
import models.{ProjectTemplateSerializer, StorageEntry, StorageSerializer}
import play.api.Configuration
import play.api.mvc._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}
import slick.driver.JdbcProfile

//class ProjectTemplate @Inject() (config: Configuration, dbConfigProvider: DatabaseConfigProvider)
//  extends GenericDatabaseObjectController with ProjectTemplateSerializer with StorageSerializer{
//
//  val dbConfig = dbConfigProvider.get[JdbcProfile]
//
//  override def list = ???
//
//  override def create = ???
//
//  override def update(id: Int) = ???
//
//  override def delete(id: Int) = ???
//}
