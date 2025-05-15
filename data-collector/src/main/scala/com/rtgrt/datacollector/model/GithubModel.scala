package com.rtgrt.datacollector.model

import io.circe.Decoder
import io.circe.generic.semiauto._

case class RepoOwner(login: String, id: Long, avatar_url: String)
object RepoOwner {
  implicit val decoder: Decoder[RepoOwner] = deriveDecoder[RepoOwner]
}

case class GithubRepo (
                       id: Long,
                       name: String,
                       full_name: String,
                       owner: RepoOwner,
                       html_url: String,
                       description: Option[String],
                       stargazers_count: Int,
                       watchers_count: Int,
                       forks_count: Int,
                       open_issues_count: Int,
                       language: Option[String],
                       created_at: String,
                       updated_at: String
                       )
object GithubRepo {
  implicit val decoder: Decoder[GithubRepo] = deriveDecoder[GithubRepo]
}

case class GithubSearchResponse (
                                total_count: Int,
                                incomplete_results: Boolean,
                                items: List[GithubRepo]
                                )
object GithubSearchResponse {
  implicit val decoder: Decoder[GithubSearchResponse] = deriveDecoder[GithubSearchResponse]
}
