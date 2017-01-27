import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.junit.runner._
import org.specs2.runner._

import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class StorageControllerSpec extends GenericControllerSpec {
  sequential
  override val componentName: String = "StorageController"
  override val uriRoot: String = "/api/storage"

  override def testParsedJsonObject(checkdata: JsLookupResult, parsed_test_json: JsValue) = {


    val object_keys = Seq("storageType","user")
    object_keys.map(key=>
      (checkdata \ key).as[String] must equalTo((parsed_test_json \ key).as[String])
    )
  }

//    (checkdata \ "storageType").as[String] must equalTo((parsed_test_json \ "storageType").as[String])
//    (checkdata \ "user").as[String] must equalTo((parsed_test_json \ "user").as[String])
//  )


  override val testGetId: Int = 1
  override val testGetDocument: String = """{"storageType": "Local", "user": "me"}"""
  override val testCreateDocument: String =  """{"storageType": "ftp", "user": "tests"}"""
  override val testDeleteId: Int = 4
  override val testConflictId: Int = 2
  override val minimumNewRecordId: Int = 3


}
