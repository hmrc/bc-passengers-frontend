package models

import scala.util.Random

object ChargeReference {

  val ChargeRefCheckCharTable = Map(
    0  -> 'A', 1  -> 'B', 2  -> 'C', 3  -> 'D', 4  -> 'E', 5  -> 'F', 6  -> 'G', 7  -> 'H',
    8  -> 'X', 9  -> 'J', 10 -> 'K', 11 -> 'L', 12 -> 'M', 13 -> 'N', 14 -> 'Y', 15 -> 'P',
    16 -> 'Q', 17 -> 'R', 18 -> 'S', 19 -> 'T', 20 -> 'Z', 21 -> 'V', 22 -> 'W'
  )

  val ChargeRefWeights = List(9,10,11,12,13,8,7,6,5,4,3,2,1)

  def generate: ChargeReference = {
    val randomFactor: List[Char] = Random.alphanumeric.filter(c => c.isDigit || c.isUpper).take(10).toList
    val digits: List[Int] = 48 :: 50 :: randomFactor.map(c => if(c.isDigit) c.asDigit else c.toInt-32)
    val checkSum: Int = (ChargeRefWeights zip digits).map(x => x._1 * x._2).sum % 23

    val checkChar: Char = ChargeRefCheckCharTable(checkSum)

    ChargeReference( ('X' :: checkChar :: 'P' :: 'R' :: randomFactor).mkString )
  }

}
case class ChargeReference(value: String)

