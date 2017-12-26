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



