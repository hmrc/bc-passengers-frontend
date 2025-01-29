/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import scala.util.Random

object ChargeReference {

  private val ChargeRefCheckCharTable: Map[Int, Char] = Map(
    0  -> 'A',
    1  -> 'B',
    2  -> 'C',
    3  -> 'D',
    4  -> 'E',
    5  -> 'F',
    6  -> 'G',
    7  -> 'H',
    8  -> 'X',
    9  -> 'J',
    10 -> 'K',
    11 -> 'L',
    12 -> 'M',
    13 -> 'N',
    14 -> 'Y',
    15 -> 'P',
    16 -> 'Q',
    17 -> 'R',
    18 -> 'S',
    19 -> 'T',
    20 -> 'Z',
    21 -> 'V',
    22 -> 'W'
  )

  private val ChargeRefWeights: List[Int] = List(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2, 1)

  def generate: ChargeReference = {
    val randomFactor: List[Char] = Random.alphanumeric.filter(c => c.isDigit).take(10).toList
    val digits: List[Int]        = 48 :: 50 :: randomFactor.map(c => if (c.isDigit) c.asDigit else c.toInt - 32)
    val checkSum: Int            = (ChargeRefWeights zip digits).map(x => x._1 * x._2).sum % 23

    val checkChar: Char = ChargeRefCheckCharTable(checkSum)

    ChargeReference(('X' :: checkChar :: 'P' :: 'R' :: randomFactor).mkString)
  }

}
case class ChargeReference(value: String)
