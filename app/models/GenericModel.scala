package models
import slick.driver.PostgresDriver.api._

case class GenericModel(id:Option[Int], name: String) {

}

class GenericModelRow(tag:Tag) extends Table[GenericModel](tag, "GenericModel") {
  def id=column[Int]("id",O.PrimaryKey)
  def name=column[Int]("name")
  def * = (id.?,name) <> (GenericModel.tupled, GenericModel.unapply)
}