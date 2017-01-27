import org.junit.runner._
import org.specs2.runner._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class FileControllerSpec extends GenericControllerSpec {
  override val componentName: String = "FileController"
  override val uriRoot: String = "/file"

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {
    val object_keys = Seq("filepath","user","ctime","mtime","atime")
    val object_keys_int = Seq("storage","version")

    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    ) ++ object_keys_int.map(key=>
      (checkdata \ key).as[Int] must equalTo((parsed_test_json \ key).as[Int])
    )
  }

  override val testGetId: Int = 1
  override val testGetDocument: String = """{"filepath":"/path/to/a/video.mxf","storage":2,"user":"me","version":1,"ctime":"2017-01-17T16:55:00.123+0000","mtime":"2017-01-17T16:55:00.123+0000","atime":"2017-01-17T16:55:00.123+0000"}"""
  override val testCreateDocument: String =  """{"filepath":"/path/to/some/other.project","storage":1,"user":"test","version":3,"ctime":"2017-03-17T13:51:00.123+0000","mtime":"2017-03-17T13:51:00.123+0000","atime":"2017-03-17T13:51:00.123+0000"}"""
  override val minimumNewRecordId = 4
  override val testDeleteId: Int = 2
  override val testConflictId: Int = 1
}
