package models

import java.io.{Reader, StringReader, StringWriter}
import java.sql.Timestamp
import java.time.Instant

import helpers.PostrunDataCache
import javax.script.{ScriptEngine, ScriptEngineManager}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration

import scala.util.Success

class PostrunActionSpec extends Specification with Mockito {
  "PostrunAction.runJS" should {
    "run a provided javascript script and return its content" in {
      val testScript =
        """
          |var postrun = function(postrunInfo, dataCache) {
          | print("Got postrun info: " + postrunInfo);
          | print("\nProject title is " + postrunInfo.get("projectTitle").get());
          | print("Data cache is " + dataCache);
          | return "Hello world";
          |}
        """.stripMargin
      val configuration = Configuration.empty

      val testAction = new PostrunAction(Some(1),"somescriptfile","Test postrun",None,"test",1,Timestamp.from(Instant.now())) {
        override def scriptSourceAsReader(implicit config:Configuration) = {
          Success(new StringReader(testScript))
        }

        def testRunJS(projectFileName:String,projectEntry:ProjectEntry,projectType:ProjectType,dataCache:PostrunDataCache,
                      workingGroupMaybe: Option[PlutoWorkingGroup], commissionMaybe: Option[PlutoCommission])
                     (implicit config:Configuration, engine:ScriptEngine) = runJS(projectFileName, projectEntry, projectType, dataCache, workingGroupMaybe, commissionMaybe)
      }

      val fakeProjectEntry = ProjectEntry(Some(2),1,None,"Some title", Timestamp.from(Instant.now()),"test",None,None,None,None,None)
      val fakeProjectType = ProjectType(Some(3),"Test type","Some.app","1.0",None,None)

      val stdout = new StringWriter()
      val stderr = new StringWriter()

      implicit val engine = new ScriptEngineManager().getEngineByMimeType("text/javascript")
      val scriptContext = engine.getContext
      scriptContext.setWriter(stdout)
      scriptContext.setErrorWriter(stderr)
      val result = testAction.testRunJS("test-project",fakeProjectEntry,fakeProjectType,mock[PostrunDataCache],None,None)(configuration, engine)

      println(s"Got stdout: ${stdout.toString}")
      println(s"Got stderr: ${stderr.toString}")

      result must beSuccessfulTry
      result.get.toString mustEqual "Hello world"
    }
  }
}
