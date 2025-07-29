package sum.searcher

trait SumSearcher {
  import SumSearcher._

  def findSums(data: Iterable[Value], target: Int): Iterable[FoundSum]
}

object SumSearcher {
  type Index = Int
  type Value = Int

  case class FoundSum(indexes: (Index, Index), values: (Value, Value))
}

class SumSearcherProd extends SumSearcher {
  import SumSearcher._

  def findSums(data: Iterable[Value], target: Int): Iterable[FoundSum] = {
    var valueToIndexes = Map.empty[Value, List[Index]]
    var result = List.empty[FoundSum]

    data.zipWithIndex.map { case (value, index) =>
      val missingPairValue = target - value
      val missingPairIndexes = valueToIndexes.getOrElse(missingPairValue, Nil)

      missingPairIndexes match {
        case missingPairIndex :: tail =>
          valueToIndexes = valueToIndexes + (missingPairValue -> tail)
          val foundSum = FoundSum(
            indexes = (missingPairIndex, index),
            values = (missingPairValue, value)
          )
          result = foundSum :: result
        case Nil =>
          val newIndexes = (index :: valueToIndexes.getOrElse(value, List.empty))
          val valueToIndexesEntry = (value -> newIndexes)
          valueToIndexes = valueToIndexes + valueToIndexesEntry
      }
    }
    result
  }

}
