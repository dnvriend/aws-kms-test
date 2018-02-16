package com.github.dnvriend

import com.amazonaws.encryptionsdk._
import com.amazonaws.encryptionsdk.kms.{ KmsMasterKey, KmsMasterKeyProvider }
import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory

import scala.collection.JavaConverters._

class EncryptDataTest extends TestSpec {
  // All CMKs must be enabled and not pending deletion else an error will occur
  // eg. com.amazonaws.services.kms.model.DisabledException -> when one of the keys is disabled
  // or. com.amazonaws.services.kms.model.KMSInvalidStateException -> when deleting the key
  // Customer Master Key (CMK)
  val KeyArn1 = "arn:aws:kms:eu-west-1:015242279314:key/04a8c913-9c2b-42e8-a4b5-1bd2beccc3f2"
  val KeyArn2 = "arn:aws:kms:eu-west-2:015242279314:key/8272df61-67ce-42ec-b3b3-a8f2e080ed4b"

  val text = "HelloWorld"
  val plainText: Array[Byte] = text.getBytes("UTF-8")
  val decodedText: String = new String(plainText, "UTF-8")

  // Customer Master Key (CMK) => ARN: aws managed key => no direct access to key
  // Function: "Request for Encryption Key"
  // CMK => Encryption Key: ("encrypted encryption-key":"plain text encryption key")
  // Function: "Request for Decryption"
  // (CMK, Encrypted Encryption Key) => plain text encryption key

  // encrypt
  // byte-array => byte-array
  // plain text => cipher text
  // (plain text, plain text encryption key) => cipher text
  // [cipher text][encrypted encryption-key]

  // decrypt
  // byte-array => byte-array
  // cipher text => plain text
  // (cipher text, key) => plain text

  it should "encrypt / decrypt data" in {
    // instantiate the AWS Encryption SDK
    val crypto = new AwsCrypto()
    // register a provider (can be multiple) but can also be a single one.
    val prov1: MasterKeyProvider[KmsMasterKey] = new KmsMasterKeyProvider(KeyArn1)
    val prov2: MasterKeyProvider[KmsMasterKey] = new KmsMasterKeyProvider(KeyArn2)
    // when using multiple providers, you need the 'MultipleProviderFactory' to append them together
    // the resulting encrypted message can be decrypted using any provider and thus any KeyArn registered
    // eg. use a CMK from three regions to provide high available encryption
    val multi = MultipleProviderFactory.buildMultiProvider(prov1, prov2).asInstanceOf[MasterKeyProvider[KmsMasterKey]]

    // encrypt
    // the EncryptedMessage is a data structure that contains the encrypted data (ciphertext)
    // and all encrypted data keys,
    val encryptedMessage: Array[Byte] = crypto.encryptData(multi, plainText).getResult()

    // decrypt
    val result: CryptoResult[Array[Byte], KmsMasterKey] = crypto.decryptData(multi, encryptedMessage)

    // Before returning the plaintext, verify that the customer master key that
    // was used in the encryption operation was the one supplied to the master key provider.
    println(result.getMasterKeyIds.asScala)

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