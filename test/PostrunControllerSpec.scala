import org.junit.runner._
import org.specs2.runner._

import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class PostrunControllerSpec extends GenericControllerSpec {
  sequential

  override val componentName: String = "PostrunController"
  override val uriRoot: String = "/api/postrun"

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {
    val object_keys = Seq("runnable", "title","owner")

    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    )
  }

  override val testGetId: Int = 1
  override val testGetDocument: String = """{"id":1,"runnable":"FirstTestScript.py","title":"First test postrun","owner":"system","ctime":"2018-01-01T12:13:24.000"}"""
  override val testCreateDocument: String =  """{"runnable":"AnotherTestScript.py","title":"Another test script","owner":"test","version":1,"ctime":"2018-02-03T04:05:06.789Z"}"""
  override val minimumNewRecordId = 3
  override val testDeleteId: Int = 2
  override val testConflictId: Int = 1
}
