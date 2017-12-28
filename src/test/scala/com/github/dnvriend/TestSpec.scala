package com.github.dnvriend

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement
import com.amazonaws.services.kms.AWSKMS
import org.scalatest.{ FlatSpec, Matchers, OptionValues }
import org.typelevel.scalatest.{ DisjunctionMatchers, ValidationMatchers }

abstract class TestSpec extends FlatSpec with Matchers with ValidationMatchers with DisjunctionMatchers with OptionValues {
  def toHex(arr: Array[Byte]): String = javax.xml.bind.DatatypeConverter.printHexBinary(arr)
  def fromHex(hex: String): Array[Byte] = javax.xml.bind.DatatypeConverter.parseHexBinary(hex)

  val iamClient: AmazonIdentityManagement = IamOperations.client()
  val kmsClient: AWSKMS = KmsOperations.client()
  println(IamOperations.whoAmI(iamClient))
}
