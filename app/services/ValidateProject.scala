package services

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.stream.{ActorMaterializer, ClosedShape, Materializer}
import akka.stream.scaladsl.{Balance, GraphDSL, Merge, RunnableGraph, Sink}
import javax.inject.{Inject, Singleton}
import models.{ProjectEntry, ProjectEntryRow}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.{Rep, TableQuery}
import streamcomponents.{ProjectSearchSource, ValidateProjectSwitch}
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ValidateProject {
  trait VPMsg

  /* public messages that are expected to be received */
  case object ValidateAllProjects

  /* public messages that will be sent in reply */
  case class ValidationSuccess(totalProjects:Int, projectCount:Int, failedProjects:Seq[ProjectEntry])
  case class ValidationError(err:Throwable)
}

@Singleton
class ValidateProject @Inject()(config:Configuration, dbConfigProvider:DatabaseConfigProvider, actorSystem:ActorSystem) extends Actor {
  import ValidateProject._
  private val logger = LoggerFactory.getLogger(getClass)

  private implicit val mat:Materializer = ActorMaterializer.create(actorSystem)

  /**
    * build a stream to perform the validation.
    * this consists of a database search as a source, a number of parallel verification threads and a folder to collect
    * up all records that failed.
    * @param parallelism
    * @param queryFunc
    * @return
    */
  def buildStream(parallelism:Int=4)(queryFunc: DBIOAction[Seq[ProjectEntry], NoStream, Nothing]) = {
    val sinkFactory = Sink.fold[Seq[ProjectEntry],ProjectEntry](Seq())((acc,entry)=>acc++Seq(entry))
    GraphDSL.create(sinkFactory) { implicit builder=> sink=>
      import akka.stream.scaladsl.GraphDSL.Implicits._
      val src = builder.add(new ProjectSearchSource(dbConfigProvider)(queryFunc))
      val distrib = builder.add(Balance[ProjectEntry](parallelism))
      val noMerge = builder.add(Merge[ProjectEntry](parallelism))
      val yesMerge = builder.add(Merge[ProjectEntry](parallelism))
      val yesSink = builder.add(Sink.ignore)

      src ~> distrib
      for(i<- 0 until parallelism){
        val switcher = builder.add(new ValidateProjectSwitch(dbConfigProvider))
        distrib.out(i) ~> switcher
        switcher.out(0) ~> yesMerge //"yes" branch
        switcher.out(1) ~> noMerge  //"no" branch
      }

      yesMerge ~> yesSink
      noMerge ~> sink
      ClosedShape
    }
  }

  /**
    * runs the validation stream with the given database query
    * @param queryFunc
    * @param parallelism
    * @return
    */
  def runStream(parallelism:Int=4)(queryFunc: DBIOAction[Seq[ProjectEntry], NoStream, Nothing]) =
    RunnableGraph.fromGraph(buildStream(parallelism)(queryFunc)).run()

  /**
    * return the total number of records matching the query
    * @param queryFunc
    * @return
    */
  def getTotalCount(queryFunc: DBIOAction[Seq[ProjectEntry], NoStream, Nothing]) = {
    val db = dbConfigProvider.get.db

    db.run(queryFunc).map(_.length)
  }

  /**
    * performs validation by checking the total count of projects matching the query and running the verification
    * stream and returns the result as a ValidationSuccess object in a Future.
    * If the operation fails, then the future fails; catch this with .recover()
    * @param parallelism
    * @param queryFunc
    * @return
    */
  def performValidation(parallelism:Int=4)(queryFunc: DBIOAction[Seq[ProjectEntry], NoStream, Nothing]) = {
    val resultFuture = for {
      c <- getTotalCount(queryFunc)
      r <- runStream()(queryFunc)
    } yield (c,r)

    resultFuture
      .map(resultTuple=>ValidationSuccess(resultTuple._1,resultTuple._2.length, resultTuple._2))
  }

  override def receive: Receive = {
    case ValidateAllProjects=>
      val originalSender = sender()

      performValidation()(TableQuery[ProjectEntryRow].result).onComplete({
        case Failure(err)=>
          logger.error(s"Projects validation failed: $err")
          originalSender ! ValidationError(err)

        case Success(result)=>
          logger.info(s"Project validation completed: $result")
          originalSender ! result

      })
  }
}
