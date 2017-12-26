package com.github.dnvriend

import com.amazonaws.encryptionsdk.{ AwsCrypto, CryptoResult }
import com.amazonaws.encryptionsdk.kms.{ KmsMasterKey, KmsMasterKeyProvider }

import scala.collection.JavaConverters._

class EncryptDataTest extends TestSpec {
  val KeyArn = "arn:aws:kms:eu-west-1:123456789:key/88ba2f35-6d06-4cf8-8bd8-75e7157ee58e"
  val PlainKeyHex = "74E9A9D1C798F0B26CCFE5036129AB4E6D86C6A1EFFDA39F68E53714D61AF8C1"
  val CipherKeyHex = "0102030078C86E258E568AE2CBF07D27F042FF241FD78953C8C722BACBCEC878095A0982EA01DF79CB675A279B7E09D4B654123F53780000007E307C06092A864886F70D010706A06F306D020100306806092A864886F70D010701301E060960864801650304012E3011040CA569E26080E4E2AE934604A8020110803BF48CC7D1DAE450E7C3D2649ED6C94A5A00ED705784F97BFBACA2CC503F8C300CF3B8BCB4A66E8025C0691A8914F38330B46B3FEE630AB321607E94"

  val text = "HelloWorld"
  val plainText: Array[Byte] = text.getBytes("UTF-8")

  it should "encrypt / decrypt data" in {
    val crypto = new AwsCrypto()
    val prov = new KmsMasterKeyProvider(KeyArn)

    // encrypt
    val context = Map("Example" -> "String").asJava
    val ciphertext: Array[Byte] = crypto.encryptData(prov, plainText, context).getResult()
    println(toHex(ciphertext))

    // decrypt
    val result: CryptoResult[Array[Byte], KmsMasterKey] = crypto.decryptData(prov, ciphertext)

    // Before returning the plaintext, verify that the customer master key that
    // was used in the encryption operation was the one supplied to the master key provider.
    result.getMasterKeyIds.asScala.head shouldBe KeyArn

    // Also, verify that the encryption context in the result contains the
    // encryption context supplied to the encryptData method. Because the
    // SDK can add values to the encryption context, don't require that
    // the entire context matches.
    println(result.getEncryptionContext.asScala)

    // Now we can return the plaintext data
    val decrypted: Array[Byte] = result.getResult
    val decryptedText = new String(decrypted)
    decryptedText shouldBe text
  }
}
