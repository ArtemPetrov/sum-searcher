package sum.searcher

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.jdk.CollectionConverters._

class ThrottlerSpec extends AsyncWordSpec with Matchers {

  def newThrottler(querisePerMinute: Int) = {
    val config = ConfigFactory.parseMap(Map("sum.searcher.requests-per-minute" -> querisePerMinute).asJava)
    val actorSystem = ActorSystem[Nothing](Behaviors.empty, getClass().getSimpleName(), config)
    val throttler = new Throttler()(actorSystem)
    actorSystem.terminate()
    throttler
  }

  "throttler" should {
    "work" in {
      val throttler = newThrottler(querisePerMinute = 3)
      throttler.shouldThrottle() shouldBe false
      throttler.shouldThrottle() shouldBe false
      throttler.shouldThrottle() shouldBe false
      throttler.shouldThrottle() shouldBe true
    }
  }
}
