package com.rtgrt.datacollector

import com.rtgrt.datacollector.service.{DynamoDBService, GithubService}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.model.WriteRequest

import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.JavaConverters._

object DataCollectorApp extends App {
  val logger = LoggerFactory.getLogger(this.getClass)
  logger.info("Starting Real-time Github Repo Tracker - Data Collector")

  val config = ConfigFactory.load()

  val githubService = new GithubService(config)
  val dynamoDBService = new DynamoDBService(config)

  try {
    dynamoDBService.ensureTableExists()
  } catch {
    case e: Throwable =>
      logger.error(s"Failed to ensure dynamoDB table exists. Existing. Error: ${e.getMessage}", e)
      System.exit(1)
  }

  val task = new Runnable {
    def run(): Unit = {
      logger.info("Fetching trending Github repos...")
      githubService.fetchTrendingRepos() match {
        case Right(searchResponse) =>
          logger.info(s"Successfully fetched ${searchResponse.items.length} repos. Total found: ${searchResponse.total_count}.")
          if (searchResponse.items.nonEmpty) {
            dynamoDBService.batchSaveRepos(searchResponse.items) match {
              case scala.util.Success(responses) =>
                val totalUnprocessedCount = responses.map { batchResponse =>
                  val unprocessedScalaMap: Map[String, java.util.List[WriteRequest]] =
                    batchResponse.unprocessedItems().asScala.toMap

                  val unprocessedItemsForOurTable: List[WriteRequest] =
                    unprocessedScalaMap
                      .get(config.getString("aws.dynamodb.tableName"))
                      .map(_.asScala.toList)
                      .getOrElse(List.empty[WriteRequest])

                  unprocessedItemsForOurTable.size
                }.sum
                val totalProcessed = searchResponse.items.size - totalUnprocessedCount
                logger.info(s"Successfully saved/updated $totalProcessed repositories to DynamoDB.")
              case scala.util.Failure(e) =>
                logger.error(s"Failed to save repositories to DynamoDB: ${e.getMessage}", e)
            }
          } else {
            logger.info("No repositories found matching the criteria.")
          }
        case Left(error) =>
          logger.error(s"Error fetching data from GitHub: ${error.getMessage}", error)
      }
    }
  }

  val scheduler = Executors.newSingleThreadScheduledExecutor()
  val initialDelay = config.getLong("scheduler.initialDelaySeconds")
  val interval = config.getLong("scheduler.intervalSeconds")

  scheduler.scheduleAtFixedRate(task, initialDelay, interval, TimeUnit.SECONDS)
  logger.info(s"Scheduled GitHub data fetch every $interval seconds with an initial delay of $initialDelay seconds.")

  sys.addShutdownHook {
    logger.info("Shutting down Data Collector...")
    scheduler.shutdown()
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow()
      }
    } catch {
      case ie: InterruptedException => scheduler.shutdownNow()
    }
    dynamoDBService.close() // Close DynamoDB client
    logger.info("Data Collector shutdown complete.")
  }
}
