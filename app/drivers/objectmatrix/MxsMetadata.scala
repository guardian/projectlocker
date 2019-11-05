package drivers.objectmatrix

import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}

import com.om.mxs.client.japi.Attribute
import org.slf4j.LoggerFactory

case class MxsMetadata (stringValues:Map[String,String], boolValues:Map[String,Boolean], longValues:Map[String,Long], intValues:Map[String,Int]) {
  private val logger = LoggerFactory.getLogger(getClass)

  /**
    * converts the data to a Seq[com.om.mxs.client.japi.Attribute], suitable for passing to ObjectMatrix API calls
    * @return sequence of Attributes.
    */
  def toAttributes:Seq[Attribute] = {
    stringValues.map(entry=>
      Option(entry._2).map(realValue=>Attribute.createTextAttribute(entry._1,realValue,true))
    ).toSeq.collect({case Some(attrib)=>attrib}) ++
      boolValues.map(entry=>new Attribute(entry._1,entry._2,true)) ++
      longValues.map(entry=>new Attribute(entry._1, entry._2, true)) ++
      intValues.map(entry=>new Attribute(entry._1, entry._2, true))
  }

  /**
    * convenience function to set a string value. Internally calls `withValue`.
    * @param key key to set
    * @param value string value to set it to
    * @return and updated [[MxsMetadata]] object
    */
  def withString(key:String, value:String):MxsMetadata = {
    withValue[String](key,value)
  }

  /**
    * sets the given value for the given key and returns an updated [[MxsMetadata]] object.
    * if the type of `value` is not supported, then it emits a warning and returns the original [[MxsMetadata]]
    * @param key key to identify the metadata
    * @param value value to set. This must be either Boolean, String, Int, Long or ZonedDateTime. ZonedDateTime is converted
    *              to epoch millisenconds and stored as a long
    * @tparam T the data type of `value`. Normally the compiler can infer this from the argument
    * @return an updated [[MxsMetadata]] object
    */
  def withValue[T](key:String, value:T):MxsMetadata = {
    value match {
      case boolValue: Boolean => this.copy(boolValues = this.boolValues ++ Map(key -> boolValue))
      case stringValue: String => this.copy(stringValues = this.stringValues ++ Map(key -> stringValue))
      case intValue: Int => this.copy(intValues = this.intValues ++ Map(key -> intValue))
      case longValue: Long => this.copy(longValues = this.longValues ++ Map(key -> longValue))
      case timeValue: ZonedDateTime => this.copy(longValues = this.longValues ++ Map(key -> timeValue.toInstant.toEpochMilli))
      case timeValue: LocalDateTime => this.copy(longValues = this.longValues ++ Map(key -> timeValue.toInstant(ZoneOffset.UTC).toEpochMilli))
      case instant: Instant => this.copy(longValues = this.longValues ++ Map(key -> instant.toEpochMilli))
      case _ =>
        logger.warn(s"Could not set key $key to value $value (type ${value.getClass.toGenericString}), type not recognised")
        this
    }
  }

  def toSymbolMap:Map[Symbol,String] =
    stringValues.map(tuple=>(Symbol(tuple._1), tuple._2)) ++
    boolValues.map(tuple=>(Symbol(tuple._1), tuple._2.toString)) ++
      longValues.map(tuple=>(Symbol(tuple._1), tuple._2.toString)) ++
      intValues.map(tuple=>(Symbol(tuple._1), tuple._2.toString))


  def dumpString(fieldNames:Seq[String]) = {
    val kv = fieldNames.map(fieldName=>{
      val maybeString = stringValues.get(fieldName)
      val maybeBool = boolValues.get(fieldName)
      val maybeLong = longValues.get(fieldName)
      val maybeInt = intValues.get(fieldName)

      val v = if(maybeString.isDefined){
        maybeString.get
      } else if(maybeBool.isDefined){
        if(maybeBool.get){
          "true"
        } else {
          "false"
        }
      } else if(maybeLong.isDefined){
        maybeLong.get.toString
      } else if(maybeInt.isDefined){
        maybeInt.get.toString
      } else {
        "(none)"
      }

      s"$fieldName=$v"
    })

    kv.mkString(", ")
  }
}

object MxsMetadata {
  def apply(stringValues: Map[String, String], boolValues: Map[String, Boolean], longValues: Map[String, Long], intValues: Map[String, Int]): MxsMetadata = new MxsMetadata(stringValues, boolValues, longValues, intValues)

}
