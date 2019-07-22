package streamcomponents

import akka.stream.{Attributes, Inlet, Outlet, UniformFanOutShape}
import akka.stream.stage.{AbstractInHandler, AbstractOutHandler, GraphStage, GraphStageLogic}
import com.google.inject.Inject
import models.ProjectEntry
import org.slf4j.{LoggerFactory, MDC}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * pushes the incoming ProjectEntry to "yes" if it exists in the location expected or "no" if it does not.
  */
class ValidateProjectSwitch @Inject()(dbConfigProvider:DatabaseConfigProvider) extends GraphStage[UniformFanOutShape[ProjectEntry,ProjectEntry]]{
  private val in:Inlet[ProjectEntry] = Inlet.create("ValidateProjectSwitch.in")
  private val yes:Outlet[ProjectEntry] = Outlet.create("ValidateProjectSwitch.yes")
  private val no:Outlet[ProjectEntry] = Outlet.create("ValidateProjectSwitch.no")

  override def shape: UniformFanOutShape[ProjectEntry, ProjectEntry] = UniformFanOutShape(in,yes,no)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private val logger = LoggerFactory.getLogger(getClass)
    private implicit val dbConfig = dbConfigProvider.get[PostgresProfile]
    private implicit val db = dbConfig.db

    setHandler(in, new AbstractInHandler {
      override def onPush(): Unit = {
        val yesCb = createAsyncCallback[ProjectEntry](entry=>push(yes,entry))
        val noCb = createAsyncCallback[ProjectEntry](entry=>push(no, entry))
        val errorCb = createAsyncCallback[Throwable](err=>failStage(err))

        val elem = grab(in)

        elem.associatedFiles.map(entries=>{
          Future.sequence(entries.map(_.validatePathExists)).map(lookups=>{
            val failures = lookups.collect({case Left(err)=>err})
            if(failures.nonEmpty){
              logger.error(s"Received ${failures.length} errors looking up associated files for ${elem.id} (${elem.projectTitle}): ")
              MDC.put("errors",failures.toString())
              failures.foreach(err=>logger.error(s"\t$err"))
              errorCb.invoke(new RuntimeException(s"Received ${failures.length} errors looking up associated files for ${elem.id} (${elem.projectTitle}), please consult log"))
            } else {
              val notexist = lookups.collect({case Right(result)=>result}).filter(_==false)
              if(notexist.nonEmpty){
                logger.warn(s"Project ${elem.id} (${elem.projectTitle}) is missing ${notexist.length} files out of ${lookups.length}!")
                noCb.invoke(elem)
              } else {
                yesCb.invoke(elem)
              }
            }
          })
        })
      }
    })

    setHandler(yes, new AbstractOutHandler {
      override def onPull(): Unit = if(!hasBeenPulled(in)) pull(in)
    })

    setHandler(no, new AbstractOutHandler {
      override def onPull(): Unit = if(!hasBeenPulled(in)) pull(in)
    })
  }
}
