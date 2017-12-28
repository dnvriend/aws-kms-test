package com.github.dnvriend

import java.nio.ByteBuffer

import com.amazonaws.services.kms.{ AWSKMS, AWSKMSClientBuilder }
import com.amazonaws.services.kms.model._

import scala.collection.JavaConverters._

object KmsOperations {
  def client(): AWSKMS = {
    AWSKMSClientBuilder.defaultClient()
  }

  def createCmk(description: String, projectName: String, projectOrganization: String, client: AWSKMS): CreateKeyResult = {
    client.createKey(new CreateKeyRequest()
      .withDescription(description)
      .withTags(
        new Tag().withTagKey("project:name").withTagValue(projectName),
        new Tag().withTagKey("project:organization").withTagValue(projectOrganization)
      )
    )
  }

  /**
   * Returns a data encryption key that you can use in your application to encrypt data locally.
   */
  def createDataKey(keyId: String, client: AWSKMS): GenerateDataKeyResult = {
    client.generateDataKey(new GenerateDataKeyRequest()
      .withKeyId(keyId)
      .withKeySpec(DataKeySpec.AES_256))
  }

  def describeKey(keyId: String, client: AWSKMS): DescribeKeyResult = {
    client.describeKey(new DescribeKeyRequest().withKeyId(keyId))
  }

  /**
   * Decrypts ciphertext.
   */
  def decryptCiphertextBlob(ciphertextBlob: Array[Byte], client: AWSKMS): DecryptResult = {
    client.decrypt(new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(ciphertextBlob)))
  }

  def enableCmk(keyId: String, client: AWSKMS): EnableKeyResult = {
    client.enableKey(new EnableKeyRequest().withKeyId(keyId))
  }

  def disableCmk(keyId: String, client: AWSKMS): DisableKeyResult = {
    client.disableKey(new DisableKeyRequest().withKeyId(keyId))
  }

  def deleteKey(keyId: String, client: AWSKMS): ScheduleKeyDeletionResult = {
    client.scheduleKeyDeletion(new ScheduleKeyDeletionRequest().withKeyId(keyId))
  }

  def list(client: AWSKMS): List[KeyListEntry] = {
    client.listKeys().getKeys.asScala.toList
  }

  def findKeyByArn(keyArn: String, client: AWSKMS): Option[KeyListEntry] = {
    list(client).find(_.getKeyArn == keyArn)
  }

  def findKeyIdByArn(keyArn: String, client: AWSKMS): Option[String] = {
    findKeyByArn(keyArn, client).map(_.getKeyId)
  }
}