package sum.searcher

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol._
import sum.searcher.HttpService._
import sum.searcher.SumSearcher._

import scala.concurrent._

class HttpServiceSpec
    extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest
    with SprayJsonSupport
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val actorSystem = ActorSystem(Behaviors.empty, getClass().getSimpleName())

  val sumSearcherMock = mock(classOf[SumSearcher])
  val targetProviderMock = mock(classOf[TargetProvider])
  val throttlerMock = mock(classOf[Throttler])

  val testedHttpService = new HttpService(sumSearcherMock, targetProviderMock, throttlerMock)(actorSystem)
  val testedRoute = testedHttpService.route
  import testedHttpService.responseFormat

  override protected def beforeEach(): Unit = {
    when(sumSearcherMock.findSums(any[Iterable[Int]](), anyInt()))
      .thenReturn(Seq.empty)
    when(targetProviderMock.getTarget(any[Option[Int]]()))
      .thenReturn(Future.successful(-1))
    when(throttlerMock.shouldThrottle()).thenReturn(false)
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
  }

  "http service" should {
    "return found sums" when {

      "target parameter is provided" in {
        val targetParam = 456
        val target = 8
        val data = Seq(1, 2, 3)
        val expectedResult = Seq(FoundSum((1, 2), (3, 4)))
        val request = Request(data = data, targetOpt = Some(targetParam))

        when(targetProviderMock.getTarget(Some(targetParam))).thenReturn(Future.successful(target))
        when(sumSearcherMock.findSums(data, target)).thenReturn(expectedResult)

        Post(s"/find", request) ~> testedRoute ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[FoundSum]] shouldBe expectedResult
        }
      }

      "target parameter is not available" in {
        val target = 11
        val data = Seq(2, 3, 4)
        val expectedResult = Seq(FoundSum((2, 3), (4, 5)))
        val request = Request(data = data, targetOpt = None)

        when(targetProviderMock.getTarget(None)).thenReturn(Future.successful(target))
        when(sumSearcherMock.findSums(data, target)).thenReturn(expectedResult)

        Post(s"/find", request) ~> testedRoute ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[FoundSum]] shouldBe expectedResult
        }
      }
    }

    "apply throttling" in {
      val request = Request(data = Seq(1, 2), targetOpt = None)
      when(throttlerMock.shouldThrottle()).thenReturn(false)
      Post(s"/find", request) ~> testedRoute ~> check {
        status shouldBe StatusCodes.OK
      }
      when(throttlerMock.shouldThrottle()).thenReturn(true)
      Post(s"/find", request) ~> testedRoute ~> check {
        status shouldBe StatusCodes.TooManyRequests
      }
    }

    "reply with 500 on error" in {
      val request = Request(data = Seq(1, 2), targetOpt = None)
      when(targetProviderMock.getTarget(any(classOf[Option[Int]])))
        .thenReturn(Future.failed(new RuntimeException("expected exception")))

      Post(s"/find", request) ~> testedRoute ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

  }

}
