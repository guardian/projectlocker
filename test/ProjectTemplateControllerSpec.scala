import org.junit.runner._
import org.specs2.runner._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class ProjectTemplateControllerSpec extends GenericControllerSpec {
  sequential

  override val componentName: String = "ProjectTemplateController"
  override val uriRoot: String = "/template"

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {
    val object_keys = Seq("name","filepath")
    val object_keys_int = Seq("projectTypeId","storageId")

    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    ) ++ object_keys_int.map(key=>
      (checkdata \ key).as[Int] must equalTo((parsed_test_json \ key).as[Int])
    )
  }

  override val testGetId: Int = 1
  override val testGetDocument: String = """{"name": "Premiere test template 1","projectTypeId": 1,"filepath":"/srv/projectfiles/ProjectTemplatesDev/Premiere/premiere_template_2014.prproj", "storageId": 1}"""
  override val testCreateDocument: String =  """{"name": "Cubase test template 1","projectTypeId": 3,"filepath":"/srv/projectfiles/ProjectTemplatesDev/Cubase/cubase_test_template.cpr", "storageId": 1}"""
  override val minimumNewRecordId = 3
  override val testDeleteId: Int = 2
  override val testConflictId: Int = -1
}
