package com.github.dnvriend

import org.scalatest.{ FlatSpec, Matchers, OptionValues }
import org.typelevel.scalatest.{ DisjunctionMatchers, ValidationMatchers }

abstract class TestSpec extends FlatSpec with Matchers with ValidationMatchers with DisjunctionMatchers with OptionValues {
  def toHex(arr: Array[Byte]): String = javax.xml.bind.DatatypeConverter.printHexBinary(arr)
  def fromHex(hex: String): Array[Byte] = javax.xml.bind.DatatypeConverter.parseHexBinary(hex)
}
