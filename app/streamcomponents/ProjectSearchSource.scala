package streamcomponents

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{AbstractOutHandler, GraphStage, GraphStageLogic}
import models.{ProjectEntry, ProjectEntryRow}
import org.slf4j.LoggerFactory
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile
import slick.lifted.{AbstractTable, TableQuery}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

class ProjectSearchSource[E <:AbstractTable[_]](dbConfigProvider:DatabaseConfigProvider, pageSize:Int=100)(queryFunc: TableQuery[ProjectEntryRow]) extends GraphStage[SourceShape[ProjectEntry]]{
  private final val out:Outlet[ProjectEntry] = Outlet.create("ProjectSearchSource.out")

  override def shape = SourceShape.of(out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    private val logger = LoggerFactory.getLogger(getClass)
    private val dbConfig = dbConfigProvider.get[PostgresProfile]
    private var cache:List[ProjectEntry] = List()
    private var resultCounter = 0

    setHandler(out, new AbstractOutHandler(){
      override def onPull() = {
        val nextResultCb = createAsyncCallback[ProjectEntry](entry=>push(out,entry))
        val failureCb = createAsyncCallback[Throwable](err=>fail(out, err))
        val completionCb = createAsyncCallback[Unit](_=>complete(out))

        if(cache.isEmpty) { //cache is empty, we need to pull more results from the database
          logger.debug("empty cache, fetching more results")
          dbConfig.db.run(queryFunc.drop(resultCounter).take(pageSize).result).map(results=>{
            logger.debug(s"ProjectEntry search returned ${results.length} more items")
            resultCounter+=results.length

            cache = results.toList
            if(cache.isEmpty){
              logger.debug("cache is still empty, assuming we got everything")
              //if the cache is still empty then we have iterated everything.
              completionCb.invoke(() )
            } else {
              val nextResult = cache.head
              logger.debug(s"pushing next result $nextResult")
              cache = cache.tail
              nextResultCb.invoke(nextResult)
            }
          }).recover({
            case err:Throwable=>
              logger.error(s"Could not perform ProjectEntry search: ", err)
              failureCb.invoke(err)
          })
        } else {            //we have results in the cache, don't serve from the database
          logger.debug("cache is not empty, serving from cache")
          val nextResult = cache.head
          cache = cache.tail
          push(out,nextResult)
        }
      }
    })
  }
}
