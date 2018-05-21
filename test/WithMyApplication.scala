import org.specs2.execute.{AsResult, Result}
import play.api.test.WithApplication

abstract class WithMyApplication extends WithApplication {
  override def around[T](t: => T)(implicit evidence$2: AsResult[T]): Result = super.around {

    t
  }
}
