package sum.searcher

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Try

object App {
  private val log = LoggerFactory.getLogger(getClass())

  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "sum-searcher")
    implicit val es = actorSystem.executionContext
    Try {
      run.onComplete(_.failed.foreach(onInitFailure))
      StdIn.readLine()
    }.failed.foreach(onInitFailure)
  }

  def run(implicit actorSystem: ActorSystem[Nothing]): Future[Unit] = {
    implicit val ec = actorSystem.executionContext
    val sumSearcher: SumSearcher = new SumSearcherProd()
    val targetProvider: TargetProvider = new TargetProviderProd()
    val throttler = new Throttler()
    val httpService = new HttpService(sumSearcher, targetProvider, throttler)

    val bindingFuture = Http()
      .newServerAt("0.0.0.0", 8080)
      .bind(httpService.route)
      .map { serverBinding =>
        log.info(s"Server started: ${serverBinding.localAddress}")
        val cs = CoordinatedShutdown(actorSystem)
        cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "service-unbind") { () =>
          log.info("Shutting down")
          Future.successful(Done)
        }
      }

    bindingFuture
  }

  private def onInitFailure(e: Throwable)(implicit actorSystem: ActorSystem[Nothing]): Unit = {
    log.error(s"Error starting ${actorSystem.name}", e)
    actorSystem.terminate()
  }
}
