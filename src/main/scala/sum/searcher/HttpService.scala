package sum.searcher

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import sum.searcher.SumSearcher.FoundSum

import scala.util._
import scala.util.control.NonFatal

class HttpService(
    sumSearcher: SumSearcher,
    targetProvider: TargetProvider,
    throttler: Throttler
)(implicit actorSystem: ActorSystem[Nothing])
    extends SprayJsonSupport {
  import HttpService._

  private val log = LoggerFactory.getLogger(getClass())
  private implicit val ec = actorSystem.executionContext

  implicit val responseFormat: RootJsonFormat[FoundSum] = jsonFormat2(FoundSum.apply)

  private def throttleRequest: Directive0 = {
    if (throttler.shouldThrottle()) {
      complete((StatusCodes.TooManyRequests, "Too many requests"))
    } else {
      pass
    }
  }

  val route = path("find") {
    post {
      entity(as[Request]) { request =>
        throttleRequest {
          val futureResult = targetProvider
            .getTarget(request.targetOpt)
            .map(sumSearcher.findSums(request.data, _))

          onComplete(futureResult) {
            case Success(result) =>
              complete(result)
            case Failure(NonFatal(e)) =>
              log.error(s"Fail to find sum: $e")
              complete(HttpResponse(StatusCodes.InternalServerError))
            case Failure(e) =>
              log.error("Fatal error on finding sum", e)
              actorSystem.terminate()
              complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
      }
    }
  }

}

object HttpService {
  case class Request(data: Seq[Int], targetOpt: Option[Int])

  implicit val responseFormat: RootJsonFormat[Request] = jsonFormat2(Request.apply)
}
