package models
import slick.driver.PostgresDriver.api._

case class GenericModel(id:Option[Int], name: String) {

}

trait GenericModelRow[M] extends Table[M] {
  def id = column[Int]("id",O.PrimaryKey)
}

//class GenericModelRow(tag:Tag) extends Table[GenericModel](tag, "GenericModel") {
//  def id=column[Int]("id",O.PrimaryKey)
//  def name=column[Int]("name")
//  def * = (id.?,name) <> (GenericModel.tupled, GenericModel.unapply)
//}