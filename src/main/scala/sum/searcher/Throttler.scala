package sum.searcher

import akka.actor.typed.ActorSystem
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration._

class Throttler(implicit actorSystem: ActorSystem[Nothing]) {

  private val log = LoggerFactory.getLogger(getClass())
  private val requestsPerMinute = actorSystem.settings.config.getInt("sum.searcher.requests-per-minute")
  private implicit val ec = actorSystem.executionContext
  private val counter = new AtomicLong()

  actorSystem.scheduler.scheduleAtFixedRate(0.minutes, 1.minute) { () =>
    log.info("resetting throttling counter")
    counter.set(0)
  }

  def shouldThrottle(): Boolean = {
    val count = counter.getAndIncrement()
    count >= requestsPerMinute
  }
}
