package com.rtgrt.datacollector.service

import com.rtgrt.datacollector.model.GithubRepo
import com.typesafe.config.Config
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.model.{AttributeDefinition, AttributeValue, BatchWriteItemRequest, BatchWriteItemResponse, CreateTableRequest, DescribeTableRequest, KeySchemaElement, KeyType, ProvisionedThroughput, PutItemRequest, PutItemResponse, PutRequest, ResourceNotFoundException, ScalarAttributeType, WriteRequest}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

import java.net.URI
import scala.util.Try

import scala.collection.JavaConverters._

class DynamoDBService(config: Config) {
  private val tableName = config.getString("aws.dynamodb.tableName")
  private val endpointOverride = URI.create(config.getString("aws.dynamodb.endpointOverride"))
  private val region = Region.of(config.getString("aws.dynamodb.region"))

  private val ddbClient: DynamoDbClient = DynamoDbClient.builder()
    .region(region)
    .endpointOverride(endpointOverride)
    .credentialsProvider(DefaultCredentialsProvider.create())
    .build()

  def ensureTableExists(): Unit = {
    Try {
      ddbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build())
      println(s"Table $tableName already exists.")
    }.recover {
      case _: ResourceNotFoundException =>
        val request = CreateTableRequest.builder()
        .tableName(tableName)
        .keySchema(
          KeySchemaElement.builder().attributeName("full_name").keyType(KeyType.HASH).build()
        )
        .attributeDefinitions(
          AttributeDefinition.builder().attributeName("full_name").attributeType(ScalarAttributeType.S).build()
        )
        .provisionedThroughput(
          ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build()
        )
        .build()

        ddbClient.createTable(request)
        ddbClient.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build())
      case e: Throwable =>
        throw e
    }.get
  }

  def saveRepo(repo: GithubRepo): Try[PutItemResponse] = Try {
    val itemValues = Map(
      "id" -> AttributeValue.builder().n(repo.id.toString).build(),
      "name" -> AttributeValue.builder().s(repo.name).build(),
      "full_name" -> AttributeValue.builder().s(repo.full_name).build(),
      "owner_login" -> AttributeValue.builder().s(repo.owner.login).build(),
      "html_url" -> AttributeValue.builder().s(repo.html_url).build(),
      "description" -> repo.description.map(d =>
      AttributeValue.builder().s(d).build()).getOrElse(AttributeValue.builder().nul(true).build()),
      "stargazers_count" -> AttributeValue.builder().n(repo.stargazers_count.toString).build(),
      "watchers_count" -> AttributeValue.builder().n(repo.watchers_count.toString).build(),
      "forks_count" -> AttributeValue.builder().n(repo.forks_count.toString).build(),
      "open_issues_count" -> AttributeValue.builder().n(repo.open_issues_count.toString).build(),
      "language" -> repo.language.map(l => AttributeValue.builder().s(l).build()).getOrElse(AttributeValue.builder().nul(true).build()),
      "created_at" -> AttributeValue.builder().s(repo.created_at).build(),
      "updated_at" -> AttributeValue.builder().s(repo.updated_at).build(),
      "last_processed_timestamp" -> AttributeValue.builder().s(java.time.Instant.now().toString).build()
    )
    val request = PutItemRequest.builder()
      .tableName(tableName)
      .item(itemValues.asJava)
      .build()
    ddbClient.putItem(request)
  }

  def batchSaveRepos(repos: List[GithubRepo]): Try[List[BatchWriteItemResponse]] = Try {
    repos.grouped(25).map { batch =>
      val writeRequests = batch.map { repo =>
        val itemValues = Map(
          "id" -> AttributeValue.builder().n(repo.id.toString).build(),
          "name" -> AttributeValue.builder().s(repo.name).build(),
          "full_name" -> AttributeValue.builder().s(repo.full_name).build(),
          "owner_login" -> AttributeValue.builder().s(repo.owner.login).build(),
          "html_url" -> AttributeValue.builder().s(repo.html_url).build(),
          "description" -> repo.description.map(d => AttributeValue.builder().s(d).build()).getOrElse(AttributeValue.builder().nul(true).build()),
          "stargazers_count" -> AttributeValue.builder().n(repo.stargazers_count.toString).build(),
          "watchers_count" -> AttributeValue.builder().n(repo.watchers_count.toString).build(),
          "forks_count" -> AttributeValue.builder().n(repo.forks_count.toString).build(),
          "open_issues_count" -> AttributeValue.builder().n(repo.open_issues_count.toString).build(),
          "language" -> repo.language.map(l => AttributeValue.builder().s(l).build()).getOrElse(AttributeValue.builder().nul(true).build()),
          "created_at" -> AttributeValue.builder().s(repo.created_at).build(),
          "updated_at" -> AttributeValue.builder().s(repo.updated_at).build(),
          "last_processed_timestamp" -> AttributeValue.builder().s(java.time.Instant.now().toString).build()
        )
        WriteRequest.builder().putRequest(PutRequest.builder().item(itemValues.asJava).build()).build()
      }
      val batchWriteRequest = BatchWriteItemRequest.builder()
        .requestItems(Map(tableName -> writeRequests.asJava).asJava)
        .build()
      ddbClient.batchWriteItem(batchWriteRequest)
    }.toList
  }

  def close(): Unit = {
    ddbClient.close()
  }
}
