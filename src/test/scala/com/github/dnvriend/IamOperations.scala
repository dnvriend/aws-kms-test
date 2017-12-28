package com.github.dnvriend

import java.io.File

import com.amazonaws.auth.{ AWSCredentials, DefaultAWSCredentialsProviderChain }
import com.amazonaws.profile.path.cred.CredentialsDefaultLocationProvider
import com.amazonaws.regions.{ DefaultAwsRegionProviderChain, Regions }
import com.amazonaws.services.identitymanagement.{ AmazonIdentityManagement, AmazonIdentityManagementClientBuilder }
import com.amazonaws.services.identitymanagement.model.User
import scala.collection.JavaConverters._
import scalaz._
import scalaz.Scalaz._

import scalaz.Disjunction

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
  final case class CredentialsProfileAndRegion(credentials: AWSCredentials, profileLocation: File, region: Regions)

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

  def whoAmI(client: AmazonIdentityManagement): String = {
    getAwsCredentialsAndUser(client).foldMap {
      case CredentialsRegionAndUser(CredentialsProfileAndRegion(creds, profileLocation, region), iamUser) =>
        s"""
             |===================================
             |Using the following AWS credentials
             |===================================
             |* Profile File Location: ${profileLocation.getAbsolutePath}
             |* Region: '$region'
             |* IAM User:
             |  - UserName: '${iamUser.getUserName}'
             |  - Arn: '${iamUser.getArn}'
             |  - Created on: '${iamUser.getCreateDate}'
             |  - Last used on: '${iamUser.getPasswordLastUsed}'
             |* Credentials:
             |  - AWSAccessKeyId: '${creds.getAWSAccessKeyId}'
             |  - AWSSecretKey: '${creds.getAWSSecretKey}'
      """.stripMargin
    }
  }
}