package com.rtgrt.datacollector.service

import com.rtgrt.datacollector.model.GithubSearchResponse
import com.typesafe.config.Config
import io.circe.jawn.decode
import sttp.client3.{HttpClientSyncBackend, Response, UriContext, basicRequest}

import scala.util.Try
import scala.util.{Failure, Success}

class GithubService(config: Config) {
  private val backend = HttpClientSyncBackend()
  private val baseUrl = config.getString("github.api.baseUrl")
  private val token: String = Try(config.getString("github.api.token"))
    .orElse(Try(sys.env("GITHUB_TOKEN_ENV")))
    .getOrElse(throw new RuntimeException("GitHub token not found. Please set GITHUB_TOKEN_ENV environment variable or github.api.token in application.conf"))
  private val searchQuery = config.getString("github.api.searchQuery")
  private val perPage = config.getInt("github.api.perPage")

  def fetchTrendingRepos(): Either[Exception, GithubSearchResponse] = {
    val request = basicRequest
      .header("Authorization", s"token $token")
      .header("Accept", "application/vnd.github.v3+json")
      .get(uri"$baseUrl/search/repositories?q=$searchQuery&sort=stars&order=desc&per_page=$perPage")

    val triedResult: Try[Either[Exception, GithubSearchResponse]] = Try {
      val response: Response[Either[String, String]] = request.send(backend)

      response.body match {
        case Right(jsonString) =>
          decode[GithubSearchResponse](jsonString) match {
            case Right(parsedResponse) => Right(parsedResponse)
            case Left(circeError) =>
              Left(new Exception(s"JSON decoding error: ${circeError.getMessage}", circeError))
          }
        case Left(errorMsg) =>
          Left(new Exception(s"Github API request failed with status ${response.code}: $errorMsg"))
      }
    }
    triedResult match {
      case Success(eitherResult) =>
        eitherResult
      case Failure(exception) =>
        val finalException: Exception = exception match {
          case e: Exception => e
          case t: Throwable => new Exception(s"Underlying throwable during API call: ${t.getMessage}", t)
        }
        Left(finalException)
    }
  }
}
