# aws-kms-test
The [AWS Key Management Service (AWS KMS)](http://docs.aws.amazon.com/kms/latest/developerguide/overview.html) is a managed service 
that makes it easy for you to create and control the encryption keys used to encrypt your data.

## Prerequisites
To use AWS KMS, you need:

- An AWS account,
- Create a Customer Master Key (CMK) in AWS KMS,
- The AWS SDK for Java (already setup in SBT)
- Download [Java Cryptography Extension (JCE) Unlimited Strength ](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)
  - go to $JAVA_HOME/jre/lib/security
  - overwrite `US_exports_policy.jar` and `local_policy.jar` from the zip
  - launch your application again

## AWS KMS API
The [AWS KMS API](http://docs.aws.amazon.com/kms/latest/developerguide/programming-top.html) allows to perform
the following actions:

- Create, describe, list, enable, and disable keys.
- Create, delete, list, and update aliases.
- Encrypt, decrypt, and re-encrypt content.
- Set, list, and retrieve key policies.
- Create, retire, revoke, and list grants.
- Retrieve key rotation status.
- Update key descriptions.
- Generate data keys with or without plaintext.
- Generate random data.

## AWS Encryption SDK
The [AWS Encryption SDK](http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/introduction.html) is an encryption library that helps make it easier for you to implement encryption best practices 
in your application. It enables you to focus on the core functionality of your application, rather than on how to best encrypt 
and decrypt your data.

There is an [AWS Encryption SDK for Java](http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/java.html) available
and is also available on [github](https://github.com/awslabs/aws-encryption-sdk-java).

The AWS Encryption SDK provides an encryption library that optionally integrates with AWS KMS as a master key provider,
`com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider`. The AWS Encryption SDK builds on the AWS SDKs to do the following things:

- Generate, encrypt, and decrypt data keys
- Use those data keys to encrypt and decrypt your raw data
- Store the encrypted data keys with the corresponding encrypted data in a single object

**Note:** You can also use the AWS Encryption SDK with no AWS integration by defining a custom master key provider.

The AWS Encryption SDK provides encryption and decryption for data that you can store anywhere. The AWS Encryption SDK 
uses the Advanced Encryption Standard (AES) algorithm. The SDK supports 256-bit, 192-bit, and 128-bit encryption keys.

When AWS KMS is the master key provider, the SDK uses the AWS KMS GenerateDataKey API operation to generate each data key 
in both plaintext and encrypted forms. It uses the Decrypt operation to decrypt the data key. AWS KMS encrypts and decrypts 
the data key by using the customer master key (CMK) that you specified when configuring the master key provider. For every
encryption invocation, the AWS Encryption SDK generates a new Data Key for the given CMK. The result of the encryption is an encrypted
message that contains the encrypted data key and the encrypted payload. The AWS Encryption SDK parses the encrypted message,
automatically reads the encrypted data key, contacts KMS with the given CMK Arn to decrypt the data key, and decrypts the payload.
For more information read: [How the AWS Encryption SDK Works](http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/how-it-works.html).

When you encrypt data, the SDK encrypts the data key and stores the encrypted key along with the encrypted data in the 
encrypted message that it returns. When you decrypt data, the AWS Encryption SDK extracts the encrypted data key from the 
encrypted method, decrypts it, and then uses it to decrypt the data. You can just store the encrypted message and just manage
access to, and the lifecycle of the CMK ARN.

The overhead that AWS Encryption SDK has over the payload, is just 1kB.

It is possible to use multiple `KmsMasterKeyProvider` by combining them into one using the 
`com.amazonaws.encryptionsdk.multi.MultipleProviderFactory` provider. Each single KmsMasterKeyProvider is initialized 
with a different CMK, eg. each key from a different region in order to decrypt the message with any of the CMK. This way
a key to decrypt is always available and the message can always be decrypted.  

The AWS Encryption SDK can encrypt raw bytes (byte arrays), I/O streams (byte streams), and Strings.

## How does the AWS Encryption SDK store encrypted data keys with their encrypted data?
The encryption operations in the AWS Encryption SDK return an encrypted message, a single data structure that contains 
the encrypted data and its encrypted data keys. The message format consists of at least two parts: a header and a body. 
In some cases, the message format consists of a third part known as a footer. The message header contains the encrypted 
data keys and information about how the message body is formed. The message body contains the encrypted data. The message 
footer contains a signature that authenticates the message header and message body. For more information, 
see [AWS Encryption SDK Message Format Reference](http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html) 
and [How the AWS Encryption SDK Works](http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/how-it-works.html)

## Concepts

## Customer Master Keys (CMK)
The primary resources in AWS KMS are customer master keys (CMKs). CMKs are either customer managed or AWS managed. 
You can use either type of CMK to protect up to 4 kilobytes (4096 bytes) of data directly. Typically you use CMKs to 
protect data encryption keys (or data keys) which are then used to encrypt or decrypt larger amounts of data outside of 
the service. CMKs never leave AWS KMS unencrypted, but data keys can. AWS KMS does **not** store, manage, or track your data keys.

## Data Keys
You use data keys to encrypt large data objects within your own application outside AWS KMS. When you make a 
GenerateDataKey API request, AWS KMS returns a plaintext copy of the data key and a ciphertext that contains the data 
key encrypted under the specified CMK. You use the **plaintext data key** in your application **to encrypt data**, 
and you typically store the encrypted data key **alongside** your encrypted data.

To decrypt data in your application, you **pass the encrypted data key with a Decrypt API request**. AWS KMS uses 
your CMK to decrypt the data key into plaintext, and then returns it to you. You use the plaintext data key to decrypt 
your data and then remove the plaintext data key from memory as soon as practical after use.

