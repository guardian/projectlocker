import java.io.File

import helpers.PostrunDataCache
import org.apache.commons.io.FileUtils
import org.specs2.mutable.Specification
import postrun.UpdatePremiereScratchpaths

import scala.concurrent.Await
import scala.concurrent.duration._

class UpdatePremiereScratchpathsSpec extends Specification {
  "UpdatePremiereScratchpaths.postrun" should {
    "correctly read in and update a gzipped xml file" in {
      FileUtils.copyFileToDirectory(new File("postrun/tests/data/blank_premiere_2017.prproj"), new File("/tmp"))

      val dataCache = PostrunDataCache(Map("created_asset_folder"->"/path/to/my/assets"))
      val s = new UpdatePremiereScratchpaths
      val result = Await.result(s.postrun("/tmp/blank_premiere_2017.prproj","",dataCache),10 seconds)
      result must beSuccessfulTry
    }
  }
}
