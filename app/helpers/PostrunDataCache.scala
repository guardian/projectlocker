package helpers

import org.python.core.{PyDictionary, PyNone, PyObject, PyString}

import collection.JavaConverters._

class PostrunDataCache(entries:PyDictionary) {
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

  /**
    * Retrieve a value from the data cache, as a string. The internally held python object is converted back to a Scala
    * string in the process
    * @param key key to check
    * @return An Option with None if no value exists or Some(string) if it does
    */
  def get(key:String):Option[String] = {
    val pythonValue = entries.get(new PyString(key))
    if(pythonValue==null)
      None
    else
      Some(pythonValue.asString())
  }

  /**
    * Convert the contents back into a Scala map
    * @return a Map[String,String] of the cache contents
    */
  def asScala:Map[String,String] = {
    entries.getMap.asScala.map(kvTuple=>(kvTuple._1.asString(), kvTuple._2.asString())).asInstanceOf[Map[String,String]]
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