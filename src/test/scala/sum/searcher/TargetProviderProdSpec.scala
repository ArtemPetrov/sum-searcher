package sum.searcher

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sum.searcher.TargetProviderProd._

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class TargetProviderProdSpec extends AsyncWordSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  val testDefaultTarget = 9
  val mockHttpServerActorSystem = ActorSystem(Behaviors.empty, "mock-http-server")
  val mockHttpServer = new MockTargetHttpServer()(mockHttpServerActorSystem)

  val mockHttpServerPort = Await.result(mockHttpServer.getPort(), 1.minute)

  val testActorSystem = ActorSystem[Nothing](
    Behaviors.empty,
    name = "test-target-provider",
    config = ConfigFactory
      .parseMap(
        Map(
          "sum.searcher.target-api-url" -> s"http://localhost:$mockHttpServerPort/get",
          "sum.searcher.default-target" -> testDefaultTarget
        ).asJava
      )
      .withFallback(ConfigFactory.load())
  )

  val tested = new TargetProviderProd()(testActorSystem)

  override protected def afterEach(): Unit = {
    mockHttpServer.reset()
  }
  override protected def afterAll(): Unit = {
    mockHttpServerActorSystem.terminate()
    testActorSystem.terminate()
  }

  "target provider" should {

    "provide target when initial target is available" in {
      val expectedTarget = 120
      mockHttpServer.onRequestWithTargetReply(target => expectedTarget.toString)
      tested.getTarget(Some(20)).map { _ shouldBe 120 }
    }

    "provide target when initial target is not available" in {
      mockHttpServer.onRequestWithoutTargetReply(() => "97")
      tested.getTarget(None).map { _ shouldBe 97 }
    }

    "provide default target on error" when {
      "initial target is not available" in {
        mockHttpServer.onRequestWithoutTargetReply(() => throw new RuntimeException("expected error"))
        tested.getTarget(None).map(_ shouldBe 9)
      }
      "initial target is available" in {
        mockHttpServer.onRequestWithTargetReply(t => throw new RuntimeException(s"expected error for $t"))
        tested.getTarget(Some(123)).map(_ shouldBe testDefaultTarget)
      }
    }

  }

}

class MockTargetHttpServer(implicit actorSystem: ActorSystem[Nothing]) extends SprayJsonSupport {
  private implicit val ec = actorSystem.executionContext
  private val defaultReply: Option[String] => String = i => throw new RuntimeException("call if not mocked");
  @volatile
  private var replyWith = defaultReply

  private val rout = path("get") {
    get {
      parameters("target".as[String].?) { case target =>
        Try {
          replyWith(target)
        } match {
          case Success(r) => complete(Response(ResponseArgs(r)))
          case Failure(e) =>
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  private val binding: Future[Http.ServerBinding] = Http()
    .newServerAt("localhost", 0)
    .bind(rout)

  def onRequestWithTargetReply(fn: Option[String] => String) = replyWith = fn
  def onRequestWithoutTargetReply(fn: () => String) = replyWith = None => fn()

  def reset() = replyWith = defaultReply

  def getPort(): Future[Int] = binding.map(_.localAddress.getPort())
}
