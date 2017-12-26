import IamOperations.{CredentialsProfileAndRegion, CredentialsRegionAndUser}
import com.amazonaws.auth._
import com.amazonaws.profile.path.cred.CredentialsDefaultLocationProvider
import com.amazonaws.regions.{DefaultAwsRegionProviderChain, Regions}
import com.amazonaws.services.identitymanagement.model.User
import com.amazonaws.services.identitymanagement.{AmazonIdentityManagement, AmazonIdentityManagementClientBuilder}
import com.amazonaws.services.kinesis.model.{CreateStreamResult, DeleteStreamResult, StreamDescription}
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.amazonaws.services.kms.model._
import com.amazonaws.services.kms.{AWSKMS, AWSKMSClientBuilder}
import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._

import scala.collection.JavaConverters._
import scala.collection.immutable
import scalaz.Disjunction
import scalaz.Scalaz._

trait AwsPluginKeys {
  // aws clients
  lazy val clientIam = SettingKey[AmazonIdentityManagement]("Returns the amazon identity and access management (IAM client")
  lazy val clientKinesis = SettingKey[AmazonKinesis]("Returns the kinesis client")
  lazy val clientKMS = SettingKey[AWSKMS]("Returns the KMS client")

  // iam tasks
  lazy val whoAmI = taskKey[Unit]("Shows the current configuration and settings")
  lazy val credsAndUser = taskKey[Disjunction[String, CredentialsRegionAndUser]]("Returns the current user")
  lazy val iamUsers = taskKey[List[User]]("Returns all the users in the account")
  lazy val iamShowUser = taskKey[Unit]("Shows the current user")
  lazy val iamShowUsers = taskKey[Unit]("Shows all the users in the account")

  // kinesis tasks
  lazy val kinesisListStreams = taskKey[List[String]]("Returns the names of the streams associated with the account")
  lazy val kinesisDescribeStream = inputKey[Unit]("Describes a stream")
  lazy val kinesisCreateStream = inputKey[Unit]("Creates a kinesis stream")
  lazy val kinesisDeleteStream = inputKey[Unit]("Deletes a kinesis stream")

  // cmk
  lazy val kmsCreateCmk = taskKey[CreateKeyResult]("Create a KMS CMK")
  lazy val kmsCreateDataKey = inputKey[Unit]("Create a KMS Data Key")
  lazy val kmsDescribeKey = inputKey[Unit]("Describe a KMS key")
  lazy val kmsDeleteKey = inputKey[Unit]("Delete a KMS CMK")
  lazy val kmsListKeys = taskKey[Unit]("List all keys")
}
object AwsPluginKeys extends AwsPluginKeys

object AwsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  val autoImport = AwsPluginKeys
  import autoImport._


  override def projectSettings = Seq(
    // aws clients
    clientKinesis := KinesisOperations.client(),
    clientIam := IamOperations.client(),
    clientKMS := KMSOperations.client(),
    // kinesis tasks
    kinesisListStreams := KinesisOperations.listStreams(clientKinesis.value),
    kinesisListStreams := (kinesisListStreams keepAs kinesisListStreams).value,
    kinesisDescribeStream := {
      val streamName = Defaults.getForParser(kinesisListStreams)((state, functions) => {
        val strings = functions.getOrElse(Nil)
        Space ~> StringBasic.examples(strings: _*)
      }).parsed
      val log = streams.value.log
      val description = KinesisOperations.describeStream(streamName, clientKinesis.value)
      log.info(
        s"""
           |Stream Description
           |====================================
           |StreamName: ${description.getStreamName}
           |StreamStatus: ${description.getStreamStatus}
           |StreamArn:  ${description.getStreamARN}
           |KeyId: ${description.getKeyId}
         """.stripMargin)
    },

    kinesisCreateStream := {
      val log = streams.value.log
      val streamName = Def.spaceDelimited("stream-name").examples(" " + name.value).parsed
      KinesisOperations.createStream(streamName.head, clientKinesis.value)
      log.info(s"Created the stream: $streamName")
    },

    kinesisDeleteStream := {
      val log = streams.value.log
      val streamName = Defaults.getForParser(kinesisListStreams)((state, functions) => {
        val strings = functions.getOrElse(Nil)
        Space ~> StringBasic.examples(strings: _*)
      }).parsed
      val input = UserInput.readInput("Are you sure? (y/n)")
      require(input == "y", "canceled deleting stream")
      KinesisOperations.deleteStream(streamName, clientKinesis.value)
      log.info(s"Deleting the stream: $streamName")
    },

    credsAndUser := IamOperations.getAwsCredentialsAndUser(clientIam.value),
    credsAndUser := (credsAndUser keepAs credsAndUser).value,

    iamShowUser := {
      val log = streams.value.log
      val credsOrError = credsAndUser.value
      credsOrError.foreach {
        case CredentialsRegionAndUser(CredentialsProfileAndRegion(creds, profileLocation, region), iamUser) =>
          log.info(
            s"""
               |===================================
               |Using the following AWS credentials
               |===================================
               |* Profile File Location: ${profileLocation.absolutePath}
               |* Region: '$region'
               |* IAM User:
               |  - UserName: '${iamUser.getUserName}'
               |  - Arn: '${iamUser.getArn}'
               |  - Created on: '${iamUser.getCreateDate}'
               |  - Last used on: '${iamUser.getPasswordLastUsed}'
               |* Credentials:
               |  - AWSAccessKeyId: '${creds.getAWSAccessKeyId}'
               |  - AWSSecretKey: '${creds.getAWSSecretKey}'
      """.stripMargin)
      }
      credsOrError.swap.foreach(err => log.error(err))
    },
    whoAmI := iamShowUser.value,

    iamUsers := IamOperations.listUsers(clientIam.value),
    iamUsers := (iamUsers keepAs iamUsers).value,
    iamShowUsers := {
      val log = streams.value.log
      val users = iamUsers.value
      users.foreach(user => log.info(s"* ${user.getUserName} - ${user.getArn} - LastUsed: ${user.getPasswordLastUsed}"))
    },

    kmsCreateCmk := {
      val projectName: String = name.value
      val projectOrganization: String = organization.value
      val projectVersion: String = version.value
      val keyDescription: String = s"$organization.$projectName-$projectVersion"
      val kmsClient = clientKMS.value
      val result = KMSOperations.createCmk(keyDescription, kmsClient)
      println(
        s"""
          |KeyId: ${result.getKeyMetadata.getKeyId}
          |Arn: ${result.getKeyMetadata.getArn}
          |Enabled: ${result.getKeyMetadata.getEnabled}
          |KeyState: ${result.getKeyMetadata.getKeyState}
          |ValidTo: ${result.getKeyMetadata.getValidTo}
          |ExpirationModel: ${result.getKeyMetadata.getExpirationModel}
        """.stripMargin)
      result
    },
    kmsCreateCmk := (kmsCreateCmk keepAs kmsCreateCmk).value,

    kmsCreateDataKey := {
      val keyId = Defaults.getForParser(kmsCreateCmk)((state, key) => {
        val strings = key.map(_.getKeyMetadata.getKeyId).map(List(_)).getOrElse(Nil)
        Space ~> StringBasic.examples(strings: _*)
      }).parsed
      val kmsClient = clientKMS.value
      val result = KMSOperations.createDataKey(keyId, kmsClient)
      def hex(arr: Array[Byte]): String = javax.xml.bind.DatatypeConverter.printHexBinary(arr)
      val plainhex = hex(result.getPlaintext.array())
      val cipherhex = hex(result.getCiphertextBlob.array())
      println(
        s"""
          |KeyId: ${result.getKeyId}
          |PlainHex: $plainhex
          |CipherHex: $cipherhex
        """.stripMargin)
    },

    kmsDescribeKey := {
      val keyId = Defaults.getForParser(kmsCreateCmk)((state, key) => {
        val strings = key.map(_.getKeyMetadata.getKeyId).map(List(_)).getOrElse(Nil)
        Space ~> StringBasic.examples(strings: _*)
      }).parsed
      val kmsClient = clientKMS.value
      val result = KMSOperations.describeKey(keyId, kmsClient)
      def hex(arr: Array[Byte]): String = javax.xml.bind.DatatypeConverter.printHexBinary(arr)
      println(
        s"""
           |KeyId: ${result.getKeyMetadata.getKeyId}
           |Arn: ${result.getKeyMetadata.getArn}
           |Enabled: ${result.getKeyMetadata.getEnabled}
           |KeyState: ${result.getKeyMetadata.getKeyState}
           |ValidTo: ${result.getKeyMetadata.getValidTo}
           |ExpirationModel: ${result.getKeyMetadata.getExpirationModel}
        """.stripMargin)
    },

    kmsDeleteKey := {
      val keyId = Defaults.getForParser(kmsCreateCmk)((state, key) => {
        val strings = key.map(_.getKeyMetadata.getKeyId).map(List(_)).getOrElse(Nil)
        Space ~> StringBasic.examples(strings: _*)
      }).parsed
      val kmsClient = clientKMS.value
      val result = KMSOperations.deleteKey(keyId, kmsClient)
      def hex(arr: Array[Byte]): String = javax.xml.bind.DatatypeConverter.printHexBinary(arr)
      println(
        s"""
          |KeyId: ${result.getKeyId}
          |DeletionDate: ${result.getDeletionDate}
        """.stripMargin)
    },

    kmsListKeys := {
      val kmsClient = clientKMS.value
      val result = KMSOperations.list(kmsClient)
      result.foreach { entry =>
        println(s"${entry.getKeyId} - ${entry.getKeyArn}")
      }
    }
  )
}

object KinesisOperations {
  def client(): AmazonKinesis = {
    AmazonKinesisClientBuilder.defaultClient()
  }

  /**
    * Creates a Kinesis Stream
    */
  def createStream(streamName: String, client: AmazonKinesis): CreateStreamResult = {
    client.createStream(streamName, 1)
  }

  /**
    * Deletes a Kinesis stream
    */
  def deleteStream(streamName: String, client: AmazonKinesis): DeleteStreamResult = {
    client.deleteStream(streamName)
  }

  /**
    * Returns the names of the streams that are associated with the AWS account making the ListStreams request.
    */
  def listStreams(client: AmazonKinesis): List[String] = {
    client.listStreams().getStreamNames.asScala.toList
  }

  /**
    * Describes the stream, the information returned includes the stream name, Amazon Resource Name (ARN), creation time, enhanced metric
    * configuration, and shard map. The shard map is an array of shard objects. For each shard object, there is the
    * hash key and sequence number ranges that the shard spans, and the IDs of any earlier shards that played in a role
    * in creating the shard. Every record ingested in the stream is identified by a sequence number, which is assigned
    * when the record is put into the stream.
    */
  def describeStream(streamName: String, client: AmazonKinesis): StreamDescription = {
    client.describeStream(streamName).getStreamDescription
  }

  /**
    * Returns all stream descriptions
    */
  def describeAllStreams(client: AmazonKinesis): List[StreamDescription] = {
    val describeStreamFunction = describeStream(_: String, client)
    listStreams(client).map(describeStreamFunction)
  }
}

object IamOperations {
  def client(): AmazonIdentityManagement = {
    AmazonIdentityManagementClientBuilder.defaultClient()
  }

  /**
    * Returns all users in the AWS account.
    */
  def listUsers(client: AmazonIdentityManagement): List[User] = {
    client.listUsers().getUsers.asScala.toList
  }

  /**
    * Returns the user based on the AWS access key ID used to sign the request to this API.
    */
  def getUser(client: AmazonIdentityManagement): User = {
    client.getUser.getUser
  }

  final case class CredentialsRegionAndUser(credentialsAndRegion: CredentialsProfileAndRegion, user: User)
  final case class CredentialsProfileAndRegion(credentials:AWSCredentials, profileLocation: File, region: Regions)

  /**
    * Returns the inferred AWS Credentials and users using the DefaultAWSCredentialsProviderChain
    * and DefaultAwsRegionProviderChain used by the default AWS clients.
    */
  def getAwsCredentials(): Disjunction[Throwable, CredentialsProfileAndRegion] = Disjunction.fromTryCatchNonFatal {
    val profileLocation: File = new CredentialsDefaultLocationProvider().getLocation()
    val defaultCredsProvider = new DefaultAWSCredentialsProviderChain()
    val defaultRegionProvider = new DefaultAwsRegionProviderChain()
    val region: Regions = Regions.fromName(defaultRegionProvider.getRegion)
    val credentials: AWSCredentials = defaultCredsProvider.getCredentials
    CredentialsProfileAndRegion(credentials, profileLocation, region)
  }

  /**
    * Returns the inferred AWS Credentials and users using the DefaultAWSCredentialsProviderChain
    * and DefaultAwsRegionProviderChain used by the default AWS clients.
    */
  def getAwsCredentialsAndUser(client: AmazonIdentityManagement): Disjunction[String, CredentialsRegionAndUser] = {
    val creds = getAwsCredentials.leftMap(_.getMessage).validationNel
    val user = Disjunction.fromTryCatchNonFatal(getUser(client)).leftMap(_.getMessage).validationNel
    (creds |@| user)(CredentialsRegionAndUser.apply).leftMap(_.intercalate1(",")).disjunction
  }
}

object KMSOperations {
  def client(): AWSKMS = {
    AWSKMSClientBuilder.defaultClient()
  }

  def createCmk(description: String, client: AWSKMS): CreateKeyResult = {
    client.createKey(new CreateKeyRequest().withDescription(description))
  }

  def createDataKey(keyId: String, client: AWSKMS): GenerateDataKeyResult = {
    client.generateDataKey(new GenerateDataKeyRequest()
        .withKeyId(keyId )
      .withKeySpec(DataKeySpec.AES_256))
  }

  def describeKey(keyId: String, client: AWSKMS): DescribeKeyResult = {
    client.describeKey(new DescribeKeyRequest().withKeyId(keyId))
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
}

object UserInput {
  def readInput(prompt: String): String = {
    SimpleReader.readLine(s"$prompt\n") getOrElse {
      val badInputMessage = "Unable to read input"
      val updatedPrompt = if (prompt.startsWith(badInputMessage)) prompt else s"$badInputMessage\n$prompt"
      readInput(updatedPrompt)
    }
  }
}