import org.mockito._
import org.specs2.mock.Mockito
import play.api.cache.SyncCacheApi

import scala.concurrent.duration.Duration
import scala.runtime.Nothing$

trait MockedCacheApi extends Mockito {
  val mockedSyncCacheApi = mock[SyncCacheApi]

//  mockedSyncCacheApi.getOrElseUpdate[Option[String]](anyString,any[Duration])(any[Option[String]]) returns None
//  mockedSyncCacheApi.getOrElseUpdate[Option[List[String]]](anyString,any[Duration])(any[Option[List[String]]]) returns None

}
