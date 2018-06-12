package utils

import org.specs2.mock.Mockito
import play.api.cache.SyncCacheApi

trait MockedCacheApi extends Mockito {
  val mockedSyncCacheApi = mock[SyncCacheApi]
  /*the hell with it, I can't make this work. Idea is to inject the Administrator value into the user's groups when under
  test, so you don't have to rely on external environment to make testing of admin functions work.
   */
//  mockedSyncCacheApi.getOrElseUpdate[Option[List[String]]]("userRoles.testuser",Duration.Inf)(any) responds { input=>
//    println(s"mocked sync cache api: $input")
//    Some(List("Administrator"))
//  }

}
