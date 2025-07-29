package sum.searcher

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future
import scala.util.control.NonFatal

trait TargetProvider {
  def getTarget(targetOpt: Option[Int]): Future[Int]
}

class TargetProviderProd(implicit actorSystem: ActorSystem[Nothing]) extends TargetProvider {
  import TargetProviderProd._

  private val log = LoggerFactory.getLogger(getClass())
  private implicit val ec = actorSystem.executionContext
  private val defaultTarget = actorSystem.settings.config.getInt("sum.searcher.default-target")
  private val targetApiUrl = actorSystem.settings.config.getString("sum.searcher.target-api-url")

  def getTarget(targetParamOpt: Option[Int]): Future[Int] = {
    val params = targetParamOpt match {
      case Some(t) => Map("target" -> t.toString)
      case None    => Map.empty[String, String]
    }
    val uri = Uri(targetApiUrl).withQuery(Uri.Query(params))
    Http()
      .singleRequest(HttpRequest(HttpMethods.GET, uri))
      .flatMap { response =>
        if (response.status.isSuccess()) {
          Unmarshal(response).to[Response].map(_.args.target.toInt)
        } else {
          log.warn("Error on getting target: {}", response)
          Future.successful(defaultTarget)
        }
      }
      .recover { case NonFatal(e) =>
        log.warn(s"Error on getting target: {}", e.toString())
        defaultTarget
      }
  }

}

object TargetProviderProd {
  case class Response(args: ResponseArgs)
  case class ResponseArgs(target: String)
  implicit val responseArgsFormat: RootJsonFormat[ResponseArgs] = jsonFormat1(ResponseArgs.apply)
  implicit val responseFormat: RootJsonFormat[Response] = jsonFormat1(Response.apply)
}
