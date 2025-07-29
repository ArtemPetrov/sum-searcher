package sum.searcher

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SumSearcherProdSpec extends AnyWordSpec with Matchers {
  import SumSearcher._
  val tested = new SumSearcherProd

  "sum searcher" should {

    "find one sum" in {
      tested.findSums(Seq(3, 8, 10, 14), 18) shouldBe Seq(
        FoundSum((1, 2), (8, 10))
      )
      tested.findSums(Seq(3, 1, 6), 7) shouldBe Seq(FoundSum((1, 2), (1, 6)))
      tested.findSums(Seq(3, 3), 6) shouldBe Seq(FoundSum((0, 1), (3, 3)))
    }

    "find no sum" in {
      tested.findSums(Seq(3, 3), 7) shouldBe Nil
      tested.findSums(Seq(1, 2, 3), 7) shouldBe Nil
    }

    "find multiple sums" in {
      tested.findSums(Seq(3, 1, 6, 4), 7) should contain theSameElementsAs Seq(
        FoundSum((0, 3), (3, 4)),
        FoundSum((1, 2), (1, 6))
      )
      tested.findSums(Seq(3, 3, 4, 4), 7) should contain theSameElementsAs Seq(
        FoundSum((1, 2), (3, 4)),
        FoundSum((0, 3), (3, 4))
      )
    }

  }
}
