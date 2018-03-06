package helpers
import java.io._

import collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object DirectoryScanner {
  /**
    * Traverses a sequence of a Try of type A and returns either a Right with all results if they all succeeded or a Left
    * with all of the errors if any failed.
    * https://stackoverflow.com/questions/15495678/flatten-scala-try
    * @param xs - sequence to traverse
    * @tparam A - type of sequence xs
    * @return either Left containing a sequence of Throwable or Right containing sequence of A
    */
  protected def collectFailures[A](xs:Seq[Try[A]]):Either[Seq[Throwable],Seq[A]] =
    Try(Right(xs.map(_.get))).getOrElse(Left(xs.collect({case Failure(err)=>err})))

  def scanAll(dir:String):Future[Try[Seq[File]]] = scanAll(new File(dir))

  def scanAll(dir:File):Future[Try[Seq[File]]] = Future {
    Try { dir.listFiles().toSeq } match {
      case Success(allFiles)=>Success(allFiles.filter(_.isFile))
      case Failure(error)=>Failure(error)
    }
  }
}
