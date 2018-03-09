package helpers

import org.python.core._
import play.api.Logger

import collection.JavaConverters._
import scala.collection.immutable.HashMap

class PostrunDataCache(entries:PyDictionary) {
  private val logger = Logger(getClass)

  /**
    * appends a string->string map of entries to the data cache and returns a new cache instance with them in it
    * @param values values to append
    * @return new PostrunDataCache
    */
  def ++(values:Map[String,String]):PostrunDataCache = {
    val pythonifiedValues = values.map(kvTuple=>(new PyString(kvTuple._1), new PyString(kvTuple._2)))

    val newDict = entries.copy()
    newDict.putAll(pythonifiedValues.asJava)
    new PostrunDataCache(newDict)
  }

  def ++(values:PyObject):PostrunDataCache = {
    logger.debug(s"++: got values ${values}")

    if(values==null || values==Py.None || !values.isMappingType){
      this
    } else {
      val newDict = entries.copy()
      newDict.update(values.asInstanceOf[PyDictionary])
      new PostrunDataCache(newDict)
    }
  }

  /**
    * Retrieve a value from the data cache, as a string. The internally held python object is converted back to a Scala
    * string in the process
    * @param key key to check
    * @return An Option with None if no value exists or Some(string) if it does
    */
  def get(key:String):Option[String] = {
    logger.debug(s"asking for ${(new PyString(key)).toString}")
    logger.debug(s"entries are ${entries.toString}")
    val pythonValue = entries.get(new PyString(key))
    logger.debug(s"Got python value $pythonValue")
    if(pythonValue==null || pythonValue==Py.None)
      None
    else
      Some(pythonValue.asString())
  }

  /**
    * Convert the contents back into a Scala map
    * @return a Map[String,String] of the cache contents
    */
  def asScala:Map[String,String] = {
    entries.getMap.asScala.map(kvTuple=>(kvTuple._1.asString(), kvTuple._2.asString())).toMap
  }

  /**
    * Convert to python compatible dict
    */
  def asPython:PyDictionary = entries
}

object PostrunDataCache {
  def apply():PostrunDataCache = {
    new PostrunDataCache(new PyDictionary())
  }

  def apply(entries: Map[String,String]): PostrunDataCache = {
    val pythonifiedEntries = entries.map(kvTuple=>(
      new PyString(kvTuple._1).asInstanceOf[PyObject],
      new PyString(kvTuple._2).asInstanceOf[PyObject])
    )

    new PostrunDataCache(new PyDictionary(pythonifiedEntries.asJava))
  }
}