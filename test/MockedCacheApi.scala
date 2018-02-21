import org.mockito._
import org.specs2.mock.Mockito
import play.api.cache.SyncCacheApi

import scala.concurrent.duration.Duration
import scala.runtime.Nothing$

trait MockedCacheApi extends Mockito {
  val mockedSyncCacheApi = mock[SyncCacheApi]

}
