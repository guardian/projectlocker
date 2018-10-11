package utils

import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import play.api.cache.SyncCacheApi
import play.api.cache.ehcache.EhCacheModule
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.json.Json
import services.actors.ProjectCreationActor
import testHelpers.TestDatabase

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait BuildMyApp extends MockedCacheApi {
  def buildApp = new GuiceApplicationBuilder().disable(classOf[EhCacheModule])
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .overrides(bind[SyncCacheApi].toInstance(mockedSyncCacheApi))
    .build

  def buildAppWithMockedProjectHelper = new GuiceApplicationBuilder().disable(classOf[EhCacheModule])
    .overrides(bind[DatabaseConfigProvider].to[TestDatabase.testDbProvider])
    .overrides(bind[SyncCacheApi].toInstance(mockedSyncCacheApi))
    .build

  def bodyAsJsonFuture(response:Future[play.api.mvc.Result])(implicit materializer:ActorMaterializer) = response.flatMap(result=>
    result.body.consumeData.map(contentBytes=> {
      Json.parse(contentBytes.decodeString("UTF-8"))
    }
    )
  )
}
