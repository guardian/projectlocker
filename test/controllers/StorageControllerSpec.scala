package controllers

import java.io.{File, FileOutputStream}

import org.junit.runner._
import org.specs2.runner._
import org.specs2.specification.BeforeAfterAll

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class StorageControllerSpec extends GenericControllerSpec with BeforeAfterAll {
  sequential
  override val componentName: String = "StorageController"
  override val uriRoot: String = "/api/storage"

  override def beforeAll(): Unit ={
    val f = new File("/tmp/teststorage")
    if(!f.exists())
      f.mkdirs()
  }

  override def afterAll(): Unit ={
    val f = new File("/tmp/teststorage")
    if(f.exists())
      f.delete()
  }

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {
    val object_keys = Seq("storageType","user")
    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    )
  }

  override val testGetId: Int = 1
  override val testGetDocument: String = """{"storageType": "Local", "user": "me"}"""
  override val testCreateDocument: String =  """{"storageType": "Local", "user": "tests", "rootpath":"/tmp/teststorage"}"""
  override val testDeleteId: Int = 4
  override val testConflictId: Int = 2
  override val minimumNewRecordId: Int = 3
}
